from serif.model.event_event_relation_mention_model import EventEventRelationMentionModel


class DummyEventEventRelationMentionModel(EventEventRelationMentionModel):
    def __init__(self, **kwargs):
        super(DummyEventEventRelationMentionModel, self).__init__(**kwargs)

    def add_event_event_relation_mentions_to_document(self, serif_doc):
        ret = list()
        for sentence in serif_doc.sentences:
            if len(sentence.event_mention_set) < 2:
                continue
            ems = sentence.event_mention_set
            ret.extend(EventEventRelationMentionModel.add_new_event_event_relation_mention(
                serif_doc.event_event_relation_mention_set,
                "DUMMY_RELATION",
                0.75,
                "TESTMODEL",
                ems[0], ems[1]))
        return ret
