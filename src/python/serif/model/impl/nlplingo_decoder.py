import os, json
import logging
import numpy as np
import collections
import serifxml3
from serif.model.base_model import BaseModel
from serif.theory.sentence import Sentence
from nlplingo.decoding.decoder import Decoder, DocumentPrediction, SentencePrediction, EventPrediction, \
    TriggerPrediction, ArgumentPrediction
from nlplingo.text.text_theory import Document as lingoDoc
from nlplingo.annotation.serif import to_lingo_sentence
from nlplingo.annotation.ingestion import populate_doc_sentences_with_embeddings_and_annotations
from nlplingo.embeddings.word_embeddings import DocumentContextualEmbeddings
from nlplingo.text.text_theory import EventEventRelation
from nlplingo.tasks.eventrelation.postprocess import add_serif_eerm_to_all_eer_predictions
import time

logger = logging.getLogger(__name__)

class DummySentenceTheory(object):
    def __init__(self):
        self.token_sequence = list()

class DummySentence(object):
    def __init__(self,sent_no,start_edt,end_edt):
        dummy_sentence_theory = DummySentenceTheory()
        self.sentence_theories = [dummy_sentence_theory]
        self.sentence_theory = dummy_sentence_theory
        self.sent_no = sent_no
        self.start_edt = start_edt
        self.end_edt = end_edt


def find_lowest_common_ancestor(syn_node_1, syn_node_2):
    # https://www.hrwhisper.me/algorithm-lowest-common-ancestor-of-a-binary-tree
    assert isinstance(syn_node_1, serifxml3.SynNode)
    assert isinstance(syn_node_2, serifxml3.SynNode)
    visited = set()
    while syn_node_1 is not None and syn_node_2 is not None:
        if syn_node_1 is not None:
            if syn_node_1 in visited:
                return syn_node_1
            visited.add(syn_node_1)
            syn_node_1 = syn_node_1.parent
        if syn_node_2 is not None:
            if syn_node_2 in visited:
                return syn_node_2
            visited.add(syn_node_2)
            syn_node_2 = syn_node_2.parent
    return None


def build_nlplingo_entity_mention_id_to_serif_mention_valuemention_name_mapping_dict(serif_doc):
    assert isinstance(serif_doc, serifxml3.Document)
    # For why this is implemented in this way, refer to  nlplingo.annotation.serif
    # It turns out that nlplingo would use serif node id as nlplingo.text.text_span.EntityMention.id

    ret = dict()
    for sentence in serif_doc.sentences:
        assert isinstance(sentence, serifxml3.Sentence)
        for m in sentence.mention_set:
            ret[m.id] = m
        for m in sentence.value_mention_set:
            ret[m.id] = m
        for m in sentence.name_theory:
            ret[m.id] = m
    return ret


class NLPLingoDecoder(BaseModel):

    def __init__(self, params_path, npz_filelist, argparse, **kwargs):
        super(NLPLingoDecoder, self).__init__(**kwargs)
        self.argparse = argparse
        with open(params_path) as fp:
            self.params = json.load(fp)

        self.doc_id_to_bert_npz_path = dict()



        self.should_output_event_emb = False
        for extractor in self.params.get("extractors",[]):
            output_vectors = extractor.get("output_vectors",False)
            if output_vectors is True:
                self.should_output_event_emb = True
                break


        self.max_number_of_tokens_per_sentence = int(kwargs.get("max_number_of_tokens_per_sentence", -1))

        if os.path.isfile(npz_filelist):
            with open(npz_filelist) as fp:
                for i in fp:
                    i = i.strip()
                    docid = os.path.basename(i)
                    docid = docid.replace(".npz", "")
                    self.doc_id_to_bert_npz_path[docid] = i
        self.decoder = Decoder(self.params)
        self.decoder.load_model()

    def get_npz(self, docid):
        if docid in self.doc_id_to_bert_npz_path:
            return np.load(self.doc_id_to_bert_npz_path[docid], allow_pickle=True)
        else:
            return {"embeddings":np.asarray([]),"token_map":np.asarray([])}

    def reload_model(self):
        self.decoder.reload_model()

    def decode_event_and_event_argument(self, serif_doc):
        docid = serif_doc.docid
        lingo_doc = lingoDoc(docid)
        sent_edt_off_to_sent = dict()

        for st_index, sentence in enumerate(serif_doc.sentences):
            if self.max_number_of_tokens_per_sentence > -1:
                st = sentence.sentence_theories[0]
                if len(st.token_sequence) == 0 or len(st.token_sequence) > self.max_number_of_tokens_per_sentence:
                    to_lingo_sentence(serif_doc,st_index,DummySentence(st_index,sentence.start_edt,sentence.end_edt),lingo_doc=lingo_doc,
                              add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                              add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                              add_serif_entity_relation_mentions=self.params.get(
                                  'add_serif_entity_entity_relation_mentions', False),
                              add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                    continue

            to_lingo_sentence(serif_doc, st_index, sentence, lingo_doc=lingo_doc,
                              add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                              add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                              add_serif_entity_relation_mentions=self.params.get(
                                  'add_serif_entity_entity_relation_mentions', False),
                              add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
            if len(sentence.token_sequence) > 0:
                sent_edt_off_to_sent[
                    sentence.token_sequence[0].start_char, sentence.token_sequence[-1].end_char] = sentence

        if hasattr(serif_doc,"aux") and hasattr(serif_doc.aux,"bert_npz"):
            DocumentContextualEmbeddings.load_embeddings_into_doc(
                lingo_doc, serif_doc.aux.bert_npz)
        elif len(self.doc_id_to_bert_npz_path) > 0:
            DocumentContextualEmbeddings.load_embeddings_into_doc(
                lingo_doc, self.get_npz(docid))
        populate_doc_sentences_with_embeddings_and_annotations([lingo_doc], self.params, self.decoder.embeddings)
        list_trigger_extractor_result_collection, doc_id_to_event_and_event_arg_feature = self.decoder.decode_trigger_and_argument(
            [lingo_doc])

        serif_id_to_serif_mention_valuemention_name_mapping = build_nlplingo_entity_mention_id_to_serif_mention_valuemention_name_mapping_dict(
            serif_doc)
        if self.should_output_event_emb:
            self.decoder.serialize_doc_event_and_event_arg_feature_npz(doc_id_to_event_and_event_arg_feature,
                                                                       self.argparse.output_directory)
        for trigger_extractor_result_collection in list_trigger_extractor_result_collection:
            trigger_extractor_result_collection.organize_into_prediction_objects()
            prediction_object = trigger_extractor_result_collection.document_predictions
            for doc_p_docid, doc_p in prediction_object.items():
                assert docid == doc_p_docid
                for sent_p in doc_p.sentences.values():
                    assert isinstance(sent_p, SentencePrediction)
                    sent_start_edt = sent_p.start
                    sent_end_edt = sent_p.end - 1
                    sentence = sent_edt_off_to_sent[sent_start_edt, sent_end_edt]
                    assert isinstance(sentence, serifxml3.Sentence)
                    event_mention_set = sentence.event_mention_set
                    if event_mention_set is None:
                        event_mention_set = \
                            sentence.add_new_event_mention_set()
                        ''':type: EventMentionSet'''
                    token_start_edt_to_token = {token.start_edt: token for token in sentence.token_sequence}
                    token_end_edt_to_token = {token.end_edt: token for token in sentence.token_sequence}
                    for event_p in sent_p.events.values():
                        assert isinstance(event_p, EventPrediction)
                        list_serif_argument_tuple = list()
                        for argument_p in event_p.arguments.values():
                            assert isinstance(argument_p, ArgumentPrediction)
                            arg_start_char = argument_p.start
                            arg_end_char = argument_p.end - 1
                            arg_serif_id = argument_p.em_id
                            arg_serif_obj = serif_id_to_serif_mention_valuemention_name_mapping[arg_serif_id]
                            for arg_role, arg_score in argument_p.labels.items():
                                list_serif_argument_tuple.append(tuple((arg_role, arg_serif_obj, arg_score)))
                        trigger = event_p.trigger
                        assert isinstance(trigger, TriggerPrediction)
                        trigger_start_char = trigger.start
                        trigger_end_char = trigger.end - 1
                        start_token = token_start_edt_to_token[trigger_start_char]
                        end_token = token_end_edt_to_token[trigger_end_char]
                        event_anchor_synnode = find_lowest_common_ancestor(start_token.syn_node, end_token.syn_node)
                        assert isinstance(event_anchor_synnode, serifxml3.SynNode)
                        for event_type, event_type_score in trigger.labels.items():
                            event_mention = event_mention_set.add_new_event_mention(
                                event_type, event_anchor_synnode, event_type_score)
                            # add arguments
                            for arg_role, arg_serif_obj, arg_score in list_serif_argument_tuple:
                                added_arg = None
                                if isinstance(arg_serif_obj, serifxml3.Mention):
                                    added_arg = event_mention.add_new_mention_argument(arg_role, arg_serif_obj,
                                                                                       arg_score)
                                elif isinstance(arg_serif_obj, serifxml3.ValueMention):
                                    added_arg = event_mention.add_new_value_mention_argument(arg_role, arg_serif_obj,
                                                                                             arg_score)
                                else:
                                    raise ValueError(
                                        "Bad argument type {} in EventMention".format(type(arg_serif_obj).__name__))

    def decode_event_event_relation_doc_list(self, serif_doc_list):
        # START LOADING SERIFS
        lingo_docs = []
        sent_edt_off_to_sent_dict = {}
        lingo_anchor_int_pair_to_serif_ems_dict = {}
        eerm_set_dict = {}
        all_eer_predictions = dict()
        start = time.time()
        for serif_doc_idx, serif_doc in enumerate(serif_doc_list):
            docid = serif_doc.docid
            lingo_doc = lingoDoc(docid)
            sent_edt_off_to_sent_dict[docid] = dict()
            sent_edt_off_to_sent = sent_edt_off_to_sent_dict[docid]
            lingo_anchor_int_pair_to_serif_ems_dict[docid] = dict()
            lingo_anchor_int_pair_to_serif_ems = lingo_anchor_int_pair_to_serif_ems_dict[docid]
            # Code block for event and event argument
            for st_index, sentence in enumerate(serif_doc.sentences):
                assert isinstance(sentence, serifxml3.Sentence)
                if self.max_number_of_tokens_per_sentence > -1:
                    st = sentence.sentence_theories[0]
                    if len(st.token_sequence) == 0 or len(st.token_sequence) > self.max_number_of_tokens_per_sentence:
                        to_lingo_sentence(serif_doc,st_index,DummySentence(st_index,sentence.start_edt,sentence.end_edt),lingo_doc=lingo_doc,
                                  add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                  add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                  add_serif_entity_relation_mentions=self.params.get(
                                      'add_serif_entity_entity_relation_mentions', False),
                                  add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                        continue
                to_lingo_sentence(serif_doc, st_index, sentence, lingo_doc=lingo_doc,
                                  add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                  add_serif_event_mentions=True,
                                  add_serif_entity_relation_mentions=self.params.get(
                                      'add_serif_entity_entity_relation_mentions', False),
                                  add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                if len(sentence.token_sequence) > 0:
                    sent_edt_off_to_sent[
                        sentence.token_sequence[0].start_char, sentence.token_sequence[-1].end_char] = sentence
                ### Populate EER candidates Now only do in sentence EER
                for event_mention_src in sentence.event_mention_set or []:
                    lingo_em_arg1 = lingo_doc.get_event_with_id(event_mention_src.id)
                    for anchor in lingo_em_arg1.anchors:
                        lingo_anchor_int_pair_to_serif_ems.setdefault(
                            (anchor.start_char_offset(), anchor.end_char_offset()), set()).add(event_mention_src)
                    for event_mention_dst in sentence.event_mention_set or []:
                        if event_mention_src != event_mention_dst:
                            lingo_em_arg2 = lingo_doc.get_event_with_id(event_mention_dst.id)
                            relation_type = None
                            eer = EventEventRelation(relation_type, lingo_em_arg1, lingo_em_arg2, serif_sentence=sentence, serif_event_0=event_mention_src, serif_event_1=event_mention_dst)
                            lingo_doc.add_event_event_relation(eer)
                ### End populate EER candidates


            eerm_set = serif_doc.event_event_relation_mention_set
            if eerm_set is None:
                eerm_set = \
                    serif_doc.add_new_event_event_relation_mention_set()
                ''':type: EventEventRelationMentionSet'''
            # add LearnIt event-event relation mentions into a data structure
            for serif_eerm in eerm_set or []:
                if serif_eerm.model == 'LearnIt':
                    add_serif_eerm_to_all_eer_predictions(all_eer_predictions, serif_eerm, lingo_doc)
            eerm_set = serif_doc.add_new_event_event_relation_mention_set() # kill the eerm_set
            eerm_set_dict[docid] = eerm_set
            lingo_docs.append(lingo_doc)
        # END LOADING SERIFS
        end = time.time()
        logging.info('SerifXML loading took %s seconds', end - start)

        logging.info('Start of entire EER decoding step')
        start = time.time()
        event_event_relation_result_collection = self.decoder.decode_event_event_relation(lingo_docs, all_eer_predictions, sent_edt_off_to_sent_dict)
        end = time.time()
        logging.info('Entire EER decoding took %s seconds', end - start)

        logging.info('Start of EER prediction object organization')
        start = time.time()
        event_event_relation_result_collection.organize_into_prediction_objects()
        end = time.time()
        logging.info('EER prediction object organization took %s seconds', end - start)
        prediction_object = event_event_relation_result_collection.document_predictions

        logging.info('Start of writing EERs into SerifXML')
        start = time.time()
        for doc_p_docid, doc_p in prediction_object.items():
            sent_edt_off_to_sent = sent_edt_off_to_sent_dict[doc_p_docid]
            lingo_anchor_int_pair_to_serif_ems = lingo_anchor_int_pair_to_serif_ems_dict[doc_p_docid]
            eerm_set = eerm_set_dict[doc_p_docid]
            for sent_p in doc_p.sentences.values():
                assert isinstance(sent_p, SentencePrediction)
                sent_start_edt = sent_p.start
                sent_end_edt = sent_p.end - 1
                sentence = sent_edt_off_to_sent[sent_start_edt, sent_end_edt]
                assert isinstance(sentence, serifxml3.Sentence)

                for event_event_relation_p in sent_p.event_event_relations.values():
                    left_trigger_p = event_event_relation_p.left_event.trigger
                    right_trigger_p = event_event_relation_p.right_event.trigger
                    for relation_type, score in event_event_relation_p.labels.items():
                        for left_serif_em in lingo_anchor_int_pair_to_serif_ems[
                            (left_trigger_p.start, left_trigger_p.end)]:
                            for right_serif_em in lingo_anchor_int_pair_to_serif_ems[
                                (right_trigger_p.start, right_trigger_p.end)]:
                                eerm = eerm_set.add_new_event_event_relation_mention(
                                    relation_type, score, "nlplingo")
                                eerm.add_new_event_mention_argument("arg1", left_serif_em)
                                eerm.add_new_event_mention_argument("arg2", right_serif_em)
                                logger.debug("{}\t{}\t{}\t{}".format(left_serif_em.anchor_node.text, relation_type,
                                                                     right_serif_em.anchor_node.text, sentence.text))

        for docid in event_event_relation_result_collection.learnit_relations:
            learnit_relations = event_event_relation_result_collection.learnit_relations[docid]
            for serif_eerm in learnit_relations:
                eerm_set_dict[docid].add_event_event_relation_mention(serif_eerm)
        end = time.time()
        logging.info('Writing EERs into SerifXML took %s seconds', end - start)

    def process(self, serif_doc):
        assert self.decoder.model_loaded == True
        if len(self.decoder.event_trigger_extractors) > 0 or len(self.decoder.event_argument_extractors) > 0:
            self.decode_event_and_event_argument(serif_doc)
        if len(self.decoder.event_event_relation_extractors) > 0:
            self.decode_event_event_relation_doc_list([serif_doc])

    def process_barrier(self, serif_doc_list):
        assert self.decoder.model_loaded == True
        if len(self.decoder.event_event_relation_extractors) > 0:
            self.decode_event_event_relation_doc_list(serif_doc_list)
