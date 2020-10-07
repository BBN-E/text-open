from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.theory.token import Token
from serif.model.validate import *

class TokenizerModel(BaseModel):

    def __init__(self,**kwargs):
        super(TokenizerModel,self).__init__(**kwargs)

    @abstractmethod
    def get_token_info(self, sentence):
        """
        :type Sentence: sentence
        :return: List where each element corresponds to one Token. Each
                 element consists of a string, a start char offset,
                 and an end char offset.
        :rtype: list(tuple(str, int, int))
        """
        pass

    def add_tokens_to_sentence(self, sentence):
        token_sequence = sentence.add_new_token_sequence(0.7)
        texts_starts_ends = self.get_token_info(sentence)
        for t in texts_starts_ends:
            token_sequence.add_new_token(t[1], t[2], t[0])

    def process(self, serif_doc):
        for sentence in serif_doc.sentences:
            self.add_tokens_to_sentence(sentence)
