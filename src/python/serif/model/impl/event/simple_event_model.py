from serif.model.event_model import EventModel


class SimpleEventModel(EventModel):
    def __init__(self,**kwargs):
        super(SimpleEventModel,self).__init__(**kwargs)
    def get_event_info(self, serif_doc):
        # Put each EventMention into its own Event
        tuples = []
        for sentence in serif_doc.sentences:
            for event_mention in sentence.event_mention_set:
                event_info = (event_mention.event_type, [event_mention])
                tuples.append(event_info)
        return tuples
