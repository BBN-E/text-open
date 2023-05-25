from serif.model.relation_model import RelationModel


class SimpleRelationModel(RelationModel):

    def __init__(self, **kwargs):
        super(SimpleRelationModel, self).__init__(**kwargs)

    def add_relations_to_document(self, serif_doc):
        # Put each RelMention into its own Relation
        mention_to_entity_dict = {}
        for entity in serif_doc.entity_set:
            for mention in entity.mentions:
                mention_to_entity_dict[mention] = entity

        tuples = []
        for sentence in serif_doc.sentences:
            for relation_mention in sentence.rel_mention_set:
                left_mention = relation_mention.left_mention
                right_mention = relation_mention.right_mention
                if left_mention not in mention_to_entity_dict:
                    continue
                if right_mention not in mention_to_entity_dict:
                    continue
                left_entity = mention_to_entity_dict[left_mention]
                right_entity = mention_to_entity_dict[right_mention]
                tuples.extend(RelationModel.add_new_relation(serif_doc.relation_set, relation_mention.type, left_entity,
                                                             right_entity, [relation_mention]))
        return tuples
