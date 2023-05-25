import json
import logging
from collections import defaultdict

from nlplingo.annotation.ingestion import populate_doc_sentences_with_embeddings_and_annotations
from nlplingo.annotation.serif import to_lingo_sentence, add_entities, add_document_events
from nlplingo.decoding.decoder import Decoder
from nlplingo.text.text_theory import Document as lingoDoc

from serif.model.corpus_model import CorpusModel
from serif.util.add_nlplingo_event_mentions_to_serifxml_better import nlplingo_event_mention_adder

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


class NLPLingoDecoderGranular(CorpusModel):
    """
    When it's mature, it should replace `nlplingo_decoder`
    """

    def __init__(self, params_path, **kwargs):
        super(NLPLingoDecoderGranular, self).__init__(**kwargs)
        with open(params_path) as fp:
            self.params = json.load(fp)
        logger.info("NLPLingo param we got is: \n{}".format(json.dumps(self.params, indent=4, sort_keys=True, ensure_ascii=False)))
        self.decoder = Decoder(self.params)
        # self.decoder.load_model()
        self.max_number_of_tokens_per_sentence = int(kwargs.get("max_number_of_tokens_per_sentence", -1))

    def reload_model(self):
        self.decoder.reload_model()

    def create_annotated_docs(self, serif_docs):
        lingo_docs = []
        for serif_doc in serif_docs:
            docid = serif_doc.docid
            lingo_doc = lingoDoc(docid)
            sent_edt_off_to_sent = dict()
            for st_index, sentence in enumerate(serif_doc.sentences):
                if len(sentence.sentence_theories[0].token_sequence) == 0:
                    to_lingo_sentence(serif_doc, st_index,
                                      DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                      lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                      add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                      add_serif_entity_relation_mentions=self.params.get(
                                          'add_serif_entity_entity_relation_mentions', False),
                                      add_serif_prop_adj=self.params.get('add_serif_prop_adj', False),
                                      allow_anchor_as_event_argument=self.params.get('allow_anchor_as_event_argument',
                                                                                     False),
                                      allow_event_mention_as_event_argument=self.params.get(
                                          'allow_event_mention_as_event_argument', False))

                    sent_edt_off_to_sent[sentence.start_edt, sentence.end_edt] = sentence
                elif self.max_number_of_tokens_per_sentence > -1 and len(
                        sentence.sentence_theories[0].token_sequence) > self.max_number_of_tokens_per_sentence:
                    # TODO check whether this happens often
                    to_lingo_sentence(serif_doc, st_index,
                                      DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                      lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                      add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                      add_serif_entity_relation_mentions=self.params.get(
                                          'add_serif_entity_entity_relation_mentions', False),
                                      add_serif_prop_adj=self.params.get('add_serif_prop_adj', False),
                                      allow_anchor_as_event_argument=self.params.get('allow_anchor_as_event_argument',
                                                                                     False),
                                      allow_event_mention_as_event_argument=self.params.get(
                                          'allow_event_mention_as_event_argument', False))

                    sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt,
                                         sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence
                else:
                    to_lingo_sentence(serif_doc, st_index, sentence, lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=True,
                                      add_serif_event_mentions=True,
                                      add_serif_entity_relation_mentions=False,
                                      add_serif_prop_adj=False,
                                      allow_anchor_as_event_argument=True,
                                      allow_event_mention_as_event_argument=True)

                    sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt,
                                         sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence

            if True:
                add_entities(serif_doc, lingo_doc)

            if True:  # this is actually adding document level events
                add_document_events(serif_doc, lingo_doc)

            populate_doc_sentences_with_embeddings_and_annotations([lingo_doc], self.params, self.decoder.embeddings)
            lingo_docs.append(lingo_doc)
        return lingo_docs

    def decode_event_and_event_argument(self, serif_docs):
        lingo_docs = []
        for serif_doc in serif_docs:
            docid = serif_doc.docid
            lingo_doc = lingoDoc(docid)
            sent_edt_off_to_sent = dict()
            for st_index, sentence in enumerate(serif_doc.sentences):
                if len(sentence.sentence_theories[0].token_sequence) == 0:
                    to_lingo_sentence(serif_doc, st_index,
                                      DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                      lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                      add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                      add_serif_entity_relation_mentions=self.params.get(
                                          'add_serif_entity_entity_relation_mentions', False),
                                      add_serif_prop_adj=self.params.get('add_serif_prop_adj', False),
                                      allow_anchor_as_event_argument=self.params.get('allow_anchor_as_event_argument',
                                                                                     False),
                                      allow_event_mention_as_event_argument=self.params.get(
                                          'allow_event_mention_as_event_argument', False))

                    sent_edt_off_to_sent[sentence.start_edt, sentence.end_edt] = sentence
                elif self.max_number_of_tokens_per_sentence > -1 and len(
                        sentence.sentence_theories[0].token_sequence) > self.max_number_of_tokens_per_sentence:
                    # TODO check whether this happens often
                    to_lingo_sentence(serif_doc, st_index,
                                      DummySentence(st_index, sentence.start_edt, sentence.end_edt),
                                      lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                      add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                      add_serif_entity_relation_mentions=self.params.get(
                                          'add_serif_entity_entity_relation_mentions', False),
                                      add_serif_prop_adj=self.params.get('add_serif_prop_adj', False),
                                      allow_anchor_as_event_argument=self.params.get('allow_anchor_as_event_argument',
                                                                                     False),
                                      allow_event_mention_as_event_argument=self.params.get(
                                          'allow_event_mention_as_event_argument', False))

                    sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt,
                                         sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence
                else:
                    to_lingo_sentence(serif_doc, st_index, sentence, lingo_doc=lingo_doc,
                                      add_serif_entity_mentions=self.params.get('add_serif_entity_mentions', True),
                                      add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                                      add_serif_entity_relation_mentions=self.params.get(
                                          'add_serif_entity_entity_relation_mentions', False),
                                      add_serif_prop_adj=self.params.get('add_serif_prop_adj', False),
                                      allow_anchor_as_event_argument=self.params.get('allow_anchor_as_event_argument',
                                                                                     False),
                                      allow_event_mention_as_event_argument=self.params.get(
                                          'allow_event_mention_as_event_argument', False))

                    sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt,
                                         sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence

            if self.params.get('add_serif_entity_mentions', True):
                add_entities(serif_doc, lingo_doc)

            if self.params.get('add_serif_event_mentions', False):  # this is actually adding document level events
                add_document_events(serif_doc, lingo_doc)

            populate_doc_sentences_with_embeddings_and_annotations([lingo_doc], self.params, self.decoder.embeddings)
            lingo_docs.append(lingo_doc)
        # lingo_docs_with_ann = self.create_annotated_docs(serif_docs=serif_docs)
        event_document_predictions, doc_id_to_event_and_event_arg_feature = self.decoder.decode_trigger_and_argument_for_granular(
            lingo_docs)

        for serif_doc in serif_docs:
            docid = serif_doc.docid
            # Write back code start
            statistics = defaultdict(int)
            event_type_statistics = defaultdict(int)
            event_arg_type_statistics = defaultdict(int)

            if docid in event_document_predictions:
                nlplingo_event_mention_adder(serif_doc, event_document_predictions[docid], statistics,
                                             event_type_statistics, event_arg_type_statistics)

            for k in sorted(statistics):
                logger.info('{} {}'.format(k, str(statistics[k])))

            for key, value in event_type_statistics.items():
                logger.info('EVENT TYPE COUNT {}: {}'.format(key, value))

            for key, value in event_arg_type_statistics.items():
                logger.info('EVENT ARG TYPE COUNT {}: {}'.format(key, value))

    # def process(self, serif_doc):
    #     assert self.decoder.model_loaded == True
    #     # TODO: It may make more sense to batching serif_docs, sort them by sentence length, and let nlplingo decode
    #     # TODO: on sentences has similar length (to avoid padding overheads)
    #     if len(self.decoder.event_trigger_extractors) > 0 or len(self.decoder.event_argument_extractors) > 0:
    #         self.decode_event_and_event_argument(serif_doc)

    def process_documents(self, serif_docs):
        self.decode_event_and_event_argument(serif_docs)
