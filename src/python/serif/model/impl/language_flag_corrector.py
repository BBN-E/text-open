from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class LanguageFlagCorrector(DocumentModel):
    def __init__(self, language,**kwargs):
        super(LanguageFlagCorrector, self).__init__(**kwargs)
        self.language = language

    def process_document(self, serif_doc):
        serif_doc.language = self.language