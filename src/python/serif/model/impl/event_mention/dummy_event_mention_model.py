from serif.model.event_mention_model import EventMentionModel


class DummyEventMentionModel(EventMentionModel):
    def __init__(self,**kwargs):
        super(DummyEventMentionModel,self).__init__(**kwargs)
    def get_event_mention_info(self, sentence):
        # Create an EventMention whenever there is an ORG
        # mentioned in the same sentence as a DRUG
        tuples = []
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
                org_argument_spec = (org_role, org_mention, 1.0)
                drug_argument_spec = (drug_role, drug_mention, 1.0)
                arg_specs = [org_argument_spec, drug_argument_spec]
                if len(times) > 0:
                    arg_specs.append((time_role, times[0], 1.0))
                anchor_node = org_mention.syn_node.head
                event_mention_info = \
                    (event_type, anchor_node, 0.75, arg_specs)
                tuples.append(event_mention_info)
        return tuples
