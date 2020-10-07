import re
import serifxml3
import logging

from serif.model.validate import *
from serif.model.event_mention_model import EventMentionModel


logger = logging.getLogger(__name__)
whitespace_re = re.compile(r"\s+")


def get_mention_str(mention: serifxml3.Mention, mention_to_entity):
    if mention in mention_to_entity:
        entity = mention_to_entity[mention]
        assert isinstance(entity, serifxml3.Entity)
        if entity.canonical_name is not None:
            return entity.canonical_name
        longest_name = ""
        for mention in entity.mentions:
            if mention.mention_type.value == "name":
                mention_head_text = mention.head.text
                mention_head_text = whitespace_re.sub(" ", mention_head_text)
                if len(mention_head_text) > len(longest_name):
                    longest_name = mention_head_text
        if len(longest_name) > 0:
            return longest_name
    return whitespace_re.sub(" ", mention.atomic_head.text)


def get_value_mention_str(value_mention: serifxml3.ValueMention):
    serif_doc = value_mention.document
    if value_mention.value_type == "TIMEX2.TIME":
        nt = value_mention.get_normalized_time() or value_mention.text
        return nt
    return value_mention.text

def print_event_mention(event_mention:serifxml3.EventMention):
    serif_doc = event_mention.document
    mention_to_entity = dict()
    for entity in serif_doc.entity_set or list():
        assert isinstance(entity, serifxml3.Entity)
        for mention in entity.mentions:
            mention_to_entity[mention] = entity
    anchor_node = event_mention.anchor_node
    assert isinstance(event_mention, serifxml3.EventMention)
    anchor_text = anchor_node.text
    anchor_text = anchor_text.replace("\t", " ").replace("\n", " ")
    argument_role_text_pairs = list()
    for event_mention_arg in event_mention.arguments:
        assert isinstance(event_mention_arg, serifxml3.EventMentionArg)
        if event_mention_arg.mention is not None:
            mention = event_mention_arg.mention
            assert isinstance(mention, serifxml3.Mention)
            mention_text = get_mention_str(mention, mention_to_entity)
            mention_text = mention_text.replace("\t", " ").replace("\n", " ")
            argument_role_text_pairs.append((event_mention_arg.role,mention_text))
        elif event_mention_arg.value_mention is not None:
            value_mention = event_mention_arg.value_mention
            assert isinstance(value_mention, serifxml3.ValueMention)
            value_mention_text = get_value_mention_str(value_mention)
            value_mention_text = value_mention_text.replace("\t", " ").replace("\n", " ")
            argument_role_text_pairs.append((event_mention_arg.role, value_mention_text))
    logger.debug("Event type: {}, anchor: {}. {}".format(event_mention.event_type,anchor_text," ".join("<Argument type: {}, argument: {}>".format(i[0],i[1]) for i in argument_role_text_pairs)))

# Modified from DummyEventMentionModel
class DummyEventMentionDemoModel(EventMentionModel):
    def __init__(self,**kwargs):
        super(DummyEventMentionDemoModel,self).__init__(**kwargs)
    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            event_mention_set = sentence.event_mention_set
            if event_mention_set is None:
                event_mention_set = \
                    sentence.add_new_event_mention_set()
                ''':type: EventMentionSet'''
            logger.debug("SentIdx: {} sentence: {}".format(i,sentence.text))
            logger.debug("Before adding")
            for event_mention in sentence.event_mention_set:
                print_event_mention(event_mention)

            self.add_event_mentions_to_sentence(sentence)

            logger.debug("After adding")
            for event_mention in sentence.event_mention_set:
                print_event_mention(event_mention)

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
