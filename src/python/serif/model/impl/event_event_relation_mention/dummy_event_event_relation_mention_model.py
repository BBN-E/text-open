from serif.model.event_event_relation_mention_model import EventEventRelationMentionModel


class DummyEventEventRelationMentionModel(EventEventRelationMentionModel):
    def __init__(self,**kwargs):
        super(DummyEventEventRelationMentionModel,self).__init__(**kwargs)
    def get_event_event_relation_mention_info(self, serif_doc):
        # Look for two event mentions in one sentence and
        # create EventEventRelationMention from them
        tuples = []
        for sentence in serif_doc.sentences:
            if len(sentence.event_mention_set) < 2:
                continue
            ems = sentence.event_mention_set
            eerm_info = ("DUMMY_RELATION", 0.75, "TESTMODEL",
                         [("arg1", ems[0]), ("arg2", ems[1])])
            tuples.append(eerm_info)
        return tuples
