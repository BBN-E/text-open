import serifxml3
from serif.model.impl.round_tripper_util import find_matching_entity, find_matching_relation_mention
from serif.model.relation_model import RelationModel
from serif.theory.sentence import Sentence


class RoundTripperRelationModel(RelationModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperRelationModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides RelationModel.get_relation_info
    def add_relations_to_document(self, document):

        # Create list of tuples, each specifying a Relation object. 
        # These will be placed in a RelationSet object on the 
        # document
        relation_info = []
        for relation in self.serif_doc.relation_set or ():

            # RelationMentions make up a Relation
            list_of_new_relation_mentions = []
            for relation_mention in relation.rel_mentions:
                sent_no = relation_mention.owner_with_type(Sentence).sent_no
                new_sentence = document.sentences[sent_no]
                new_relation_mention = find_matching_relation_mention(relation_mention, new_sentence)
                list_of_new_relation_mentions.append(new_relation_mention)

            new_left_entity = find_matching_entity(relation.left_entity, document)
            new_right_entity = find_matching_entity(relation.right_entity, document)
            relation_info.extend(
                RelationModel.add_new_relation(document.relation_set, relation.relation_type, new_left_entity,
                                               new_right_entity,
                                               list_of_new_relation_mentions,
                                               tense=relation.tense,
                                               modality=relation.modality,
                                               confidence=relation.confidence,
                                               model=relation.model,
                                               pattern=relation.pattern
                                               ))

        return relation_info
