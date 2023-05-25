from serif.model.event_mention_model import EventMentionModel


class DummyEventMentionModel(EventMentionModel):

    def __init__(self, **kwargs):
        super(DummyEventMentionModel, self).__init__(**kwargs)

    def add_event_mentions_to_sentence(self, sentence):
        # Create an EventMention whenever there is an ORG
        # mentioned in the same sentence as a DRUG
        event_mentions = []
        event_type = 'DUMMY_EVENT'
        org_role = 'participant_org'
        drug_role = 'participant_drug'
        time_role = 'event_time'

        orgs = [m for m in sentence.mention_set if m.entity_type == 'ORG']
        drugs = [m for m in sentence.mention_set if m.entity_type == 'DRUG']
        times = [vm for vm in
                 sentence.value_mention_set if vm.value_type == 'TIMEX2.TIME']

        for org_mention in orgs:
            if len(drugs) == 0:
                continue
            for drug_mention in drugs:
                anchor_node = None
                if org_mention.syn_node is not None:
                    anchor_node = org_mention.syn_node.head
                new_event_mentions = EventMentionModel.add_new_event_mention(sentence.event_mention_set, event_type,
                                                                             anchor_node.start_token,
                                                                             anchor_node.end_token,
                                                                             score=0.75)

                for em in new_event_mentions:
                    event_mentions.append(em)
                    EventMentionModel.add_new_event_mention_argument(em, org_role, org_mention, 1.0)
                    EventMentionModel.add_new_event_mention_argument(em, drug_role, drug_mention, 1.0)
                    if len(times) > 0:
                        EventMentionModel.add_new_event_mention_argument(em, time_role, times[0], 1.0)

        return event_mentions
