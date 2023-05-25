from serif.model.document_model import DocumentModel
from serif.theory.document import Document


class MentionCleaner(DocumentModel):
    def __init__(self,**kwargs):
        super(MentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            if sentence.mention_set is not None:
                sentence.mention_set._children.clear()
            else:
                sentence.add_new_mention_set()
            if sentence.rel_mention_set is not None:
                sentence.rel_mention_set._children.clear()
            if sentence.proposition_set is not None:
                sentence.proposition_set._children.clear()
            if sentence.actor_mention_set is not None:
                sentence.actor_mention_set._children.clear()
            if sentence.event_mention_set is not None:
                for event_mention in sentence.event_mention_set:
                    white_listed_arguments = list()
                    for argument in event_mention.arguments:
                        if argument.mention is None:
                            white_listed_arguments.append(argument)
                    event_mention.arguments = white_listed_arguments
        if serif_doc.entity_set is not None:
            serif_doc.entity_set._children.clear()
        if serif_doc.event_set is not None:
            serif_doc.event_set._children.clear()
        if serif_doc.relation_set is not None:
            serif_doc.relation_set._children.clear()
        if serif_doc.actor_entity_set is not None:
            serif_doc.actor_entity_set._children.clear()
        return serif_doc

