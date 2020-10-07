from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.theory.pos import POS
from serif.model.validate import *

class PartOfSpeechModel(BaseModel):

    def __init__(self,**kwargs):
        super(PartOfSpeechModel,self).__init__(**kwargs)

    @abstractmethod
    def get_part_of_speech_info(self, sentence):
        """
        :type Sentence: sentence
        :return: List where each element corresponds to one POS. Each
                 element consists of a token, a tag, and a universal 
                 pos tag.
        :rtype: list(tuple(Token, str, str))
        """
        pass

    def add_pos_to_sentence(self, sentence):
        part_of_speech_sequence = sentence.add_new_part_of_speech_sequence(0.7)
        part_of_speech_info = self.get_part_of_speech_info(sentence)
        for p in part_of_speech_info:
            part_of_speech_sequence.add_new_pos(p[0], p[1], p[2])

    def process(self, serif_doc):
        validate_doc_sentences(serif_doc)
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            self.add_pos_to_sentence(sentence)
