from serif.model.actor_entity_model import ActorEntityModel


class EmptyActorEntityModel(ActorEntityModel):
    """Adds empty event mention set to sentence.
    """

    def __init__(self, **kwargs):
        super(EmptyActorEntityModel, self).__init__(**kwargs)

    def add_actor_entities_to_document(self, serif_doc):
        raise NotImplementedError

    def process_document(self, serif_doc):
        serif_doc.actor_entity_set._children.clear()