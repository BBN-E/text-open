from serif.model.mention_coref_model import MentionCoreferenceModel

class SingltonEntityCleaner(MentionCoreferenceModel):
    def __init__(self, **kwargs):
        super(SingltonEntityCleaner, self).__init__(**kwargs)

    def add_entities_to_document(self, serif_doc):

        bk_entity_set = list(serif_doc.entity_set._children)
        serif_doc.entity_set._children.clear()

        for entity in bk_entity_set:
            if len(entity.mentions) > 1:
                serif_doc.entity_set.add_entity(entity)
