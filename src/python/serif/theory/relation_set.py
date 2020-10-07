from serif.theory.relation import Relation
from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _ChildTheoryElementList


class RelationSet(SerifSequenceTheory):
    _children = _ChildTheoryElementList('Relation')

    def add_relation(self, relation):
        self._children.append(relation)

    def add_new_relation(
            self, relation_mentions, relation_type, left_entity, right_entity):
        relation = self.construct_relation(
            relation_mentions, relation_type, left_entity, right_entity)
        self.add_relation(relation)
        return relation

    def construct_relation(
            self, relation_mentions, relation_type, left_entity, right_entity):
        relation = Relation(owner=self)
        relation.relation_type = relation_type
        relation.rel_mentions = relation_mentions
        relation.left_entity = left_entity
        relation.right_entity = right_entity
        relation.tense = "Unspecified"
        relation.modality = "Asserted"
        relation.document.generate_id(relation)
        return relation
