from serif.model.event_mention_coref_model import EventMentionCoreferenceModel


class IntraDocumentSamTypeEventCoreferenceModel(EventMentionCoreferenceModel):
    def __init__(self, **kwargs):
        super(IntraDocumentSamTypeEventCoreferenceModel, self).__init__(**kwargs)

    def add_new_events_to_document(self, serif_doc):
        event_type_to_event_mentions = dict()
        for sentence in serif_doc.sentences:
            for event_mention in sentence.event_mention_set or ():
                event_types = set()
                event_types.add(event_mention.event_type)
                event_types.update(t.event_type for t in event_mention.event_types)
                event_types.update(t.event_type for t in event_mention.factor_types)
                event_types = set(filter(lambda x:x is not None,event_types))
                for event_type in event_types:
                    event_type_to_event_mentions.setdefault(event_type,set()).add(event_mention)
        ret = list()
        for event_type,event_mentions in event_type_to_event_mentions.items():
            ret.extend(EventMentionCoreferenceModel.add_new_event(
                serif_doc.event_set, list(event_mentions),event_type=event_type
            ))
        return ret