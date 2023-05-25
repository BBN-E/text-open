from serif.model.document_model import DocumentModel
from serif.theory.document import Document

class EventCoreferenceCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(EventCoreferenceCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        if serif_doc.event_set is not None:
            serif_doc.event_set._children.clear()
        return serif_doc
