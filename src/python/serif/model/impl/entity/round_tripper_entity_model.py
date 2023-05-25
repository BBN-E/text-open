import serifxml3
from serif.model.impl.round_tripper_util import find_matching_mention
from serif.model.mention_coref_model import MentionCoreferenceModel


class RoundTripperEntityModel(MentionCoreferenceModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperEntityModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides EntityModel.get_entity_info
    def add_entities_to_document(self, document):

        if self.serif_doc.entity_set is None:
            return []

        # Create list of tuples each specifying an Entity object.
        # These will be placed in an EntitySet object on the
        # document.
        new_entities = []
        for entity in self.serif_doc.entity_set:
            list_of_new_mentions = []
            for mention in entity.mentions:
                sent_no = mention.sent_no
                new_mention_sentence = document.sentences[sent_no]
                new_mention = find_matching_mention(mention, new_mention_sentence)
                list_of_new_mentions.append(new_mention)
            new_entities.extend(
                MentionCoreferenceModel.add_new_entity(document.entity_set, list_of_new_mentions,
                                                       entity_type=entity.entity_type,
                                                       entity_subtype=entity.entity_subtype,
                                                       is_generic=entity.is_generic,
                                                       canonical_name=entity.canonical_name,
                                                       entity_guid=entity.entity_guid,
                                                       confidence=entity.confidence,
                                                       mention_confidences=entity.mention_confidences
                                                       ))
        return new_entities
