import json
import logging
from collections import defaultdict
import enum

import serifxml3
from nlplingo.annotation.ingestion import populate_doc_sentences_with_embeddings_and_annotations
from nlplingo.annotation.serif import to_lingo_sentence
from nlplingo.decoding.decoder import Decoder
from nlplingo.decoding.prediction_theory import SentencePrediction
from nlplingo.text.text_theory import Document as lingoDoc
from serif.model.document_model import DocumentModel

logger = logging.getLogger(__name__)


class OutputFormat(enum.Enum):
    ner = enum.auto()
    entity_linking = enum.auto()


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


def parse_tuple_string(tuple_string):
    return tuple(int(x) for x in tuple_string[1:-1].split(','))


def build_nlplingo_entity_mention_id_to_serif_mention_valuemention_mapping_dict(serif_doc):
    assert isinstance(serif_doc, serifxml3.Document)
    # For why this is implemented in this way, refer to  nlplingo.annotation.serif
    # It turns out that nlplingo would use serif node id as nlplingo.text.text_span.EntityMention.id

    mention_mapping = dict()
    value_mention_mapping = dict()
    for sentence in serif_doc.sentences:
        assert isinstance(sentence, serifxml3.Sentence)
        if sentence.mention_set is None:
            sentence.add_new_mention_set()
        if sentence.value_mention_set is None:
            sentence.add_new_value_mention_set()
        for m in sentence.mention_set:
            mention_mapping[m.id] = m
        for m in sentence.value_mention_set:
            value_mention_mapping[m.id] = m
    return mention_mapping,value_mention_mapping


class NLPLingoNERAdapter(DocumentModel):
    """
    When it's mature, it should replace `nlplingo_decoder`
    """

    def __init__(self, params_path, argparse, output_format='ner', **kwargs):
        super(NLPLingoNERAdapter, self).__init__(**kwargs)
        self.argparse = argparse
        with open(params_path) as fp:
            self.params = json.load(fp)

        self.is_ner_span = False
        for extractor_params in self.params['extractors']:
            if extractor_params['extractor_type'] == "ner_span":
                self.is_ner_span = True

        self.decoder = Decoder(self.params)
        self.decoder.load_model()
        self.max_number_of_tokens_per_sentence = int(kwargs.get("max_number_of_tokens_per_sentence", -1))
        self.output_format = OutputFormat[output_format]

    def reload_model(self):
        self.decoder.reload_model()

    def decode_entity(self, serif_doc):
        docid = serif_doc.docid
        lingo_doc = lingoDoc(docid)
        sent_edt_off_to_sent = dict()
        statistics = defaultdict(int)
        entity_type_statistics = defaultdict(int)
        for st_index, sentence in enumerate(serif_doc.sentences):
            if len(sentence.sentence_theories[0].token_sequence) == 0:
                to_lingo_sentence(serif_doc, st_index,
                                  DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                  lingo_doc=lingo_doc,
                                  add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                  add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                  add_serif_entity_relation_mentions=self.params.get(
                                      'add_serif_entity_entity_relation_mentions', False),
                                  add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                sent_edt_off_to_sent[sentence.start_edt,sentence.end_edt] = sentence
            elif self.max_number_of_tokens_per_sentence > -1 and len(sentence.sentence_theories[0].token_sequence) > self.max_number_of_tokens_per_sentence:
                to_lingo_sentence(serif_doc, st_index,
                                  DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                  lingo_doc=lingo_doc,
                                  add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                  add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                  add_serif_entity_relation_mentions=self.params.get(
                                      'add_serif_entity_entity_relation_mentions', False),
                                  add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt, sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence
            else:
                to_lingo_sentence(serif_doc, st_index, sentence, lingo_doc=lingo_doc,
                                  add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                  add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                  add_serif_entity_relation_mentions=self.params.get(
                                      'add_serif_entity_entity_relation_mentions', False),
                                  add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
                sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt, sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence

        populate_doc_sentences_with_embeddings_and_annotations([lingo_doc], self.params, self.decoder.embeddings)
        entity_document_predictions = self.decoder.decode_ner([lingo_doc])
        mention_mapping, value_mention_mapping = build_nlplingo_entity_mention_id_to_serif_mention_valuemention_mapping_dict(serif_doc)

        for doc_p_docid, doc_p in entity_document_predictions.items():
            assert docid == doc_p_docid
            for sent_p in doc_p.sentences.values():
                assert isinstance(sent_p, SentencePrediction)

                serif_sent = sent_edt_off_to_sent[(sent_p.start, sent_p.end - 1)]
                token_start_edt_to_token = {token.start_edt: token for token in serif_sent.token_sequence}
                token_end_edt_to_token = {token.end_edt: token for token in serif_sent.token_sequence}

                for entity_p in sent_p.entities.values():

                    start_char_edt, end_char_edt = entity_p.start, entity_p.end - 1
                    start_token = token_start_edt_to_token.get(start_char_edt, None)
                    end_token = token_end_edt_to_token.get(end_char_edt, None)

                    if start_token is not None and end_token is not None:

                        start_token_idx, end_token_idx = start_token.index(), end_token.index()

                        assert start_token_idx < len(serif_sent.token_sequence) and end_token_idx < len(serif_sent.token_sequence)
                        entity_type_p = sorted(list(entity_p.entity_type_labels.items()), key=lambda x: x[1], reverse=True)[0][0]
                        if serif_sent.mention_set is None:
                            serif_sent.add_new_mention_set()

                        serif_mention = self.find_existing_serif_mention_by_tokens(start_token_idx,
                                                                                            end_token_idx, serif_sent)
                        if self.output_format == OutputFormat.entity_linking:
                            if serif_mention is None:
                                serif_mention = serif_sent.mention_set.add_new_mention_from_tokens(mention_type='UNDET',
                                                                                                   entity_type='UNDET',
                                                                                                   start_token=serif_sent.token_sequence[start_token_idx],
                                                                                                   end_token=serif_sent.token_sequence[end_token_idx])

                            serif_sent.actor_mention_set.add_new_actor_mention(mention=serif_mention,
                                                                               actor_db_name=entity_type_p,
                                                                               actor_uid=-1,
                                                                               actor_name="UNDET",
                                                                               source_note="nlplingo")
                        else:

                            if self.is_ner_span:
                                if serif_mention is None:
                                    print("Start: {}, End: {}, sent: {}".format(start_token_idx, end_token_idx, serif_sent))
                                    continue
                                serif_mention.entity_subtype = entity_type_p
                            else:
                                # if there's already a mention in serifxml, override its type with the prediction
                                if serif_mention is None:
                                    serif_sent.mention_set.add_new_mention_from_tokens(mention_type='UNDET',
                                                                                       entity_type=entity_type_p,
                                                                                       start_token=serif_sent.token_sequence[start_token_idx],
                                                                                       end_token=serif_sent.token_sequence[end_token_idx])
                                else:
                                    serif_mention.entity_type = entity_type_p

    def find_existing_serif_mention_by_tokens(self, start_token_idx, end_token_idx, serif_sent):
        '''returns existing serifxml mentions in sent if matches tokens, else nothing'''
        serif_mentions = serif_sent.mention_set
        for serif_mention in serif_mentions:
            if serif_sent.token_sequence[start_token_idx:end_token_idx+1] == serif_mention.tokens:
                return serif_mention
        return None

    def process_document(self, serif_doc):
        assert self.decoder.model_loaded == True

        if self.output_format == "entity_linking":
            for i, sentence in enumerate(serif_doc.sentences):
                if sentence.actor_mention_set is None:
                    sentence.add_new_actor_mention_set()

        # TODO: It may make more sense to batch serif_docs, sort them by sentence length, and let nlplingo decode
        # TODO: on sentences with similar length (to avoid padding overheads)
        logger.info("PROCESSING")
        print(self.decoder.entity_extractors)
        if len(self.decoder.entity_extractors) > 0:
            self.decode_entity(serif_doc)
