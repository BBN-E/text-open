import sys
from collections import defaultdict
import utilities
from misc.name_finder import NameFinder

# serifxml3 WIP copy is now local
#sys.path.append("/nfs/raid66/u14/users/azamania/TextGroup/Active/Projects/SERIF/python")
from serifxml3 import *


if len(sys.argv) != 3:
    print ("Usage: " + sys.argv[0] + " input-serifxml-file output-serifxml-file")
    sys.exit(1)

input_file, output_file = sys.argv[1:]

doc = Document(input_file)

# Very simple name finder
name_finder = NameFinder()

for sentence in doc.sentences:

    # Get existing name theory from sentence
    name_theory = sentence.name_theory
    # Get existing mention set from sentence
    mention_set = sentence.mention_set

    # Run name finder
    drug_name_start_and_end_tokens =\
        name_finder.find_drug_name_tokens(sentence)
    discovery_start_and_end_tokens = name_finder.find_discovery_tokens(sentence)

    names = []
    mentions = []
    for (start_token, end_token) in drug_name_start_and_end_tokens:

        #
        # EXERCISE NAME APIS
        #
        # For each drug found, make a new Name and add it to the name theory
        if len(name_theory) % 2:
            drug_name = name_theory.construct_and_add_name(
                "DRUG-construct_and_add", start_token, end_token)
        else:
            drug_name = name_theory.construct_name(
                "DRUG-construct_then_add", start_token, end_token)
            names.append(drug_name)

        #
        # EXERCISE MENTION APIS
        #
        # Also, create a mention if we can find an appropriate NP for the name
        syn_node = utilities.get_covering_np(sentence, drug_name)
        if syn_node:
            if len(mention_set) % 2:
                mention = mention_set.construct_and_add_mention(
                    syn_node, "NAME",
                    drug_name.entity_type + '-construct_and_add')
            else:
                mention = mention_set.construct_mention(
                    syn_node, "NAME",
                    drug_name.entity_type + '-construct_then_add')
                mentions.append(mention)
    name_theory.add_names(names)
    mention_set.add_mentions(mentions)

    #
    # EXERCISE RELATION MENTION SET API
    #
    # Create RelMentionSet in every sentence, fill with
    # RelMention objects that connect drugs with PER
    # or ORGs names
    rel_mention_set = sentence.construct_and_add_relation_mention_set()
    per_or_org_mentions = []
    drug_mentions = []
    for mention in mention_set:
        if (mention.mention_type == MentionType.name and
                (mention.entity_type == "PER" or mention.entity_type == "ORG")):
            per_or_org_mentions.append(mention)
        if mention.entity_type.startswith("DRUG"):
            drug_mentions.append(mention)

    #
    # EXERCISE RELATION MENTION APIS
    #
    rel_mentions = []
    for drug_mention in drug_mentions:
        for i, per_or_org_mention in enumerate(per_or_org_mentions):
            if i % 2:
                rel_mention = rel_mention_set\
                    .construct_and_add_relation_mention(
                        per_or_org_mention,
                        drug_mention,
                        "Drug-Relation-construct_and_add")
            else:
                rel_mention = rel_mention_set.construct_relation_mention(
                    per_or_org_mention, drug_mention,
                    "Drug-Relation-construct_then_add")
                rel_mentions.append(rel_mention)
    rel_mention_set.add_relation_mentions(rel_mentions)

    #
    # EXERCISE EVENT MENTION SET API
    #
    # Create EventMentionSet in every sentence and fill with
    # EventEventRelationMention objects that connect "discovery"-like events to
    # one another across the whole document
    event_mention_set = sentence.construct_and_add_event_mention_set()
    discovery_mentions = []
    for (start_token, end_token) in discovery_start_and_end_tokens:

        syn_node = utilities.get_covering_syn_node(
            sentence, start_token, end_token, ['VB'])

        #
        # EXERCISE EVENT MENTION APIS
        #
        # make a new EventMention and add it
        if start_token.text == 'discovered':
            discovery_mx = event_mention_set.construct_and_add_event_mention(
                "Discover-construct_and_add")
        elif start_token.text == 'developed':
            discovery_mx = event_mention_set.construct_and_add_event_mention(
                "Discover-construct_and_add", anchor_node=syn_node)
        elif start_token.text == 'isolated':
            discovery_mx = event_mention_set.construct_event_mention(
                "Discover-construct_then_add", anchor_node=syn_node)
            discovery_mentions.append(discovery_mx)
        else:
            discovery_mx = event_mention_set.construct_event_mention(
                "Discover-construct_then_add")
            discovery_mentions.append(discovery_mx)

        #
        # EXERCISE EVENT MENTION ARGUMENT APIS
        #
        persons = [m for m in mention_set if m.entity_type == 'PER']
        drugs = [m for m in mention_set if m.entity_type.startswith('DRUG')]
        times = [v for v in sentence.value_mention_set
                 if v.value_type == 'TIMEX2.TIME']
        if persons and drugs:
            discovery_mx.construct_and_add_mention_argument('Discoverer', persons[0])
            discovery_mx.construct_and_add_mention_argument('Discovery', drugs[0])
        if times:
            discovery_mx.construct_and_add_value_mention_argument('Time', times[0])

    event_mention_set.add_event_mentions(discovery_mentions)

#
# EXERCISE EVENT EVENT RELATION MENTION SET APIS
#
event_event_rel_mention_set = \
    doc.construct_and_add_event_event_relation_mention_set()

#
# EXERCISE EVENT EVENT RELATION MENTION API
#
# get all discovery event mentions in all sentences in document
discovery_mentions = []
for sentence in doc.sentences:
    for event_mention in sentence.event_mention_set:
        if event_mention.event_type.startswith('Discover'):
            discovery_mentions.append(event_mention)

event_event_relation_mentions = []
for i in range(len(discovery_mentions) - 1):
    left_event_mention = discovery_mentions[i]
    right_event_mention = discovery_mentions[i+1]
    if i % 2:
        event_event_rel_mention_set\
            .construct_and_add_event_event_relation_mention(
                left_event_mention, right_event_mention,
                "precedent-construct_and_add")
    else:
        event_event_relation_mention = event_event_rel_mention_set\
            .construct_event_event_relation_mention(
                left_event_mention, right_event_mention,
                "precedent-construct_then_add")
        event_event_relation_mentions.append(event_event_relation_mention)
event_event_rel_mention_set.add_event_event_relation_mentions(
    event_event_relation_mentions)

#
# EXERCISE ENTITY SET API
#
entity_set = doc.construct_and_add_entity_set()

#
# EXERCISE ENTITY APIS
#
# overly simplistic coreference map
entity_text_to_mentions = defaultdict(list)
for sentence in doc.sentences:
    for mention in sentence.mention_set:
        entity_text_to_mentions[mention.text].append(mention)

entity_texts = list(entity_text_to_mentions.keys())
for i in range(len(entity_texts)//2):
    mentions = entity_text_to_mentions[entity_texts[i]]
    entity_type = mentions[0].entity_type
    entity_subtype = mentions[0].entity_subtype + '-construct_and_add'
    entity = entity_set.construct_and_add_entity(
        mentions, entity_type, entity_subtype)

entities = []
for i in range(len(entity_texts)//2, len(entity_texts)):
    mentions = entity_text_to_mentions[entity_texts[i]]
    entity_type = mentions[0].entity_type
    entity_subtype = mentions[0].entity_subtype + '-construct_then_add'
    entity = entity_set.construct_entity(mentions, entity_type, entity_subtype)
    entities.append(entity)
entity_set.add_entities(entities)

#
# EXERCISE EVENT SET API
#
event_set = doc.construct_and_add_event_set()

#
# EXERCISE EVENT APIS
#
# overly simplistic coreference map
event_to_event_mentions = defaultdict(list)
for sentence in doc.sentences:
    for event_mention in sentence.event_mention_set:
        # There's no anchor node (yet)!
        # event_text_to_event_mentions[event_mention.anchor_node.text].append(event_mention)
        # instead merge based on arithmetic characteristics of ids
        event_id = int(event_mention.id[1:]) % 2 == 0
        event_to_event_mentions[event_id].append(event_mention)

event_ids = list(event_to_event_mentions.keys())
for i in range(len(event_ids)//2):
    event_mentions = event_to_event_mentions[event_ids[i]]
    event_type = event_mentions[0].event_type + '-construct_and_add'
    event = event_set.construct_and_add_event(event_mentions, event_type)

events = []
for i in range(len(event_ids)//2, len(event_ids)):
    event_mentions = event_to_event_mentions[event_ids[i]]
    event_type = event_mentions[0].event_type + '-construct_then_add'
    event = event_set.construct_event(event_mentions, event_type)
    events.append(event)
event_set.add_events(events)

doc.save(output_file)

#
# EXERCISE DOCUMENT API
#
new_doc = Document.construct('doc-123')
assert doc.docid != new_doc.docid and new_doc.docid == 'doc-123'

assert new_doc.sentences is None
new_doc.construct_and_add_sentences()
assert new_doc.sentences is not None and type(new_doc.sentences) is Sentences

assert len(new_doc.sentences) == 0
new_doc.sentences.construct_and_add_sentence()
sentence = new_doc.sentences.construct_sentence()
new_doc.add_sentence(sentence)
new_doc.construct_and_add_sentence()
assert len(new_doc.sentences) == 3

assert all(s.name_theory is None for s in new_doc.sentences)
assert all(s.mention_set is None for s in new_doc.sentences)
for s in new_doc.sentences:
    s.construct_and_add_name_theory()
    assert s.name_theory is not None and type(s.name_theory) is NameTheory
    s.construct_and_add_mention_set()
    assert s.mention_set is not None and type(s.mention_set) is MentionSet
