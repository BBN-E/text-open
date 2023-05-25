import os, json
import logging
import traceback
import serifxml3
from serif.model.corpus_model import CorpusModel
from serif.theory.sentence import Sentence
from serif.theory.event_mention import EventMention
import pickle

from collections import Counter

logger = logging.getLogger(__name__)

def get_event_anchor(serif_em: EventMention):
    sentence = serif_em.owner_with_type(Sentence)
    serif_sentence_tokens = sentence.sentence_theory.token_sequence
    if serif_em.semantic_phrase_start is not None:
        serif_em_semantic_phrase_text = " ".join(i.text for i in serif_sentence_tokens[
                                                                 int(serif_em.semantic_phrase_start):int(
                                                                     serif_em.semantic_phrase_end) + 1])
        return serif_em_semantic_phrase_text
    elif len(serif_em.anchors) > 0:
        return " ".join(i.anchor_node.text for i in serif_em.anchors)
    else:
        return serif_em.anchor_node.text

class WordPairCount(CorpusModel):
    def __init__(self, argparse, **kwargs):
        super(WordPairCount, self).__init__(**kwargs)
        self.argparse = argparse
        self.output_directory = argparse.output_directory


    def process_documents(self, serif_doc_list):
        try:
            # record the count for each word pair
            word_pair_counts = Counter()

            for serif_doc_idx, serif_doc in enumerate(serif_doc_list):
                for serif_eerm in serif_doc.event_event_relation_mention_set or []:
                    # Avoid processing icews_eer
                    if len(serif_eerm.icews_event_mention_relation_arguments) > 0:
                        continue

                    serif_em_arg1 = None
                    serif_em_arg2 = None

                    for arg in serif_eerm.event_mention_relation_arguments:
                        if arg.role == "arg1":
                            serif_em_arg1 = arg.event_mention
                        if arg.role == "arg2":
                            serif_em_arg2 = arg.event_mention
                    if serif_em_arg1 is not None and serif_em_arg2 is not None:
                        left_anchor_txt = get_event_anchor(serif_em_arg1)
                        right_anchor_txt = get_event_anchor(serif_em_arg2)

                        # Use last word in event phrase as representative of head/tail
                        head_word = left_anchor_txt.split()[-1].lower()
                        tail_word = right_anchor_txt.split()[-1].lower()
                        word_pair_pruned = head_word + '#' + tail_word
                        word_pair_counts[word_pair_pruned] += 1

            # collect word pairs and counts into a dictionary
            word_pair_to_count = {}
            for tuples in word_pair_counts.most_common():
                ct = tuples[1]
                word_pair_to_count[tuples[0]] = ct

            with open(self.output_directory + '/word_pair_counts_' + str(os.getpid()) + '.pkl', 'wb') as handle:
                pickle.dump(word_pair_to_count, handle, protocol=pickle.HIGHEST_PROTOCOL)
        except Exception:
            logger.warning(traceback.format_exc())