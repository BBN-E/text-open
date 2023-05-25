from serif.model.relation_mention_model import RelationMentionModel

from serif.theory.enumerated_type import Tense, Modality


# Modified from DrugFinderRelationMentionModel
class DogFoodFinderRelationMentionModel(RelationMentionModel):

    def __init__(self, **kwargs):
        super(DogFoodFinderRelationMentionModel, self).__init__(**kwargs)

    def add_relation_mentions_to_sentence(self, sentence):
        ret = []
        relation_type = 'DogHasFood'
        dogs = [m for m in sentence.mention_set if m.entity_type == 'DOG']
        foods = [m for m in sentence.mention_set if m.entity_type == 'FOOD']
        for dog in dogs:
            s1 = int(dog.syn_node.start_char)
            e1 = int(dog.syn_node.end_char)
            closest = None
            closest_distance = -1
            for food in foods:
                s2 = int(food.syn_node.start_char)
                e2 = int(food.syn_node.end_char)
                distance = min(abs(s1 - e2), abs(s2 - e1))
                if closest is None and closest_distance < 0:
                    closest = food
                    closest_distance = distance
                elif distance < closest_distance:
                    closest = food
                    closest_distance = distance
            if closest is not None:
                ret.extend(self.add_or_update_relation_mention(sentence.rel_mention_set, relation_type, dog, closest,
                                                               Tense.Unspecified,
                                                               Modality.Asserted))
        return ret
