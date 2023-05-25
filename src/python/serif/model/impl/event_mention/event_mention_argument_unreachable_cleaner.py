from serif.model.document_model import DocumentModel
from serif.theory.document import Document
from serif.theory.mention import Mention
from serif.theory.value_mention import ValueMention
from serif.theory.event_mention import EventMention
from serif.xmlio import DanglingPointer

class EventMentionArgumentUnreachableCleaner(DocumentModel):
    def __init__(self, **kwargs):
        super(EventMentionArgumentUnreachableCleaner, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        assert isinstance(serif_doc, Document)
        for sentence in serif_doc.sentences or []:
            all_possible_candidates = set()
            for mention in sentence.mention_set or ():
                all_possible_candidates.add(mention)
            for value_mention in sentence.value_mention_set or ():
                all_possible_candidates.add(value_mention)
            for event_mention in sentence.event_mention_set or ():
                all_possible_candidates.add(event_mention)

            if sentence.event_mention_set is not None:
                for event_mention in sentence.event_mention_set:
                    event_mention_args = list(event_mention.arguments)
                    allowed_event_mention_args = list()
                    for event_mention_arg in event_mention_args:
                        value = event_mention_arg.value
                        if isinstance(value, Mention) or isinstance(value, ValueMention) or isinstance(value,
                                                                                                       EventMention):
                            if value in all_possible_candidates:
                                allowed_event_mention_args.append(event_mention_arg)
                        elif isinstance(value, DanglingPointer):
                            pass
                        else:
                            raise NotImplementedError("Cannot support argument type {}".format(type(value)))
                    event_mention.arguments.clear()
                    event_mention.arguments.extend(allowed_event_mention_args)
        return serif_doc
