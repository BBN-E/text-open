from serif.model.relation_mention_model import RelationMentionModel

from serif.theory.enumerated_type import Tense, Modality

# Modified from DrugFinderRelationMentionModel
class DogFoodFinderRelationMentionModel(RelationMentionModel):

    def __init__(self,**kwargs):
        super(DogFoodFinderRelationMentionModel,self).__init__(**kwargs)

    def get_relation_mention_info(self, sentence):
        tuples = []
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
                tuples.append((relation_type, closest, food, Tense.Unspecified, Modality.Asserted))
        return tuples
