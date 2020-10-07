import logging
from abc import abstractmethod

from serif.theory.enumerated_type import MentionType
from serif.model.base_model import BaseModel
from serif.model.validate import *

logger = logging.getLogger(__name__)

class MentionModel(BaseModel):

    def __init__(self, **kwargs):
        self.modify_existing_mention = False
        if "modify_existing_mention" in kwargs:
            self.modify_existing_mention = True
        super(MentionModel, self).__init__(**kwargs)

    @abstractmethod
    def get_mention_info(self, sentence):
        """
        :type sentence: Sentence
        :return: List where each element corresponds to one Mention. Each
                 element consists of an entity type string, a mention type
                 string, and a SynNode which specifies where in the parse
                 tree the Mention was found.
        :rtype: list(tuple(str, str, SynNode))
        """
        pass

    def add_mentions_to_sentence(self, sentence):

        # build necessary structures
        mention_set = sentence.mention_set
        if mention_set is None:
            mention_set = sentence.add_new_mention_set()
            """:type: MentionSet"""

        mention_hash = set(
            [(m.entity_type, m.mention_type == 'NAME', m.syn_node)
             for m in mention_set])
        
        # get objects to add
        mentions = []
        mention_info = self.get_mention_info(sentence)

        # An opportunity of adding or changing syn node here
        syn_node_to_mentions = dict()

        if self.modify_existing_mention:
            for mention in sentence.mention_set:
                if mention.syn_node:
                    syn_node_to_mentions.setdefault(mention.syn_node,list()).append(mention)


        for entity_type, mention_type, syn_node in mention_info:
            if self.modify_existing_mention and len(syn_node_to_mentions.get(syn_node,list())) > 0:
                logger.warning("There are pre-existing mentions at SynNode {} , will change their type now.".format(syn_node))
                for mention in syn_node_to_mentions.get(syn_node,list()):

                    mention.entity_type = entity_type
                    mention.mention_type = MentionType(mention_type.lower())
            else:
                # if it exists, don't add it
                is_name = False
                if mention_type == "NAME":
                    is_name = True
                mention_key = entity_type, is_name, syn_node
                if mention_key in mention_hash:
                    continue
                mention_hash.add(mention_key)
                # construct mention
                mention = mention_set.add_new_mention(
                    syn_node, mention_type, entity_type)
                if self.modify_existing_mention:
                    syn_node_to_mentions.setdefault(syn_node,list()).append(mention)
                mentions.append(mention)

        return mentions

    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            # Since we can set a max_tokens for BeneparParser, we end up with
            # no parse sometimes
            #validate_sentence_parse(sentence, serif_doc.docid, i)
            if sentence.parse:
                self.add_mentions_to_sentence(sentence)
