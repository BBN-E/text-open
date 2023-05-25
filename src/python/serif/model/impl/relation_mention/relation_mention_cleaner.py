from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class RelationMentionCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(RelationMentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc, Document)
        if serif_doc.relation_set is not None:
            serif_doc.relation_set._children.clear()
        for sentence in serif_doc.sentences or []:
            if sentence.rel_mention_set is not None:
                sentence.rel_mention_set._children.clear()
        return serif_doc
