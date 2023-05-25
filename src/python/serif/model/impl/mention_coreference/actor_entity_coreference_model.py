from serif.model.mention_coref_model import MentionCoreferenceModel
from collections import Counter


class ActorEntityCoreferenceModel(MentionCoreferenceModel):
    '''
      Assumes that entity coreference and actor entity creation have both been run; merges entities if their actor
      entities have the same actor_db_name
      '''
    def __init__(self, **kwargs):
        super(ActorEntityCoreferenceModel, self).__init__(**kwargs)

    def merge_entities(self, merged_entity, entity_list):
        combined_mention_set = merged_entity.mentions
        for entity in entity_list:
            combined_mention_set.extend(entity.mentions)

        entity_type, entity_subtype = 'UNDET', 'UNDET'
        if len(combined_mention_set) > 0:
            entity_types = set([m.entity_type for m in combined_mention_set if m.entity_type != 'UNDET'])
            if len(entity_types) > 0:
                entity_type = MentionCoreferenceModel.get_best_type(entity_types)

            entity_subtypes = [m.entity_subtype for m in combined_mention_set if m.entity_subtype != 'UNDET']
            if len(entity_subtypes) > 0:
                count_data = Counter(entity_subtypes)
                entity_subtype = count_data.most_common(1)[0][0]

        merged_entity.mentions = combined_mention_set
        merged_entity.entity_type = entity_type
        merged_entity.entity_subtype = entity_subtype
        return merged_entity

    def add_entities_to_document(self, serif_doc):

        merged_entity_set = []
        merged_actor_entity_set = []

        actor_db_name_to_actor_entities = {}
        entity_to_actor_entity = {}

        for actor_entity in serif_doc.actor_entity_set:
            entity_to_actor_entity[actor_entity.entity] = actor_entity

            if actor_entity.actor_db_name not in actor_db_name_to_actor_entities:
                actor_db_name_to_actor_entities[actor_entity.actor_db_name] = []
            actor_db_name_to_actor_entities[actor_entity.actor_db_name].append(actor_entity)

        for actor_db_name, actor_entities in actor_db_name_to_actor_entities.items():
            merged_actor_entity = actor_entities[0]
            merged_entity = merged_actor_entity.entity

            if len(actor_entities) <= 1:
                merged_actor_entity_set.append(merged_actor_entity)
                merged_entity_set.append(merged_entity)
                continue

            entity_list = []
            for actor_entity in actor_entities[1:]:
                merged_actor_entity.actor_mentions.extend(actor_entity.actor_mentions)
                entity_list.append(actor_entity.entity)

            merged_entity = self.merge_entities(merged_entity, entity_list)
            merged_entity_set.append(merged_entity)
            merged_actor_entity_set.append(merged_actor_entity)

        # keep entities that had no corresponding actor entity
        for entity in serif_doc.entity_set:
            if entity not in entity_to_actor_entity:
                merged_entity_set.append(entity)

        serif_doc.actor_entity_set._children = merged_actor_entity_set
        serif_doc.entity_set._children = merged_entity_set

        return merged_entity_set