from serif.model.event_mention_coref_model import EventMentionCoreferenceModel


class SimpleEventModel(EventMentionCoreferenceModel):
    def __init__(self, **kwargs):
        super(SimpleEventModel, self).__init__(**kwargs)

    def add_new_events_to_document(self, serif_doc):
        added_events = list()
        for sentence in serif_doc.sentences:
            for event_mention in sentence.event_mention_set:
                added_events.extend(EventMentionCoreferenceModel.add_new_event(serif_doc.event_set, [event_mention],
                                                                               event_type=event_mention.event_type))
        return added_events
