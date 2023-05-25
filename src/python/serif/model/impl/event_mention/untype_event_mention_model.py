

from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class UntypeEventMentionModel(DocumentModel):
    def __init__(self,**kwargs):
        super(UntypeEventMentionModel, self).__init__(**kwargs)


    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            for event_mention in sentence.event_mention_set or ():
                event_mention.event_type = "Event"