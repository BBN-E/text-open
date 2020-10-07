from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *

import re

class ParserModel(BaseModel):

    def __init__(self,**kwargs):
        super(ParserModel,self).__init__(**kwargs)
        self.add_heads = False
        if "add_heads" in kwargs:
            self.add_heads = True
        
    @abstractmethod
    def get_parse_info(self, serif_doc):
        """
        :type serif_doc: Document
        :return: List where each element corresponds to one Parse. Each
                 element consists of a TokenSequence and a treebank string.
        :rtype: list(tuple(TokenSequence, str))
        """
        pass

    def add_parses_to_sentence(self, sentence):
        treebank_string = self.get_parse_info(sentence)
        try:
            if treebank_string is not None:
                parse = sentence.add_new_parse(
                    "0.9", sentence.token_sequence,
                    re.sub(pattern="\s+", repl=" ", string=treebank_string))
                if self.add_heads:
                    parse.add_heads()
        except:
            pass

    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            self.add_parses_to_sentence(sentence)
