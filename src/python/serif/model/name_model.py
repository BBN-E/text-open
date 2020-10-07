from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *


class NameModel(BaseModel):

    def __init__(self,**kwargs):
        super(NameModel,self).__init__(**kwargs)

    @abstractmethod
    def get_name_info(self, sentence):
        """
        :type sentence: Sentence
        :return: List where each element corresponds to on Name. Each
                 element consists of an entity type string, a start Token
                 object, and an end Token object.
        :rtype: list(tuple(str, Token, Token))
        """
        pass

    def add_names_to_sentence(self, sentence):
        # Build NameTheory if necessary (doc not invalid, just needs more work)
        name_theory = sentence.name_theory
        if name_theory is None:
            name_theory = sentence.add_new_name_theory()
        name_hash = set([(n.entity_type, n.start_token, n.end_token)
                         for n in name_theory])

        names = []
        # find entity_type-start_token-end_token triples
        types_starts_ends = self.get_name_info(sentence)
        # for each name found build Name object & add to sentence's NameTheory
        for entity_type, start_token, end_token in types_starts_ends:

            # if it exists, don't add it
            name_key = (entity_type, start_token, end_token)
            name_already_exists = name_key in name_hash
            if name_already_exists:
                continue
            name_hash.add(name_key)

            name = name_theory.add_new_name(
                entity_type, start_token, end_token)
            names.append(name)
        return names

    def process(self, serif_doc):
        #if bio_file: # if external name info provided
        #   bio_sents = self.preprocess_bio_file(bio_file)

        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            #if bio_file: self.add_names_to_sentence(sentence, bio_sent=bio_sents[i])
            #else: self.add_names_to_sentence(sentence, bio_sent=None)
            self.add_names_to_sentence(sentence)
