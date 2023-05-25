from serif.model.actor_mention_model import ActorMentionModel
from serif.theory.actor_mention import ActorMention
from serif.theory.actor_mention_set import ActorMentionSet
import logging
import math

import serifxml3
import pickle
import enum
import json

from genre.trie import Trie
from genre.hf_model import GENRE

start_delimiter = " [START_ENT] "
end_delimiter = " [END_ENT] "

logger = logging.getLogger(__name__)


class MentionConstraints(enum.Enum):
    # Return a link for every mention
    all = enum.auto()
    # Return links over sentences restricted by the spans of current mentions
    existing = enum.auto()


class GenreMentionLinker(ActorMentionModel):

    def __init__(self, model_path="/nfs/raid83/u13/caml/users/mselvagg_ad/wiki_extract/GENRE/models/hf_entity_disambiguation_aidayago",
                 trie_path="/nfs/raid83/u13/caml/users/mselvagg_ad/wiki_extract/GENRE/data/kilt_titles_trie_dict.pkl",
                 lang_title_2_wikidata_ID_path="/nfs/raid66/u15/aida/releases/entity_linking_files/lang_title2wikidataID-normalized_with_redirect.pkl",
                 mention_mode="all",
                 redirections_path=None, **kwargs):
        super(GenreMentionLinker, self).__init__(**kwargs)

        self.model_path = model_path
        self.trie_path = trie_path
        self.lang_title_2_wikidata_ID_path = lang_title_2_wikidata_ID_path
        self._num_mentions = 0  # keeps count of mentions to assign unique id
        self.mention_mode = MentionConstraints[mention_mode]
        self.redirections_path = redirections_path

    def load_model(self):

        if self.redirections_path:
            with open(self.redirections_path) as f:
                self.redirections = json.load(f)
        else:
            self.redirections = None

        with open(self.trie_path, "rb") as f:
            self.trie = Trie.load_from_dict(pickle.load(f))

        with open(self.lang_title_2_wikidata_ID_path, "rb") as f2:
            self.lang_title_2_wikidata_ID = pickle.load(f2)

        self.model = GENRE.from_pretrained(self.model_path).eval()

        logging.info("GENRE model loaded")

    def unload_model(self):
        del self.model
        self.model = None
        del self.trie
        self.trie = None

    def get_entity_link_info(self, sentence, serif_doc):
        mentions = sentence.mention_set

        if self.mention_mode == MentionConstraints.all:
            sentences = [self.serif_mention_to_genre_sentence(m, serif_doc) for m in mentions]

            output = self.model.sample(
                sentences=sentences,
                num_return_sequences=1,
                prefix_allowed_tokens_fn=lambda batch_id, sent: self.trie.get(sent.tolist()),
            )

            entity_link_info = [(p['text'], p['logprob'].item()) for p in output[0]]
        elif self.mention_mode == MentionConstraints.existing:

            raise NotImplementedError
            # sentences = [sentence.get_original_text_substring(sentence.start_char, sentence.end_char)]
            #
            # output = get_entity_spans(
            #     self.model,
            #     input_sentences=sentences,
            #     mention_trie=Trie([self.model.encode(e)[1:].tolist() for e in [m.text for m in mentions]])
            # )
            # entity_link_info = output[0]
        else:
            raise NotImplementedError

        entity_link_info = [(p['text'], p['logprob'].item()) for p in output[0]]
        return entity_link_info

    def serif_mention_to_genre_sentence(self, m, serif_doc):
        '''
        :type m: serif.theory.Mention
        :return:
        '''

        sentence = m.sentence
        context_left = serif_doc.get_original_text_substring(sentence.start_char, m.start_char - 1).lower()
        mention_text = serif_doc.get_original_text_substring(m.start_char, m.end_char).lower()
        context_right = serif_doc.get_original_text_substring(m.end_char + 1, sentence.end_char).lower()

        genre_sentence = "".join([context_left, start_delimiter, mention_text, end_delimiter, context_right])

        self._num_mentions += 1
        return genre_sentence

    def add_actor_mentions_to_sentence(self, sentence):
        added_actor_mentions = []

        if len(sentence.mention_set) > 0:

            entity_link_info = self.get_entity_link_info(sentence=sentence, serif_doc=sentence.document)
            if self.mention_mode == MentionConstraints.all:
                logger.info(entity_link_info)
                for i, (link, score) in enumerate(entity_link_info):
                    confidence = round(score, 2)
                    if confidence < -1 or ('en', link) not in self.lang_title_2_wikidata_ID:
                        logger.info("low confidence")

                        continue
                    qnode = list(self.lang_title_2_wikidata_ID[('en', link)])[0]
                    logger.info("QNode: {}".format(qnode))

                    mention = sentence.mention_set[i]

                    if self.redirections is not None:
                        if mention.text.lower() in self.redirections["ignore_list"] or \
                                qnode in self.redirections["removed_qnodes"]:
                            logger.info("Skipped qnode: {}".format(qnode))
                            continue
                        elif link in self.redirections["redirections_dict"]:
                            link, qnode = self.redirections["redirections_dict"][link]

                    actor_mentions = ActorMentionModel.add_new_actor_mention(actor_mention_set=sentence.actor_mention_set,
                                                                             mention=mention,
                                                                             actor_db_name=qnode,
                                                                             actor_uid=-1,
                                                                             actor_name=link,
                                                                             source_note="genre",
                                                                             confidence=str(confidence))
                    added_actor_mentions.extend(actor_mentions)

            elif self.mention_mode == MentionConstraints.existing:

                raise NotImplementedError

                # for (mention_offset, genre_mention_length, link) in entity_link_info:
                #     link = link.replace("_", " ")
                #
                #     if ('en', link) not in self.lang_title_2_wikidata_ID:
                #         continue
                #
                #     for mention in sentence.mention_set:
                #         mention_sent_start_char = mention.start_char - sentence.start_char
                #         mention_sent_end_char = mention.end_char - sentence.start_char
                #         serif_mention_length = mention_sent_start_char - mention_sent_end_char
                #
                #         if mention_sent_start_char == mention_offset and genre_mention_length == serif_mention_length:
                #
                #             qnode = list(self.lang_title_2_wikidata_ID[('en', link)])[0]
                #
                #             actor_mentions = ActorMentionModel.add_new_actor_mention(
                #                 actor_mention_set=sentence.actor_mention_set,
                #                 mention=mention,
                #                 actor_db_name=qnode,
                #                 actor_uid=-1,
                #                 actor_name=link,
                #                 source_note="genre")
                #
                #             added_actor_mentions.extend(actor_mentions)
                #             break
            else:
                raise NotImplementedError

        return added_actor_mentions
