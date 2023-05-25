from serif.model.event_mention_model import EventMentionModel


class EmptyEventMentionSetModel(EventMentionModel):
    """Adds empty event mention set to sentence.
    """

    def __init__(self, **kwargs):
        super(EmptyEventMentionSetModel, self).__init__(**kwargs)

    def add_event_mentions_to_sentence(self, sentence):
        return []
