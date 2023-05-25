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

class ConfidenceCalibration(CorpusModel):
    def __init__(self, aggregate_word_pair_count, **kwargs):
        super(ConfidenceCalibration, self).__init__(**kwargs)
        self.aggregate_word_pair_count = aggregate_word_pair_count
        self.conf_threshold = 0.65
        self.conf_threshold_remap = 0.35


    def remap_confidence(self, confidence):
        # remap 0.65-1 to range between 0.35-1
        old_range = (1 - self.conf_threshold)
        new_range = (1 - self.conf_threshold_remap)
        new_confidence = (((confidence - self.conf_threshold) * new_range) / old_range) + self.conf_threshold_remap
        return new_confidence


    def process_documents(self, serif_doc_list):
        try:
            # organize EER's by head/tail word pairs (last word in head/tail phrase)
            word_pair_to_eerm_dict = dict()
            for serif_doc_idx, serif_doc in enumerate(serif_doc_list):
                icews_eerm_set = set()
                word_pair_to_serif_eerms = dict()
                docid = serif_doc.docid
                for serif_eerm in serif_doc.event_event_relation_mention_set or []:
                    # Avoid processing icews_eer
                    if len(serif_eerm.icews_event_mention_relation_arguments) > 0:
                        icews_eerm_set.add(serif_eerm)
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
                        if word_pair_pruned not in word_pair_to_serif_eerms:
                            word_pair_to_serif_eerms[word_pair_pruned] = set()
                        word_pair_to_serif_eerms[word_pair_pruned].add(serif_eerm)
                eerm_set = serif_doc.add_new_event_event_relation_mention_set()  # kill the eerm_set
                for eerm in icews_eerm_set: # add all icews eerm's
                    eerm_set.add_event_event_relation_mention(eerm)
                word_pair_to_eerm_dict[docid] = (word_pair_to_serif_eerms, eerm_set)

            with open(self.aggregate_word_pair_count, 'rb') as handle:
                aggregate_word_pair_to_count = pickle.load(handle)

            # Compute max / min count, to use as fraction for multiplier
            max_ct = 0
            min_ct = float('inf')
            for ct in aggregate_word_pair_to_count.values():
                if ct > max_ct:
                    max_ct = ct
                if ct < min_ct:
                    min_ct = ct

            # scale confidences accordingly
            # set multiplier to a value between 1.0 and 1.2 if count >= 2
            # else, if count == 1, downweight by a slight factor of 0.9
            SINGLE_MULTIPLIER = 0.9
            scale_range = [1.0, 1.2]


            REMAPPED_THRESHOLD = 0.39 # currently 10 percent loss; set to 0.44 for 25 percent loss 

            for docid in word_pair_to_eerm_dict:
                word_pair_to_serif_eerms, eerm_set = word_pair_to_eerm_dict[docid]
                for word_pair, serif_eerms in word_pair_to_serif_eerms.items():
                    val = aggregate_word_pair_to_count[word_pair]
                    # if min_ct == max_ct, all the word pairs have the same frequency, so no multiplier is computed
                    if min_ct != max_ct:
                        if val == 1:
                            multiplier = SINGLE_MULTIPLIER
                        else:
                            frac = (float(val - min_ct) / (max_ct - min_ct))
                            multiplier = scale_range[0] + frac * (scale_range[1] - scale_range[0])

                    for serif_eerm in serif_eerms:
                        if hasattr(serif_eerm, 'confidence'):
                            if min_ct != max_ct:
                                conf = multiplier * serif_eerm.confidence
                            else:
                                conf = serif_eerm.confidence
                            if conf >= 1:
                                conf = float(1)
                            else:
                                conf = float(conf)
                            if conf > self.conf_threshold:
                                remapped_confidence = self.remap_confidence(conf)
                                if remapped_confidence > REMAPPED_THRESHOLD:
                                    serif_eerm.confidence = remapped_confidence 
                                    eerm_set.add_event_event_relation_mention(serif_eerm)
        except Exception:
            logger.warning(traceback.format_exc())
