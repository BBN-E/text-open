from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class ActorMentionCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(ActorMentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc, Document)
        if serif_doc.actor_entity_set is not None:
            serif_doc.actor_entity_set._children.clear()
        for sentence in serif_doc.sentences:
            if sentence.actor_mention_set is not None:
                sentence.actor_mention_set._children.clear()
        return serif_doc
