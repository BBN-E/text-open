from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *


class RelationModel(BaseModel):

    def __init__(self,**kwargs):
        super(RelationModel,self).__init__(**kwargs)

    @abstractmethod
    def get_relation_info(self, serif_doc):
        """
        :type serif_doc: Document
        :return: List where each element corresponds to one Relation. Each
                 element consists of a relation type string, a left Entity 
                 object, a right Entity object, and a list RelMention objects
                 which comprise the Relation.
        :rtype: list(tuple(str, Entity, Entity, list(RelMention)))
        """
        pass

    def add_relations_to_document(self, serif_doc):
        # build necessary structure
        relation_set = serif_doc.relation_set
        if relation_set is None:
            relation_set = serif_doc.add_new_relation_set()
            ''':type: RelationSet'''
        
        relations = []
        relation_info = self.get_relation_info(serif_doc)
        for relation_type, left_entity, right_entity, relation_mentions\
                in relation_info:
            relation = relation_set.add_new_relation(
                relation_mentions, relation_type, left_entity, right_entity)
            relations.append(relation)
        return relations
                
    def process(self, serif_doc):
        validate_doc_entity_sets(serif_doc)
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            validate_sentence_relation_mention_sets(
                sentence, serif_doc.docid, i)
        self.add_relations_to_document(serif_doc)
