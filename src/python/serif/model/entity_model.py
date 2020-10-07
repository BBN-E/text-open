from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *


class EntityModel(BaseModel):
    def __init__(self,**kwargs):
        super(EntityModel,self).__init__(**kwargs)
    @abstractmethod
    def get_entity_info(self, serif_doc):
        """
        :type serif_doc: Document
        :return: List where each element corresponds to one Entity. Each
                 element consists of a entity type string, an entity subtype
                 string, and a list of Mention objects which comprise the 
                 Entity.
        :rtype: list(tuple(str, str, list(Mention)))
        """
        pass

    def add_entities_to_document(self, serif_doc):
        # build necessary structure
        entity_set = serif_doc.entity_set
        if entity_set is None:
            entity_set = serif_doc.add_new_entity_set()
            ''':type: EntitySet'''
        
        entities = []
        entity_info = \
            self.get_entity_info(serif_doc)
        for entity_type, entity_subtype, mention_group, is_generic in entity_info:
            entity = entity_set.add_new_entity(
                mention_group, entity_type, entity_subtype, is_generic)
            entities.append(entity)
        return entities
    
    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            validate_sentence_mention_sets(sentence, serif_doc.docid, i)
        self.add_entities_to_document(serif_doc)
