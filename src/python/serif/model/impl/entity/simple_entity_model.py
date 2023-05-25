from serif.model.mention_coref_model import MentionCoreferenceModel


class SimpleEntityModel(MentionCoreferenceModel):
    def __init__(self, **kwargs):
        super(SimpleEntityModel, self).__init__(**kwargs)

    def add_entities_to_document(self, serif_doc):
        added_entities = list()
        for sentence in serif_doc.sentences:
            for mention in sentence.mention_set:
                if (mention.entity_type == 'OTH' or
                        mention.entity_type == 'UNDET'):
                    continue
                entity_subtype = 'UNDET'
                added_entities.extend(
                    MentionCoreferenceModel.add_new_entity(serif_doc.entity_set, [mention],
                                                           entity_type=mention.entity_type,
                                                           entity_subtype=entity_subtype, is_generic=True))
        return added_entities
