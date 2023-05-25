from serif.model.entity_linker import EntityLinkerModel
from serif.theory.actor_entity import ActorEntity
from serif.theory.actor_mention import ActorMention
from serif.theory.actor_entity_set import ActorEntitySet
from serif.theory.actor_mention_set import ActorMentionSet
import logging
import math

import serifxml3

import blink.main_dense as main_dense
import argparse
from collections import defaultdict, Counter


def sigmoid(x):
  return 1 / (1 + math.exp(-x))


class BlinkEntityLinker(EntityLinkerModel):

    def __init__(self, models_path="/nfs/raid66/u11/users/brozonoy-ad/BLINK/models/", **kwargs):
        super(BlinkEntityLinker, self).__init__(**kwargs)

        config = {
            "test_entities": None,
            "test_mentions": None,
            "interactive": False,
            "top_k": 10,
            "biencoder_model": models_path + "biencoder_wiki_large.bin",
            "biencoder_config": models_path + "biencoder_wiki_large.json",
            "entity_catalogue": models_path + "entity.jsonl",
            "entity_encoding": models_path + "all_entities_large.t7",
            "crossencoder_model": models_path + "crossencoder_wiki_large.bin",
            "crossencoder_config": models_path + "crossencoder_wiki_large.json",
            "fast": False,  # set this to be true if speed is a concern
            "output_path": "logs/"  # logging directory
        }

        self.args = argparse.Namespace(**config)

        self.models = main_dense.load_models(self.args, logger=None)

        self._num_mentions = 0  # keeps count of mentions to assign unique id

    def get_entity_link_info(self, doc_mentions, serif_doc):
        data_to_link = [self.serif_mention_to_blink_datapoint(m, serif_doc) for m in doc_mentions]
        _, _, _, _, _, predictions, scores, = main_dense.run(self.args, None, *self.models, test_data=data_to_link)
        entity_link_info = [(p[0], s[0]) for p,s in zip(predictions, scores)]
        return entity_link_info

    def serif_mention_to_blink_datapoint(self, m, serif_doc):
        '''
        :type m: serif.theory.Mention
        :return:
        '''

        sentence = serif_doc.sentences[m.sent_no]
        context_left = serif_doc.get_original_text_substring(sentence.start_char, m.start_char-1).lower()
        mention_text = serif_doc.get_original_text_substring(m.start_char, m.end_char).lower()
        context_right = serif_doc.get_original_text_substring(m.end_char+1, sentence.end_char).lower()

        blink_datapoint = {
            "id": f"{m.id}_{self._num_mentions}",
            "label": "unknown",
            "label_id": -1,
            "context_left": context_left.lower(),
            "mention": " ".join(mention_text.lower().split()[:32]),
            "context_right": context_right.lower(),
        }

        self._num_mentions += 1
        return blink_datapoint
