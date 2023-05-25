from serif.model.document_model import DocumentModel
from serif.theory.document import Document

class EventMentionCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(EventMentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        if serif_doc.event_set is not None:
            serif_doc.event_set._children.clear()
        if serif_doc.event_event_relation_mention_set is not None:
            serif_doc.event_event_relation_mention_set._children.clear()
        for sentence in serif_doc.sentences or []:
            if sentence.event_mention_set is not None:
                sentence.event_mention_set._children.clear()
            else:
                sentence.add_new_event_mention_set()
        return serif_doc
