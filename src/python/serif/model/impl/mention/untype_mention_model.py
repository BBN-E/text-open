

from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class UntypeMentionModel(DocumentModel):
    def __init__(self,**kwargs):
        super(UntypeMentionModel, self).__init__(**kwargs)


    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            for mention in sentence.mention_set or ():
                mention.entity_type = "Mention"

