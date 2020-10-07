from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *

class DependencyModel(BaseModel):
    
    def __init__(self,**kwargs):
        super(DependencyModel,self).__init__(**kwargs)
    
    # Normally, models override get_*_info() (which returns tuples), 
    # but for dependency parsers, the tuple structure would be too
    # complicated, so we override this higher level function, which
    # needs to do all the work of adding dependency sets and proposition
    # objects
    @abstractmethod
    def add_dependencies_to_sentence(self, sentence):
        pass

    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            #validate_sentence_mention_sets(sentence, serif_doc.docid, i)
            # Since we can set max_tokens, we can now end up without a 
            # MentionSet
            if sentence.mention_set:
                self.add_dependencies_to_sentence(sentence)

