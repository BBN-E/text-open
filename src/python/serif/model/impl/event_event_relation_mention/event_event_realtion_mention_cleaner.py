from serif.model.document_model import DocumentModel
from serif.theory.document import Document

class EventEventRelationMentionCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(EventEventRelationMentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        if serif_doc.event_event_relation_mention_set is not None:
            serif_doc.event_event_relation_mention_set._children.clear()
        return serif_doc
