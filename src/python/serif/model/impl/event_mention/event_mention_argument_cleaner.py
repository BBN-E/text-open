from serif.model.document_model import DocumentModel
from serif.theory.document import Document

class EventMentionArgumentCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(EventMentionArgumentCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            if sentence.event_mention_set is not None:
                for event_mention in sentence.event_mention_set:
                    event_mention.arguments.clear()
        return serif_doc
