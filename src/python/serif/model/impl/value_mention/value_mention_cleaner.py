from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class ValueMentionCleaner(DocumentModel):

    def __init__(self, **kwargs):
        super(ValueMentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)

        for sentence in serif_doc.sentences or []:
            if sentence.value_mention_set is not None:
                sentence.value_mention_set._children.clear()
            else:
                sentence.add_new_value_mention_set()
        if serif_doc.value_set is not None:
            serif_doc.value_set._children.clear()
        return serif_doc