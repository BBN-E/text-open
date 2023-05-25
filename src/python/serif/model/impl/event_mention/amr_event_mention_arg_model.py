from serif.model.event_mention_model import EventMentionModel
import json
from serif.util.amr_utils import find_amr_event_nodes_in_sentence, find_event_mentions_aligned_to_amr_node


class AMREventMentionModel(EventMentionModel):
    def __init__(self, xpo_overlay_path="/nfs/raid66/u15/aida/releases/entity_linking_files/xpo_v4.1_draft.json",
                 add_new_events=True, **kwargs):
        super(AMREventMentionModel, self).__init__(**kwargs),
        self.xpo_overlay_path = xpo_overlay_path

    def load_model(self):
        with open(self.xpo_overlay_path, "r") as f:
            xpo_overlay = json.load(f)

        self.pd_roleset_to_event = {}
        for dwd_id, event in xpo_overlay["events"].items():
            if "pb_roleset" in event:
                argument_mappings = {}
                for arg in event["arguments"]:
                    if "A0" in arg["name"]:
                        argument_mappings[":arg0"] = arg["name"]
                    elif "A1" in arg["name"]:
                        argument_mappings[":arg1"] = arg["name"]
                    elif "A2" in arg["name"]:
                        argument_mappings[":arg2"] = arg["name"]
                    elif "A3" in arg["name"]:
                        argument_mappings[":arg3"] = arg["name"]
                    elif "AM_loc" in arg["name"]:
                        argument_mappings[":location"] = arg["name"]

                self.pd_roleset_to_event[event["pb_roleset"].replace(".", "-")] = (dwd_id, argument_mappings)

    def get_mention_from_token(self, start_token, end_token, sentence):

        for mention in sentence.mention_set:
            if mention.start_token == start_token and mention.end_token == end_token:
                return mention

        return sentence.mention_set.add_new_mention_from_tokens("UNDET", "UNDET", start_token, end_token)

    def get_start_and_end_token_from_amr_node(self, amr_parse, amr_node):
        alignment_token_indices = json.loads(amr_node.alignment_token_indices)
        if len(alignment_token_indices) <= 0:
            return None, None
        start_token = amr_parse.token_sequence[alignment_token_indices[0]]
        end_token = amr_parse.token_sequence[alignment_token_indices[-1]]
        return start_token, end_token

    def add_event_mentions_to_sentence(self, sentence):

        amr_parse = sentence.amr_parse

        event_nodes = find_amr_event_nodes_in_sentence(sentence)
        for amr_event_node in event_nodes:

            start_token, end_token = self.get_start_and_end_token_from_amr_node(amr_parse, amr_event_node)
            if start_token is None or end_token is None:
                continue
            em = self.add_new_event_mention(sentence.event_mention_set, "UNDEF", start_token, end_token, model="AMR")[0]

            argument_mappings = None
            if amr_event_node.content in self.pd_roleset_to_event:
                em.event_type, argument_mappings = self.pd_roleset_to_event[amr_event_node.content]
            else:
                em.event_type = amr_event_node.content

            for (child_node, arg_label) in zip(amr_event_node._children, json.loads(amr_event_node._outgoing_amr_rels)):
                if child_node.alignment_token_indices is not None and not child_node.is_attribute_node:
                    start_token, end_token = self.get_start_and_end_token_from_amr_node(amr_parse, child_node)
                    if start_token is None or end_token is None:
                        continue
                    mention = self.get_mention_from_token(start_token, end_token, sentence)
                    if argument_mappings:
                        if arg_label in argument_mappings:
                            arg_label = argument_mappings[arg_label]
                    EventMentionModel.add_new_event_mention_argument(em, arg_label, mention, 1.0)

        return sentence.event_mention_set
