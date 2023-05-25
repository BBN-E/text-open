import logging

from serif.model.document_model import DocumentModel

logger = logging.getLogger(__name__)


class UntypedEntityMentionCleaner(DocumentModel):
    """Makes Mentions for existing Names"""

    def __init__(self, **kwargs):
        super(UntypedEntityMentionCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        good_mention_in_doc = set()
        for entity in serif_doc.entity_set or ():
            good_mention_in_doc.update(entity.mentions)

        for sentence in serif_doc.sentences:
            good_mention_in_sent = set()
            for rel_mention in sentence.rel_mention_set or ():
                good_mention_in_sent.add(rel_mention.left_mention)
                good_mention_in_sent.add(rel_mention.right_mention)
            for event_mention in sentence.event_mention_set or ():
                for event_mention_arg in event_mention.arguments:
                    if event_mention_arg.mention is not None:
                        good_mention_in_sent.add(event_mention_arg.mention)
            for actor_mention in sentence.actor_mention_set or ():
                good_mention_in_sent.add(actor_mention.mention)
            for mention in sentence.mention_set or ():
                if mention.entity_type != "UNDET":
                    good_mention_in_sent.add(mention)
                elif mention in good_mention_in_doc:
                    good_mention_in_sent.add(mention)
            if sentence.mention_set is None:
                sentence.add_new_mention_set()
            sentence.mention_set._children.clear()
            sentence.mention_set._children.extend(good_mention_in_sent)
