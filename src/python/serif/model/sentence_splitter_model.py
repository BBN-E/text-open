from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.theory.sentence import Sentence


class SentenceSplitterModel(BaseModel):

    def __init__(self,**kwargs):
        super(SentenceSplitterModel,self).__init__(**kwargs)


    @abstractmethod
    def get_sentence_info(self, region):
        """
        :type serif_doc: Document
        :return: List where each element corresponds to one Sentence. Each
                 element consists of a Region, a sentence start offset,
                 and an sentence end offset.
        :rtype: list(tuple(Region, int, int))
        """
        pass

    def add_sentences_to_document(self, serif_doc):
        sentences = serif_doc.add_new_sentences()
        
        for region in serif_doc.regions:
            regions_starts_ends = self.get_sentence_info(region)
            for region_start_end in regions_starts_ends:
                sentences.add_new_sentence(start_char=region_start_end[1],
                                           end_char=region_start_end[2],
                                           region_id=region_start_end[0])
        return sentences

    def process(self, serif_doc):
        self.add_sentences_to_document(serif_doc)

