from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *

class RelationMentionModel(BaseModel):

    def __init__(self,**kwargs):
        super(RelationMentionModel,self).__init__(**kwargs)

    @abstractmethod
    def get_relation_mention_info(self, sentence):
        """
        :type sentence: Sentence
        :return: List where each element corresponds to one RelationMention.
                 Each element consists fo a relation type string, a left 
                 Mention object, and a right Mention object.
        :rtype: list(tuple(str, Mention, Mention, Tense, Modality))
        """
        pass

    def add_relation_mentions_to_sentence(self, sentence):

        # build necessary structures
        rel_mention_set = sentence.rel_mention_set
        if rel_mention_set is None:
            rel_mention_set =\
                sentence.add_new_relation_mention_set()
            ''':type: RelMentionSet'''
        rel_mention_hash = set([(r.type, r.left_mention, r.right_mention)
                                for r in rel_mention_set])

        # get objects to add
        rel_mentions = []
        extractions = self.get_relation_mention_info(sentence)

        for rel_type, l_mention, r_mention, tense, modality in extractions:

            # if it exists, don't add it
            rel_mention_key = rel_type, l_mention, r_mention
            if rel_mention_key in rel_mention_hash:
                continue
            rel_mention_hash.add(rel_mention_key)

            # construct object
            rel_mention = rel_mention_set.add_new_relation_mention(
                l_mention, r_mention, rel_type, tense, modality)
            rel_mentions.append(rel_mention)

        return rel_mentions

    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            validate_sentence_mention_sets(sentence, serif_doc.docid, i)
            self.add_relation_mentions_to_sentence(sentence)

