from serif.model.relation_mention_model import RelationMentionModel

from serif.theory.enumerated_type import Tense, Modality


class DrugFinderRelationMentionModel(RelationMentionModel):

    def __init__(self, **kwargs):
        super(DrugFinderRelationMentionModel, self).__init__(**kwargs)

    def add_relation_mentions_to_sentence(self, sentence):
        ret = []
        relation_type = 'DISCOVERER'
        persons = [m for m in sentence.mention_set if m.entity_type == 'PER']
        drugs = [m for m in sentence.mention_set if m.entity_type == 'DRUG']
        for drug in drugs:
            s1 = int(drug.syn_node.start_char)
            e1 = int(drug.syn_node.end_char)
            closest = None
            closest_distance = -1
            for person in persons:
                s2 = int(person.syn_node.start_char)
                e2 = int(person.syn_node.end_char)
                distance = min(abs(s1 - e2), abs(s2 - e1))
                if closest is None and closest_distance < 0:
                    closest = person
                    closest_distance = distance
                elif distance < closest_distance:
                    closest = person
                    closest_distance = distance
            if closest is not None:
                ret.extend(self.add_or_update_relation_mention(sentence.rel_mention_set, relation_type, closest, drug,
                                                               Tense.Unspecified,
                                                               Modality.Asserted))
        return ret
