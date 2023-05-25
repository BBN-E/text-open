import json
import logging

logger = logging.getLogger(__name__)

from serif.model.corpus_model import CorpusModel
from serif.model.impl.nlplingo_adapter.utils import DummySentence
from serif.model.impl.nlplingo_adapter.ner import write_ner_back_to_serifxml

from nlplingo.decoding.decoder import Decoder
from nlplingo.text.text_theory import Document as lingoDoc
from nlplingo.annotation.serif import to_lingo_sentence, add_entities, add_document_events
from nlplingo.annotation.ingestion import populate_doc_sentences_with_embeddings_and_annotations

class NLPLingoDecoder(CorpusModel):
    def __init__(self, params_path, **kwargs):
        super(NLPLingoDecoder, self).__init__(**kwargs)
        with open(params_path) as fp:
            self.params = json.load(fp)

        logger.info("NLPLingo param we got is: \n{}".format(json.dumps(self.params, indent=4, sort_keys=True, ensure_ascii=False)))
        self.decoder = Decoder(self.params)
        self.max_number_of_tokens_per_sentence = int(kwargs.get("max_number_of_tokens_per_sentence", -1))

    def load_model(self):
        self.decoder.load_model()

    def process_documents(self, serif_docs):
        lingo_docs = []
        serif_doc_id_to_serif_doc = dict()
        for serif_doc in serif_docs:
            serif_doc_id_to_serif_doc[serif_doc.docid] = serif_doc
            docid = serif_doc.docid
            lingo_doc = lingoDoc(docid)
            sent_edt_off_to_sent = dict()
            for st_index, sentence in enumerate(serif_doc.sentences):
                if sentence.mention_set is None:
                    sentence.add_new_mention_set()
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

        # Only NER is supported at his point
        document_predictions = self.decoder.decode_ner(lingo_docs)
        for doc_id, document_p in document_predictions.items():
            write_ner_back_to_serifxml(serif_doc_id_to_serif_doc[doc_id], document_p)
