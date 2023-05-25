from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class EntityCoreferenceCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(EntityCoreferenceCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc, Document)
        if serif_doc.entity_set is not None:
            serif_doc.entity_set._children.clear()
        if serif_doc.event_set is not None:
            serif_doc.event_set._children.clear()
        if serif_doc.relation_set is not None:
            serif_doc.relation_set._children.clear()
        if serif_doc.actor_entity_set is not None:
            serif_doc.actor_entity_set._children.clear()
        return serif_doc
