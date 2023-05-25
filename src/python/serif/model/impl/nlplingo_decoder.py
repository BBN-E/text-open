import collections
import json
import logging
import os

import numpy as np
from nlplingo.annotation.ingestion import populate_doc_sentences_with_embeddings_and_annotations
from nlplingo.annotation.serif import to_lingo_sentence
from nlplingo.decoding.decoder import Decoder, SentencePrediction, EventPrediction, \
    TriggerPrediction, ArgumentPrediction
from nlplingo.embeddings.word_embeddings import DocumentContextualEmbeddings
from nlplingo.text.text_theory import Document as lingoDoc

import serifxml3
from serif.model.document_model import DocumentModel
from serif.model.event_event_relation_mention_model import EventEventRelationMentionModel
from serif.model.relation_mention_model import RelationMentionModel
from serif.model.mention_coref_model import MentionCoreferenceModel
from serif.theory.enumerated_type import Tense, Modality
from serif.util.add_nlplingo_event_mentions_to_serifxml_better import nlplingo_event_mention_adder
from serif.util.serifxml_utils import exist_in_event_mention_set
from serif.util.hierarchical_agglomerative_clustering import redistribute_entity_coref_predictions_based_on_hierarchical_agglomerative_clustering
from serif.util.better_serifxml_helper import find_valid_anchors_by_token_index

logger = logging.getLogger(__name__)


class DummySentenceTheory(object):
    def __init__(self):
        self.token_sequence = list()


class DummySentence(object):
    def __init__(self, sent_no, start_edt, end_edt):
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
        if sentence.value_mention_set:
            for m in sentence.value_mention_set:
                ret[m.id] = m
        for m in sentence.name_theory:
            ret[m.id] = m
    return ret


def classification_event_mention_and_event_argument_adder(event_document_predictions, serif_doc, sent_edt_off_to_sent,
                                                          model_name="NLPLingo"):
    serif_id_to_serif_mention_valuemention_name_mapping = build_nlplingo_entity_mention_id_to_serif_mention_valuemention_name_mapping_dict(
        serif_doc)
    for doc_p_docid, doc_p in event_document_predictions.items():
        assert serif_doc.docid == doc_p_docid
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
                    if (argument_p.em_id is None):
                        continue
                    arg_serif_obj = serif_id_to_serif_mention_valuemention_name_mapping[arg_serif_id]
                    for arg_role, arg_score in argument_p.labels.items():
                        list_serif_argument_tuple.append(tuple((arg_role, arg_serif_obj, arg_score)))
                trigger = event_p.trigger
                assert isinstance(trigger, TriggerPrediction)
                trigger_start_char = trigger.start
                trigger_end_char = trigger.end - 1
                start_token = token_start_edt_to_token[trigger_start_char]
                end_token = token_end_edt_to_token[trigger_end_char]
                start_token_index = list(sentence.token_sequence).index(start_token)
                end_token_index = list(sentence.token_sequence).index(end_token)
                for event_type, event_type_score in trigger.labels.items():
                    # check whether the event mention I'm trying to add, is already existing in event_mention_set
                    event_mention = exist_in_event_mention_set(
                        event_mention_set,
                        event_type,
                        None,
                        start_token_index,
                        end_token_index,
                        trigger_start_char,
                        trigger_end_char
                    )
                    if event_mention is None:
                        event_mention = event_mention_set.add_new_event_mention(
                            event_type,
                            None,
                            event_type_score
                        )
                        event_mention.semantic_phrase_start = start_token_index
                        event_mention.semantic_phrase_end = end_token_index
                        event_mention.model = model_name
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

def get_or_create_new_value_mention(start_token,end_token,value_type,span_to_value_mentions):
    found = list()
    if (start_token,end_token) in span_to_value_mentions:
        for value_mention in span_to_value_mentions[(start_token,end_token)]:
            if value_type == "*" or value_mention.value_type == value_type:
                found.append(value_mention)
    if len(found) > 0:
        return found
    else:
        serif_sentence = start_token.owner_with_type("Sentence")
        assert serif_sentence == end_token.owner_with_type("Sentence")
        new_value_em = serif_sentence.value_mention_set.add_new_value_mention(start_token, end_token, value_type)
        span_to_value_mentions.setdefault((start_token,end_token),list()).append(new_value_em)
        return [new_value_em]

def get_or_create_new_entity_mention(start_token,end_token,syn_node,entity_type,entity_subtype,mention_type,span_to_mentions):
    found = list()
    if syn_node in span_to_mentions:
        for mention in span_to_mentions[syn_node]:
            if (entity_type == "*" or mention.entity_type == entity_type) and (entity_subtype == "*" or mention.entity_subtype == entity_subtype) and (mention_type == "*" or mention.mention_type == mention_type):
                found.append(mention)
    if len(found) > 0:
        return found
    if (start_token,end_token) in span_to_mentions:
        for mention in span_to_mentions[(start_token,end_token)]:
            if (entity_type == "*" or mention.entity_type == entity_type) and (entity_subtype == "*" or mention.entity_subtype == entity_subtype) and (mention_type == "*" or mention.mention_type == mention_type):
                found.append(mention)
    if len(found) > 0:
        return found
    sent = start_token.owner_with_type("Sentence")
    assert sent == end_token.owner_with_type("Sentence")
    overlap, anchors = find_valid_anchors_by_token_index(
        # find any synnode in sentence which partially covers the token indices
        sent.sentence_theories[0],
        start_token.index(),
        end_token.index(),
        0.99
    )
    anchor = None
    if len(anchors) > 0:
        anchor = anchors[0]
    if anchor:
        new_mention = sent.mention_set.add_new_mention(syn_node=anchor, mention_type=str(mention_type.value), entity_type=entity_type)
        span_to_mentions.setdefault(syn_node,list()).append(new_mention)
    else:
        new_mention = sent.mention_set.add_new_mention_from_tokens(mention_type=str(mention_type.value),entity_type=entity_type,start_token=start_token,end_token=end_token)
        span_to_mentions.setdefault((start_token,end_token),list()).append(new_mention)
    new_mention.entity_subtype = entity_subtype
    return [new_mention]

def get_or_create_new_event_mention(start_token,end_token,event_type,span_to_event_mentions,*,score=1.0):
    start_token_idx = start_token.index()
    end_token_idx = end_token.index()
    found = list()
    if (start_token,end_token) in span_to_event_mentions:
        for event_mention in span_to_event_mentions[(start_token,end_token)]:
            for event_type in event_mention.event_types:
                if event_type == "*" or event_type.event_type == event_type:
                    found.append(event_mention)
    if len(found) > 0:
        return found
    sent = start_token.owner_with_type("Sentence")
    assert sent == end_token.owner_with_type("Sentence")
    overlap, anchors = find_valid_anchors_by_token_index(
        # find any synnode in sentence which partially covers the token indices
        sent.sentence_theories[0],
        start_token.index(),
        end_token.index(),
        0.99
    )
    anchor = None
    if len(anchors) > 0:
        anchor = anchors[0]
    new_event_mention = sent.event_mention_set.add_new_event_mention(event_type=event_type, anchor_node=anchor, score=score)
    new_event_mention.semantic_phrase_start = start_token_idx
    new_event_mention.semantic_phrase_end = end_token_idx
    new_event_mention.add_new_event_mention_type(event_type, score)
    if anchor is not None:
        new_event_mention.add_new_event_mention_anchor(anchor)
    span_to_event_mentions.setdefault((start_token,end_token),list()).append(new_event_mention)
    return [new_event_mention]

def get_or_create_new_entity_mention_relation(left_mention,right_mention,relation_type,mention_spans_to_relations):
    found = list()
    if (left_mention,right_mention) in mention_spans_to_relations:
        for entity_mention_relation in mention_spans_to_relations[(left_mention,right_mention)]:
            if entity_mention_relation.type == relation_type:
                found.append(entity_mention_relation)
    if len(found) > 0:
        return found
    sent = left_mention.owner_with_type("Sentence")
    assert sent == right_mention.owner_with_type("Sentence")
    new_entity_mention_relation = sent.rel_mention_set.add_new_relation_mention(left_mention,right_mention,relation_type,serifxml3.Tense.Unspecified,serifxml3.Modality.Other)
    mention_spans_to_relations.setdefault((left_mention,right_mention),list()).append(new_entity_mention_relation)
    return [new_entity_mention_relation]

def get_or_create_new_event_mention_event_mention_relation(left_event_mention, right_event_mention, relation_type, event_mention_spans_to_relations):
    found = list()
    if (left_event_mention, right_event_mention) in event_mention_spans_to_relations:
        for serif_eerm in event_mention_spans_to_relations[(left_event_mention, right_event_mention)]:
            if serif_eerm.relation_type == relation_type:
                found.append(serif_eerm)
    if len(found) > 0:
        return found
    serif_doc = left_event_mention.owner_with_type("Document")
    assert serif_doc == right_event_mention.owner_with_type("Document")
    new_serif_eerm = serif_doc.event_event_relation_mention_set.add_new_event_event_relation_mention(
                                    relation_type, 0.0, None)
    new_serif_eerm.add_new_event_mention_argument("arg1", left_event_mention)
    new_serif_eerm.add_new_event_mention_argument("arg2", right_event_mention)
    event_mention_spans_to_relations.setdefault((left_event_mention,right_event_mention),list()).append(new_serif_eerm)
    return [new_serif_eerm]

def get_start_end_token_idx_from_event_mention(serif_em):
    serif_sentence = serif_em.sentence
    if serif_em.semantic_phrase_start is not None and serif_em.semantic_phrase_end is not None:
        return int(serif_em.semantic_phrase_start), int(serif_em.semantic_phrase_end)
    elif len(serif_em.anchors) > 0:
        earliest_idx = len(serif_sentence.token_sequence) - 1
        latest_idx = 0
        for anchor in serif_em.anchors:
            assert type(anchor.anchor_node) == serifxml3.SynNode
            earliest_idx = min(earliest_idx, serif_sentence.token_sequence.index(anchor.anchor_node.start_token))
            latest_idx = max(latest_idx, serif_sentence.token_sequence.index(anchor.anchor_node.end_token))
        return earliest_idx, latest_idx
    elif serif_em.anchor_node is not None:
        return serif_sentence.token_sequence.index(
            serif_em.anchor_node.start_token), serif_sentence.token_sequence.index(serif_em.anchor_node.end_token)
    else:
        return None, None


def get_start_end_token_idx_from_entity_mention(serif_em):
    serif_sentence = serif_em.sentence
    if serif_em.syn_node:
        return serif_sentence.token_sequence.index(serif_em.syn_node.start_token), serif_sentence.token_sequence.index(
            serif_em.syn_node.end_token)
    else:
        return serif_sentence.token_sequence.index(serif_em.start_token), serif_sentence.token_sequence.index(
            serif_em.end_token)


def get_start_end_edt_from_serif_event_mention(serif_event_mention):
    """
    # We need to replicate the behavior of https://ami-gitlab-01.bbn.com/text-group/nlplingo/-/blob/26f55d49d210fab0fc562f56e45d1ed6c9f66b52/nlplingo/annotation/serif.py#L304
    :param serif_event_mention:
    :return:
    """
    result = set()
    serif_sent = serif_event_mention.sentence
    if serif_event_mention.semantic_phrase_start is not None and serif_event_mention.semantic_phrase_end is not None:
        start_index = int(serif_event_mention.semantic_phrase_start)
        end_index = int(serif_event_mention.semantic_phrase_end)
        start = serif_sent.token_sequence[start_index:end_index + 1][0].start_edt
        end = serif_sent.token_sequence[start_index:end_index + 1][-1].end_edt
        result.add((start, end))
    else:
        anchor = serif_event_mention.anchor_node
        if anchor is None:
            logger.info("EventMention {} in doc {} has no anchor!  Skipping...".format(serif_event_mention.id,
                                                                                       serif_event_mention.document.docid))
            return result
    for anchor in serif_event_mention.anchors:
        if anchor.semantic_phrase_start is not None and anchor.semantic_phrase_end is not None and serif_sent.sent_no == anchor.anchor_node.sent_no:
            start_index = int(anchor.semantic_phrase_start)
            end_index = int(anchor.semantic_phrase_end)
            start = serif_sent.token_sequence[start_index:end_index + 1][0].start_edt
            end = serif_sent.token_sequence[start_index:end_index + 1][-1].end_edt
        else:
            start = anchor.anchor_node.start_token.start_edt
            end = anchor.anchor_node.end_token.end_edt
        already_exists = False
        for existing_anchor_start, existing_anchor_end in result:
            if (existing_anchor_start <= start) and (end <= existing_anchor_end):
                already_exists = True
                break
        if not already_exists:
            result.add((start, end))
    return result


class NLPLingoDecoder(DocumentModel):
    def __init__(self, params_path, **kwargs):
        super(NLPLingoDecoder, self).__init__(**kwargs)
        with open(params_path) as fp:
            self.params = json.load(fp)
        logger.info("NLPLingo param we got is: \n{}".format(json.dumps(self.params, indent=4, sort_keys=True, ensure_ascii=False)))
        self.doc_id_to_bert_npz_path = dict()

        self.should_output_event_emb = False
        for extractor in self.params.get("extractors", []):
            output_vectors = extractor.get("output_vectors", False)
            if output_vectors is True:
                self.should_output_event_emb = True
                break

        self.max_number_of_tokens_per_sentence = int(kwargs.get("max_number_of_tokens_per_sentence", -1))

        if "bert_npz_filelist" in kwargs and os.path.isfile(kwargs["bert_npz_filelist"]):
            with open(kwargs["bert_npz_filelist"]) as fp:
                for i in fp:
                    i = i.strip()
                    docid = os.path.basename(i)
                    docid = docid.replace(".npz", "")
                    self.doc_id_to_bert_npz_path[docid] = i

        self.use_sequence_tagging_adder = False
        if "use_sequence_tagging_adder" in kwargs and kwargs["use_sequence_tagging_adder"].lower() == "true":
            self.use_sequence_tagging_adder = True

        self.hac_distance_threshold = float(kwargs.get("hac_distance_threshold", 0.4))

        self.decoder = Decoder(self.params)
        self.decoder.load_model()

    def get_npz(self, docid):
        if docid in self.doc_id_to_bert_npz_path:
            return np.load(self.doc_id_to_bert_npz_path[docid], allow_pickle=True)
        else:
            return {"embeddings": np.asarray([]), "token_map": np.asarray([])}

    def reload_model(self):
        self.decoder.reload_model()

    def decode_event_and_event_argument(self, serif_doc):
        docid = serif_doc.docid
        lingo_doc, sent_edt_off_to_sent = self.convert_serif_doc_to_lingo_doc(serif_doc)

        if hasattr(serif_doc, "aux") and hasattr(serif_doc.aux, "bert_npz"):
            DocumentContextualEmbeddings.load_embeddings_into_doc(
                lingo_doc, serif_doc.aux.bert_npz)
        elif len(self.doc_id_to_bert_npz_path) > 0:
            DocumentContextualEmbeddings.load_embeddings_into_doc(
                lingo_doc, self.get_npz(docid))
        populate_doc_sentences_with_embeddings_and_annotations([lingo_doc], self.params, self.decoder.embeddings)

        event_document_predictions, doc_id_to_event_and_event_arg_feature = self.decoder.decode_trigger_and_argument(
            [lingo_doc])

        if self.should_output_event_emb:
            self.decoder.serialize_doc_event_and_event_arg_feature_npz(doc_id_to_event_and_event_arg_feature,
                                                                       self.argparse.output_directory)
        if self.use_sequence_tagging_adder is True:
            # Write back code start
            if docid in event_document_predictions:
                self.statistics = collections.defaultdict(int)
                self.event_type_statistics = collections.defaultdict(int)
                self.event_arg_type_statistics = collections.defaultdict(int)
                nlplingo_event_mention_adder(serif_doc, event_document_predictions[docid], self.statistics,
                                             self.event_type_statistics, self.event_arg_type_statistics)
                for k in sorted(self.statistics):
                    logger.info('{} {}'.format(k, str(self.statistics[k])))

                for key, value in self.event_type_statistics.items():
                    logger.info('EVENT TYPE COUNT {}: {}'.format(key, value))

                for key, value in self.event_arg_type_statistics.items():
                    logger.info('EVENT ARG TYPE COUNT {}: {}'.format(key, value))

        else:
            model_name = "NLPLingo"
            if len(self.decoder.event_trigger_extractors) >= 1:
                if self.decoder.event_trigger_extractors[0].extractor_params['annotation_scheme'] == "classification":
                    model_name = "NLPLingo_classification"

            classification_event_mention_and_event_argument_adder(event_document_predictions, serif_doc,
                                                                  sent_edt_off_to_sent, model_name)

    def decode_event_event_relation(self, serif_doc):
        lingo_doc, sent_edt_off_to_sent = self.convert_serif_doc_to_lingo_doc(serif_doc)
        event_mention_off_to_event_mention_doc_level = dict()

        for st_index, sentence in enumerate(serif_doc.sentences):
            assert isinstance(sentence, serifxml3.Sentence)
            for event_mention in sentence.event_mention_set or []:
                start_token_idx, end_token_idx = get_start_end_token_idx_from_event_mention(event_mention)
                if start_token_idx is not None and end_token_idx is not None:
                    event_mention_off_to_event_mention_doc_level.setdefault((st_index, start_token_idx, end_token_idx),
                                                                            list()).append(event_mention)

        eerm_set = serif_doc.event_event_relation_mention_set
        if eerm_set is None:
            eerm_set = \
                serif_doc.add_new_event_event_relation_mention_set()
            ''':type: EventEventRelationMentionSet'''

        document_predicions = self.decoder.decode_event_event_relation([lingo_doc])
        if lingo_doc.docid not in document_predicions:
            logger.warning("Cannot find decoding result for {}".format(lingo_doc.docid))
            event_event_relation_predictions = dict()
        else:
            event_event_relation_predictions = document_predicions[lingo_doc.docid].event_event_relations

        for (left_event_ref, right_event_ref), p in event_event_relation_predictions.items():

            (left_snt_idx, left_start_token_index, left_end_token_index) = left_event_ref
            (right_snt_idx, right_start_token_index, right_end_token_index) = right_event_ref

            assert left_snt_idx == right_snt_idx

            left_event_mentions = event_mention_off_to_event_mention_doc_level.get(
                (left_snt_idx, left_start_token_index, left_end_token_index), list())
            right_event_mentions = event_mention_off_to_event_mention_doc_level.get(
                (right_snt_idx, right_start_token_index, right_end_token_index), list())
            if len(left_event_mentions) < 1 or len(right_event_mentions) < 1:
                logger.warning(
                    "Cannot align EER back for {} {} {} {} {} {}".format(left_snt_idx, left_start_token_index,
                                                                         left_end_token_index, p,
                                                                         right_start_token_index,
                                                                         right_end_token_index))
                continue

            for left_event_mention in left_event_mentions:
                for right_event_mention in right_event_mentions:
                    EventEventRelationMentionModel.add_new_event_event_relation_mention(eerm_set=eerm_set,
                                                                                        relation_type=p,
                                                                                        confidence=0.9,
                                                                                        model_name="NLPLingo",
                                                                                        arg1_event_mention=left_event_mention,
                                                                                        arg2_event_mention=right_event_mention,
                                                                                        pattern=None,
                                                                                        polarity=None,
                                                                                        trigger_text=None)

    def convert_serif_doc_to_lingo_doc(self, serif_doc):
        """
        This function exists due to
        1. We need to enforce max_number_of_tokens_per_sentence
        2. For some tasks, such as event_event_relation classification task, we need to do example generator at text-open side
        :param serif_doc:
        :return:
        """
        docid = serif_doc.docid
        lingo_doc = lingoDoc(docid)
        sent_edt_off_to_sent = dict()

        for st_index, sentence in enumerate(serif_doc.sentences):
            assert isinstance(sentence, serifxml3.Sentence)
            if self.max_number_of_tokens_per_sentence > -1:
                st = sentence.sentence_theories[0]
                if len(st.token_sequence) == 0 or len(st.token_sequence) > self.max_number_of_tokens_per_sentence:
                    to_lingo_sentence(serif_doc, st_index,
                                      DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                      lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', False),
                                      add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                      add_serif_entity_relation_mentions=self.params.get(
                                          'add_serif_entity_entity_relation_mentions', False),
                                      add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                    continue
            to_lingo_sentence(serif_doc, st_index, sentence, lingo_doc=lingo_doc,
                              add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', False),
                              add_serif_event_mentions=True,
                              add_serif_entity_relation_mentions=self.params.get(
                                  'add_serif_entity_entity_relation_mentions', False),
                              add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
            if len(sentence.token_sequence) > 0:
                sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt,
                                     sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence

        return lingo_doc, sent_edt_off_to_sent

    def decode_event_coreference(self, serif_doc):
        event_coref_set = serif_doc.event_set
        if event_coref_set is None:
            event_coref_set = serif_doc.add_new_event_set()

        lingo_doc, sent_edt_off_to_sent = self.convert_serif_doc_to_lingo_doc(serif_doc)

        char_edt_to_ems = dict()
        for sentence in serif_doc.sentences:
            for event_mention in sentence.event_mention_set or ():
                offsets = get_start_end_edt_from_serif_event_mention(event_mention)
                for start_edt, end_edt in offsets:
                    char_edt_to_ems.setdefault((start_edt, end_edt), set()).add(event_mention)

        document_predictions = self.decoder.decode_event_coreference([lingo_doc])

        added_coreference = set()  # We try to avoid (a,b) and (b,a) both appears in the decoding result

        for doc_id, doc_p in document_predictions.items():
            for coref_id, coref_p in doc_p.event_coreference.items():
                relevant_event_mentions = set()
                for event_p in coref_p.event_mentions:
                    trigger_p = event_p.trigger
                    trigger_span = trigger_p.start, trigger_p.end - 1
                    if trigger_span not in char_edt_to_ems:
                        logger.warning("Cannot find {} {} for event coreference".format(serif_doc.docid, trigger_span))
                    else:
                        relevant_event_mentions.update(char_edt_to_ems[trigger_span])
                if len(relevant_event_mentions) > 1:
                    event_coref_key = frozenset(relevant_event_mentions)
                    if event_coref_key not in added_coreference:
                        represent_event_mention = list(relevant_event_mentions)[0]
                        event_coref_set.add_new_event(list(relevant_event_mentions), represent_event_mention.event_type)
                        added_coreference.add(event_coref_key)

    def decode_entity_coreference(self, serif_doc):
        entity_coref_set = serif_doc.entity_set
        if entity_coref_set is None:
            entity_coref_set = serif_doc.add_new_entity_set()
        lingo_doc, sent_edt_off_to_sent = self.convert_serif_doc_to_lingo_doc(serif_doc)
        entity_mention_off_to_entity_mention_doc_level = dict()
        for sentence in serif_doc.sentences:
            sent_idx = sentence.sent_no
            for serif_mention in sentence.mention_set or ():
                start_token_idx, end_token_idx = get_start_end_token_idx_from_entity_mention(serif_mention)
                if start_token_idx is not None and end_token_idx is not None:
                    entity_mention_off_to_entity_mention_doc_level.setdefault(
                        (sent_idx, start_token_idx, end_token_idx), set()).add(serif_mention)

        document_predictions = self.decoder.decode_entity_coreference([lingo_doc])
        added_entity_corefs = set()
        missed_entity_spans = set()
        for doc_id, doc_p in document_predictions.items():

            doc_p = redistribute_entity_coref_predictions_based_on_hierarchical_agglomerative_clustering(doc_p,
                                                                                                         distance_threshold=self.hac_distance_threshold)
            for entity_coref_id, entity_coref_p in doc_p.entity_coreference.items():
                relevant_entity_mentions = set()
                for sent_idx, token_start_idx, token_end_idx in entity_coref_p.entity_mentions:
                    entity_mentions = entity_mention_off_to_entity_mention_doc_level.get(
                        (sent_idx, token_start_idx, token_end_idx), list())
                    if len(entity_mentions) < 1:
                        missed_entity_spans.add((doc_id, sent_idx, token_start_idx, token_end_idx))
                    else:
                        relevant_entity_mentions.update(entity_mentions)
                if len(relevant_entity_mentions) > 1:
                    entity_coref_key = frozenset(relevant_entity_mentions)
                    if entity_coref_key not in added_entity_corefs:
                        MentionCoreferenceModel.add_new_entity(entity_coref_set, relevant_entity_mentions)
        for missed_entity_span in missed_entity_spans:
            logger.warning("Cannot find matching serif_entity {}".format(missed_entity_span))

    def decode_entity_relation(self, serif_doc):
        lingo_doc, sent_edt_off_to_sent = self.convert_serif_doc_to_lingo_doc(serif_doc)
        entity_mention_off_to_entity_mention_doc_level = dict()
        for sentence in serif_doc.sentences:
            sent_idx = sentence.sent_no
            for serif_mention in sentence.mention_set or ():
                start_token_idx, end_token_idx = get_start_end_token_idx_from_entity_mention(serif_mention)
                if start_token_idx is not None and end_token_idx is not None:
                    entity_mention_off_to_entity_mention_doc_level.setdefault(
                        (sent_idx, start_token_idx, end_token_idx), set()).add(serif_mention)

        document_predictions = self.decoder.decode_entity_relation([lingo_doc])
        for doc_id, doc_p in document_predictions.items():
            for sent_id, sent_p in doc_p.sentences.items():
                for (left_entity_ref, right_entity_ref), p in sent_p.entity_mention_entity_mention_relations.items():

                    (left_snt_idx, left_start_token_index, left_end_token_index) = left_entity_ref
                    (right_snt_idx, right_start_token_index, right_end_token_index) = right_entity_ref

                    assert left_snt_idx == right_snt_idx

                    left_entity_mentions = entity_mention_off_to_entity_mention_doc_level.get(
                        (left_snt_idx, left_start_token_index, left_end_token_index), list())
                    right_entity_mentions = entity_mention_off_to_entity_mention_doc_level.get(
                        (right_snt_idx, right_start_token_index, right_end_token_index), list())
                    serif_sentence = serif_doc.sentences[left_snt_idx]
                    entity_mention_relation_set = serif_sentence.rel_mention_set
                    if entity_mention_relation_set is None:
                        entity_mention_relation_set = serif_sentence.add_new_relation_mention_set()
                    if len(left_entity_mentions) < 1 or len(right_entity_mentions) < 1:
                        logger.warning(
                            "Cannot align entity_mention entity_mention relation back for {} {} {} {} {} {}".format(
                                left_snt_idx, left_start_token_index,
                                left_end_token_index, p,
                                right_start_token_index,
                                right_end_token_index))
                        continue

                    for left_entity_mention in left_entity_mentions:
                        for right_entity_mention in right_entity_mentions:
                            RelationMentionModel.add_new_relation_mention(entity_mention_relation_set, p,
                                                                          left_entity_mention,
                                                                          right_entity_mention, Tense.Unspecified,
                                                                          Modality.Other)

    def process_document(self, serif_doc):
        assert self.decoder.model_loaded == True
        if len(self.decoder.event_trigger_extractors) > 0 or len(self.decoder.event_argument_extractors) > 0 or len(
                self.decoder.extent_extractors) > 0:
            self.decode_event_and_event_argument(serif_doc)
        if hasattr(self.decoder,"entity_relation_extractors") and len(self.decoder.entity_relation_extractors) > 0:
            self.decode_entity_relation(serif_doc)
        if hasattr(self.decoder,"entity_coreference_extractors") and len(self.decoder.entity_coreference_extractors) > 0:
            self.decode_entity_coreference(serif_doc)
        if hasattr(self.decoder,"event_event_relation_extractors") and len(self.decoder.event_event_relation_extractors) > 0:
            self.decode_event_event_relation(serif_doc)
        if hasattr(self.decoder,"event_coref_extractors") and len(self.decoder.event_coref_extractors) > 0:
            self.decode_event_coreference(serif_doc)

    # TODO: Comment out now. Need decision. When a model implement CorpusModel and DocumentModel, how to call regarding
    # Function properly
    # def process_documents(self, serif_doc_list):
    #     assert self.decoder.model_loaded == True
    #     if len(self.decoder.event_event_relation_extractors) > 0:
    #         self.decode_event_event_relation_doc_list(serif_doc_list)
