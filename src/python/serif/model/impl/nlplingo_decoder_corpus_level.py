import collections
import json
import logging
import os
from collections import defaultdict

import numpy as np
from nlplingo.annotation.ingestion import populate_doc_sentences_with_embeddings_and_annotations
from nlplingo.annotation.serif import to_lingo_sentence
from nlplingo.decoding.decoder import Decoder, SentencePrediction, EventPrediction, \
    TriggerPrediction, ArgumentPrediction
from nlplingo.embeddings.word_embeddings import DocumentContextualEmbeddings
from nlplingo.text.text_theory import Document as lingoDoc

from nlplingo.extractor.task.eventcoref_cross_document.decoder import SpanPairEventCorefCrossDocumentDecoder

import serifxml3
from serif.model.corpus_model import CorpusModel
from serif.theory.enumerated_type import Tense, Modality
from serif.util.add_nlplingo_event_mentions_to_serifxml_better import nlplingo_event_mention_adder
from serif.util.serifxml_utils import exist_in_event_mention_set
from serif.util.hierarchical_agglomerative_clustering import hierarchical_agglomerative_clustering

logger = logging.getLogger(__name__)

class NLPLingoDecoderCorpusLevel(CorpusModel):
    def __init__(self, params_path, **kwargs):
        super(NLPLingoDecoderCorpusLevel, self).__init__(**kwargs)
        with open(params_path) as fp:
            self.params = json.load(fp)

        self.max_number_of_tokens_per_sentence = int(kwargs.get("max_number_of_tokens_per_sentence", -1))

        self.hac_distance_threshold = float(kwargs.get("hac_distance_threshold", 0.4))

        self.decoder = Decoder(self.params)
        self.decoder.load_model()

    def reload_model(self):
        self.decoder.reload_model()

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
                              add_serif_event_mentions=self.params.get('add_serif_event_mentions', False),
                              add_serif_entity_relation_mentions=self.params.get(
                                  'add_serif_entity_entity_relation_mentions', False),
                              add_serif_prop_adj=self.params.get('add_serif_prop_adj', False))
            if len(sentence.token_sequence) > 0:
                sent_edt_off_to_sent[sentence.sentence_theories[0].token_sequence[0].start_edt,
                                     sentence.sentence_theories[0].token_sequence[-1].end_edt] = sentence

        return lingo_doc, sent_edt_off_to_sent

    def decode_cross_document_event_coreference(self, serif_doc_list):

        docid_to_serif_doc = {serif_doc.docid: serif_doc for serif_doc in serif_doc_list}

        # store event mentions by their docid and sentence/token index
        event_mention_ref_to_event_mention = defaultdict(lambda: defaultdict(int))
        num_event_mentions_in_serif_docs = []

        for serif_doc in serif_doc_list:

            num_event_mentions_in_serif_doc = 0
            for i, sentence in enumerate(serif_doc.sentences):

                for em in sentence.event_mention_set:
                    num_event_mentions_in_serif_doc += 1
                    event_mention_ref_to_event_mention[serif_doc.docid][(i, em.semantic_phrase_start, em.semantic_phrase_end)] = em

            num_event_mentions_in_serif_docs.append(num_event_mentions_in_serif_doc)

        num_event_mentions_in_corpus = sum(num_event_mentions_in_serif_docs)
        logger.info("# event mentions in corpus: {}".format(num_event_mentions_in_corpus))

        # convert serif docs to lingo docs
        docs = [self.convert_serif_doc_to_lingo_doc(serif_doc)[0] for serif_doc in serif_doc_list]

        # apply decoder
        cross_document_event_coref_pairs_with_probs = self.decoder.decode_event_coreference_cross_document(docs)

        # gather event mention refs from decoder's predictions, and store predicted pairwise similarity scores between them
        event_mentions_from_spanpair_predictions = set()
        pairwise_similarity = defaultdict(lambda: defaultdict(int))
        for (em1, em2, p) in cross_document_event_coref_pairs_with_probs:

            event_mentions_from_spanpair_predictions.add(em1)
            event_mentions_from_spanpair_predictions.add(em2)

            pairwise_similarity[em1][em2] = p
            pairwise_similarity[em2][em1] = p

        event_mentions_from_spanpair_predictions = sorted(list(event_mentions_from_spanpair_predictions), key=lambda x: x[0])  # sort by docid

        # assert len(all_event_mentions_from_spanpair_predictions) == num_event_mentions_in_corpus, \
        #         "{} vs {}".format(str(len(all_event_mentions_from_spanpair_predictions)), str(num_event_mentions_in_corpus))
        logger.info("# event mentions from spanpair predictions: {}".format(str(len(event_mentions_from_spanpair_predictions))))

        # apply hierarchical agglomerative clustering based on corpus-level spanpair predictions' similarity scores
        cluster_label_to_event_mentions = hierarchical_agglomerative_clustering(event_mentions_from_spanpair_predictions,
                                                                                pairwise_similarity,
                                                                                distance_threshold=self.hac_distance_threshold)

        # iterate over predicted clusters (coreferent mentions)
        for cluster_id, event_mentions in cluster_label_to_event_mentions.items():

            # sort corpus-level coreferent event mentions by docid
            docid_to_event_mentions_for_cluster = defaultdict(list)
            for (docid, sentence_index, start_token_index, end_token_index) in event_mentions:
                event_mention = event_mention_ref_to_event_mention[docid][(sentence_index, start_token_index, end_token_index)]
                docid_to_event_mentions_for_cluster[docid].append(event_mention)

            # create within-document clusters corresponding to corpus-level cluster
            for docid, event_mentions_for_cluster in docid_to_event_mentions_for_cluster.items():
                serif_doc = docid_to_serif_doc[docid]
                serif_doc.event_set.add_new_event(event_mentions=event_mentions_for_cluster,
                                                  event_type=event_mentions_for_cluster[0].event_type,  # TODO figure out how to resolve event type
                                                  cross_document_id=cluster_id)

    def process_documents(self, serif_doc_list):
        assert self.decoder.model_loaded == True
        self.decode_cross_document_event_coreference(serif_doc_list)
