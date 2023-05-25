import json
import logging
import math
import traceback

from nlplingo.decoding.decoder import SentencePrediction

import serifxml3
from serif.theory.enumerated_type import MentionType
from serif.theory.event_mention import EventMention
from serif.theory.mention import Mention
from serif.util.better_serifxml_helper import find_valid_syn_nodes_by_offsets, find_tokens_by_offsets, \
    find_valid_mentions_for_mention_offset, OffsetSchema
from serif.util.serifxml_utils import exist_in_event_mention_set

logger = logging.getLogger(__name__)

# Roles that only take event mentions
event_fill_slot_roles = {
    'REF_EVENT', 'charged-with', 'corrupt-event', 'judicial-actions', 'outcome-averted', 'outcome-hypothetical',
    'outcome-occurred', 'NPI-Events', 'outbreak-event', 'protest-event', 'terror-event', 'outcome-averted',
    # P2
    'assistance-needed', 'assistance-provided',
    'charged-with', 'declare-emergency', 'disease-outbreak-events',
    'event-or-soa-at-origin', 'human-displacement-events',
    'judicial-actions', 'npi-events', 'outcome',
    'protest-against', 'protest-for', 'related-natural-phenomena', 'repair',
    'rescue-events', 'settlement-status-event-or-soa',

    # p3 event slots
    'NPI-Events', 'outcome', 'related-crimes', 'response', 'victim-impact'
}
# These roles can take either a mention or an event mention, currently unused
ambiguous_fill_slot_roles = {'protest-for', 'protest-against', 'meeting-topic'}

IS_GRANULAR = True


def truncate_float(number: float, digits: int) -> float:
    # https://stackoverflow.com/a/37697840/6254393
    stepper = 10.0 ** digits
    return math.trunc(stepper * number) / stepper


TRUNCATE_FLOAT_DIGHTS = 5


def build_nlplingo_entity_mention_id_to_serif_mention_valuemention_mapping_dict(serif_doc):
    assert isinstance(serif_doc, serifxml3.Document)
    # For why this is implemented in this way, refer to  nlplingo.annotation.serif
    # It turns out that nlplingo would use serif node id as nlplingo.text.text_span.EntityMention.id

    mention_mapping = dict()
    for sentence in serif_doc.sentences:
        assert isinstance(sentence, serifxml3.Sentence)
        if sentence.mention_set is None:
            sentence.add_new_mention_set()
        for m in sentence.mention_set:
            mention_mapping[m.id] = m
    return mention_mapping


# Returns anchor span if no semantic phrase start on event mention
def get_semantic_phrase_start(em, sent):
    if em.semantic_phrase_start is not None:
        return em.semantic_phrase_start
    else:
        return list(sent.token_sequence).index(
            em.anchor_node.start_token)


def get_semantic_phrase_end(em, sent):
    if em.semantic_phrase_end is not None:
        return em.semantic_phrase_end
    else:
        return list(sent.token_sequence).index(
            em.anchor_node.end_token)


def get_serif_sentence_that_covers_offset(start, end, serif_doc):
    """ Given a (start, end) char offset, get the serif_sentence that covers it
    """
    for st_index, sentence in enumerate(serif_doc.sentences):
        if len(sentence.sentence_theories[0].token_sequence) == 0:
            sentence_start = sentence.start_edt
            sentence_end = sentence.end_edt
        else:
            sentence_start = sentence.sentence_theories[0].token_sequence[0].start_edt
            sentence_end = sentence.sentence_theories[0].token_sequence[-1].end_edt

        if sentence_start <= start and end - 1 <= sentence_end:
            return sentence
    return None


def find_matching_event_mention(serif_sent, arg):
    found_em = None
    for serif_em in serif_sent.event_mention_set:
        event_mention_start = serif_sent.token_sequence[serif_em.semantic_phrase_start].start_edt
        event_mention_end = serif_sent.token_sequence[serif_em.semantic_phrase_end].end_edt
        if arg.start == event_mention_start and (arg.end - 1) == event_mention_end:
            found_em = serif_em
            break
    return found_em


def add_event_mention_argument_to_event_mention(serif_sent, em, arg, role, score, statistics,
                                                create_new_event_when_missing):
    found_em = find_matching_event_mention(serif_sent, arg)

    if found_em:
        statistics['num_recovered_event_mention_args_by_event_mention'] += 1
        event_mention_arg = em.add_new_event_mention_argument(role, found_em, score)
        event_mention_arg.model = "NLPLingo"
    else:
        if not create_new_event_when_missing:
            return

        overlap, start_token, end_token = find_tokens_by_offsets(
            serif_sent,
            arg.start,
            arg.end - 1,
            offset_schema=OffsetSchema.EDT
        )

        label = 'UNK'
        statistics['num_created_event_mention'] += 1
        score = truncate_float(score, TRUNCATE_FLOAT_DIGHTS)
        new_em = serif_sent.event_mention_set.add_new_event_mention(label, None, score)
        new_em.semantic_phrase_start = list(serif_sent.token_sequence).index(start_token)
        new_em.semantic_phrase_end = list(serif_sent.token_sequence).index(end_token)
        new_em.model = "NLPLingo"
        event_mention_arg = em.add_new_event_mention_argument(role, new_em, score)
        event_mention_arg.model = "NLPLingo"


def closest_mention(mention, mention_list):
    distance = 99999
    closest_mention = None
    for m in mention_list:
        d = abs(m.start_char - mention.start_char)
        if d < distance:
            distance = d
            closest_mention = m
    return closest_mention


def find_representative_mention(mention):
    if mention.mention_type == MentionType.name:
        return None
    entity = mention.entity()
    if entity is None:
        return None

    # Look for names
    names_before_mention = []
    names_after_mention = []
    for m in entity.mentions:
        if m.mention_type != MentionType.name:
            continue
        if m.start_char <= mention.start_char:
            names_before_mention.append(m)
        else:
            names_after_mention.append(m)
    if len(names_before_mention) > 0:
        return closest_mention(mention, names_before_mention)
    if len(names_after_mention) > 0:
        return closest_mention(mention, names_after_mention)

    # Look for desc if we have a pronoun
    if mention.mention_type != MentionType.pron:
        return None

    descs_before_mention = []
    descs_after_mention = []
    for m in entity.mentions:
        if m.mention_type != MentionType.desc:
            continue
        if m.start_char <= mention.start_char:
            descs_before_mention.append(m)
        else:
            descs_after_mention.append(m)
    if len(descs_before_mention) > 0:
        return closest_mention(mention, descs_before_mention)
    if len(descs_after_mention) > 0:
        return closest_mention(mention, descs_after_mention)

    return None


def adjust_offsets(mention, head_extent_mapping):
    serif_doc = mention.document
    sentence = serif_doc.sentences[mention.sent_no]

    # Match the notion of start and end in head_extent_mapping
    start = None
    end = None
    if mention.head is not None:
        start = mention.head.start_edt
        end = mention.head.end_edt + 1
    else:
        start = mention.end_token.start_edt
        end = mention.end_token.end_edt + 1

    key = (start, end)
    if key in head_extent_mapping:
        adjusted = head_extent_mapping[key]
        if adjusted != key:
            overlap, start_token, end_token = find_tokens_by_offsets(sentence, adjusted[0], adjusted[1] - 1,
                                                                     offset_schema=OffsetSchema.EDT)
            if overlap >= 1.0:
                adjusted_mention = sentence.mention_set.add_new_mention_from_tokens("SPECIAL", "SPECIAL", start_token,
                                                                                    end_token)
                adjusted_mention.mention_type = mention.mention_type
                adjusted_mention.entity_type = mention.entity_type
                adjusted_mention.model = "NLPLingo"
                if mention.confidence is not None:
                    adjusted_mention.confidence = truncate_float(mention.confidence, TRUNCATE_FLOAT_DIGHTS)
                return adjusted_mention

    return mention


def add_mention_argument_to_event_mention(arg_serif_sent, head_extent_mapping, mention_mapping, em, arg, role, score,
                                          statistics):
    # Try various means to find or replace mention
    mention = None

    # For next line, please don't do arg.end-1 here
    overlap_1, valid_mentions = find_valid_mentions_for_mention_offset(arg_serif_sent.mention_set, arg.start, arg.end,
                                                                       1.0, offset_schema=OffsetSchema.EDT)
    # For next line, please don't do arg.end-1 here
    overlap_2, valid_syn_nodes = find_valid_syn_nodes_by_offsets(arg_serif_sent.sentence_theories[0], arg.start,
                                                                 arg.end, 1.0, offset_schema=OffsetSchema.EDT)
    overlap_3, start_token, end_token = find_tokens_by_offsets(arg_serif_sent, arg.start, arg.end - 1,
                                                               offset_schema=OffsetSchema.EDT)

    created = False

    if arg.em_id in mention_mapping:
        # Under the sequence model, arg.em_id is always None
        mention = mention_mapping[arg.em_id]
        statistics['num_recovered_entity_mention_args_by_id'] += 1

    elif overlap_1 >= 1.0 and len(valid_mentions) > 0:
        # Exact match with existing mention
        mention = valid_mentions[0]  # We always have exactly 1 valid mention here
        statistics['num_recovered_entity_mention_args_by_offsets'] += 1

    elif overlap_2 >= 1.0 and len(valid_syn_nodes) > 0:  # overlap == 1.0:
        # Match with SynNode
        valid_syn_node = valid_syn_nodes[0]
        mention = arg_serif_sent.mention_set.add_new_mention(valid_syn_node, "NONE", "NONE")
        mention.model = "NLPLingo"
        mention.confidence = truncate_float(score, TRUNCATE_FLOAT_DIGHTS)
        created = True
        statistics['num_recovered_syn_node_args_by_offsets'] += 1

    elif overlap_3 >= 1.0 and start_token is not None and end_token is not None:  # overlap == 1.0:
        # Match with tokens
        mention = arg_serif_sent.mention_set.add_new_mention_from_tokens("NONE", "NONE", start_token, end_token)
        mention.model = "NLPLingo"
        mention.confidence = truncate_float(score, TRUNCATE_FLOAT_DIGHTS)
        created = True
        if overlap_3 == 1.0:
            statistics['num_recovered_token_args_by_offsets'] += 1
        elif overlap_3 > 0.0:
            statistics['num_recovered_token_args_partial_by_offsets'] += 1
        else:
            statistics['num_recovered_token_args_no_overlap_by_offsets'] += 1

    else:
        # Shouldn't happen
        statistics['num_missed_args'] += 1
        logger.warning(
            "Failed to find potential mention anchor. The offset you requested was {} {} , on doc {} sentence {}, acceptable token offsets are {}".format(
                arg.start, arg.end - 1, arg_serif_sent.document.docid, arg_serif_sent.sent_no,
                ", ".join("({}, {})".format(i.start_char, i.end_char) for i in arg_serif_sent.token_sequence or ())))
        return

    # If we have a desc/pronoun,  we want to look for a name/desc
    # to include in the final output, so find it here and adjust
    # offsets with the head_extent_mapping as well

    try:
        representative_mention = find_representative_mention(mention)
        if representative_mention is not None:
            representative_mention = adjust_offsets(representative_mention, head_extent_mapping)
            serif_doc = mention.document
            special_entity = serif_doc.entity_set.add_new_entity(
                [mention, representative_mention], "SPECIAL", "SPECIAL", False)
    except Exception as e:
        logger.exception(traceback.format_exc())

    overlap4, head_start_token, head_end_token = find_tokens_by_offsets(arg_serif_sent, arg.head_start,
                                                                        arg.head_end - 1,
                                                                        offset_schema=OffsetSchema.EDT)
    if head_start_token is not None and head_end_token is not None:
        mention.head_start_token = head_start_token
        mention.head_end_token = head_end_token

    # em.add_new_mention_argument(role, mention, score)
    return mention


def print_entity_mention_span_change(serif_sent, old_entity_mention_start, old_entity_mention_end,
                                     new_entity_mention_start, new_entity_mention_end):
    marked_tokens = list()
    for idx, token in enumerate(serif_sent.token_sequence):
        c = ""
        if idx == old_entity_mention_start:
            c += "["
        if idx == new_entity_mention_start:
            c += "{"
        c += token.text
        if idx == new_entity_mention_end:
            c += "}"
        if idx == old_entity_mention_end:
            c += "]"
        marked_tokens.append(c)
    return " ".join(marked_tokens)


def merge_event_mention_argument_as_is(src_em, tar_em):
    existing_arguments = set()
    for argument in tar_em.arguments:
        existing_arguments.add((argument.role, argument.value))
    for argument in src_em.arguments:
        if isinstance(argument.value, EventMention):
            if (argument.role, argument.value) not in existing_arguments:
                new_event_arg = tar_em.add_new_event_mention_argument(argument.role, argument.value, argument.score)
                new_event_arg.model = "NLPLingo"
                existing_arguments.add((argument.role, argument.value))


def recalibrate_ref_set(entry, event_id_to_event_mentions, event_mention_to_event_id, new_entry):
    local_event_mention_set = set()
    for src_event_mention in event_id_to_event_mentions[entry["src"]]:
        local_event_mention_set.add(src_event_mention)
    for dst in entry["dsts"]:
        for dst_event_mention in event_id_to_event_mentions[dst]:
            local_event_mention_set.add(dst_event_mention)
    event_mention_to_weighted_score = dict()
    for event_mention in local_event_mention_set:
        score_list = [event_mention.score]
        argument_role_to_highest_score = dict()
        for argument in event_mention.arguments:
            argument_role_to_highest_score[argument.role] = max(argument_role_to_highest_score.get(argument.role, 0.0),
                                                                argument.score)
        score_offset_for_argument = 0.2 * len(argument_role_to_highest_score)
        for role, score in argument_role_to_highest_score.items():
            score_list.append(score)
        weighted_score = sum(score_list) / len(score_list) + score_offset_for_argument
        event_mention_to_weighted_score[event_mention] = weighted_score
    highest_event_mention = max(event_mention_to_weighted_score.items(), key=lambda x: x[1])[0]
    highest_event_id = event_mention_to_event_id[highest_event_mention]
    dst_set = set()
    for event_mention, score in event_mention_to_weighted_score.items():
        event_id = event_mention_to_event_id[event_mention]
        if event_id != highest_event_id:
            dst_set.add(event_id)
    new_entry.append({"src": highest_event_id, "dsts": list(dst_set)})


def recalibrate_event_coref_leader_by_weighted_score(doc_p, event_id_to_event_mentions, event_mention_to_event_id):
    for sent_p in doc_p.sentences.values():
        new_entry = list()
        for entry in sent_p.similar_event_clusters.get("ref_events", ()):
            recalibrate_ref_set(entry, event_id_to_event_mentions, event_mention_to_event_id, new_entry)
        sent_p.similar_event_clusters["ref_events"] = new_entry
        new_event_entry = list()
        for entry in sent_p.similar_event_clusters.get("event_trigger", ()):
            recalibrate_ref_set(entry, event_id_to_event_mentions, event_mention_to_event_id, new_event_entry)
        sent_p.similar_event_clusters["event_trigger"] = new_event_entry


def merge_event_argument_through_overlapping(src_ems, tar_em):
    role_to_entity_mentions = dict()
    role_to_argument_entries = dict()
    role_to_entity_to_scores = dict()
    for argument in tar_em.arguments:
        if isinstance(argument.value, Mention) is True:
            role_to_entity_mentions.setdefault(argument.role, set()).add(argument.value)
            role_to_argument_entries.setdefault(argument.role, list()).append([tar_em, argument])
            role_to_entity_to_scores.setdefault(argument.role, dict())[argument.value] = argument.score
    for event_mention in src_ems:
        for argument in event_mention.arguments:
            if isinstance(argument.value, Mention) is True:
                role_to_entity_mentions.setdefault(argument.role, set()).add(argument.value)
                role_to_argument_entries.setdefault(argument.role, list()).append([event_mention, argument])
                role_to_entity_to_scores.setdefault(argument.role, dict())[argument.value] = argument.score
    role_to_resolved_entity_mentions = dict()
    role_to_resolved_scores = dict()
    for role, entity_mentions in role_to_entity_mentions.items():
        # Method 1: trust nlplingo and only attach entity mention associated with leader event mention but with auto expansion
        for event_mention, argument in role_to_argument_entries[role]:
            if event_mention is tar_em:
                role_to_resolved_entity_mentions.setdefault(role, list()).append(argument.value)
                role_to_resolved_scores.setdefault(role, list()).append(argument.score)
        # End Method 1

    resolved_arguments = list()
    for argument in tar_em.arguments:
        if isinstance(argument.value, Mention) is False:
            resolved_arguments.append(argument)
    tar_em.arguments.clear()
    for role in role_to_resolved_entity_mentions.keys():
        entity_mentions = role_to_resolved_entity_mentions[role]
        scores = role_to_resolved_scores[role]
        for entity_mention, score in zip(entity_mentions, scores):
            new_event_arg = tar_em.add_new_event_mention_argument(role, entity_mention, score)
            new_event_arg.model = "NLPLingo"
    tar_em.arguments.extend(resolved_arguments)


def format_event_mention(event_mention):
    marked_tokens = []
    token_start_to_arg_role = {}
    token_end_to_arg_role = {}
    for event_arg in event_mention.arguments:
        role = event_arg.role
        tokens = event_arg.value.tokens
        token_start_to_arg_role.setdefault(tokens[0].index(), list()).append(role)
        token_end_to_arg_role.setdefault(tokens[-1].index(), list()).append(role)
    trigger_start_idxs = set()
    trigger_start_idxs.add(event_mention.semantic_phrase_start)
    trigger_end_idxs = set()
    trigger_end_idxs.add(event_mention.semantic_phrase_end)
    for anchor in event_mention.anchors:
        trigger_start_idxs.add(int(anchor.semantic_phrase_start))
        trigger_end_idxs.add(int(anchor.semantic_phrase_end))
    for idx, token in enumerate(event_mention.sentence.token_sequence):
        c = ""
        if idx in trigger_start_idxs:
            c += "["
        for _ in token_start_to_arg_role.get(idx, ()):
            c += "{"
        c += token.text
        if idx in token_end_to_arg_role:
            c += "}" * len(token_end_to_arg_role.get(idx, ())) + ",".join(token_end_to_arg_role.get(idx, ()))
        if idx in trigger_end_idxs:
            c += "]" + event_mention.event_type
        marked_tokens.append(c)
    return " ".join(marked_tokens)


def print_cluster(cluster_entry, event_id_to_event_mentions, event_mention_to_event_id):
    for src_event_mention in event_id_to_event_mentions[cluster_entry['src']]:
        logger.debug("[L] {}".format(format_event_mention(src_event_mention)))
    for dst_event_id in cluster_entry['dsts']:
        for dst_event_mention in event_id_to_event_mentions[dst_event_id]:
            logger.debug("[G]\t {}".format(format_event_mention(dst_event_mention)))


def print_event_mention_set(event_mention_set):
    for event_mention in event_mention_set:
        logger.debug("[E] {}".format(format_event_mention(event_mention)))


def nlplingo_event_mention_adder(serif_doc, doc_p, statistics, event_type_statistics, event_arg_type_statistics):
    """
    :type doc_p: nlplingo.decoding.prediction_theory.DocumentPrediction
    """
    # with open('/d4m/ears/expts/48354_p2-granular-decode_devtest/sequences/doc_p.json', 'w') as wfp:
    #     json.dump(doc_p.to_json(), wfp, indent=4, sort_keys=True, ensure_ascii=False)

    serif_doc.add_better_template_metadata()
    serif_doc.better_template_metadata.doc_cls_scores = json.dumps(doc_p.doc_type_scores)
    serif_doc.better_template_metadata.doc_cls_count_scores = json.dumps(doc_p.doc_type_count_scores)
    # logger.info("doc_id: {}, doc_type_scores: {}, doc_type_count_scores: {}".format(
    #     serif_doc.docid, doc_p.doc_type_scores, doc_p.doc_type_count_scores))
    for serif_sent in serif_doc.sentences:
        if serif_sent.event_mention_set is None:
            # print("Adding new event mention set")
            serif_sent.add_new_event_mention_set()

    # For the serif_doc, construct mapping: dict[entity_mention_id] = entity_mention
    mention_mapping = build_nlplingo_entity_mention_id_to_serif_mention_valuemention_mapping_dict(serif_doc)

    # dict[sentence_start_edt, sentence_end_edt] = serif sentence
    sent_edt_off_to_sent = dict()
    for st_index, sentence in enumerate(serif_doc.sentences):
        if len(sentence.sentence_theories[0].token_sequence) == 0:
            sent_edt_off_to_sent[sentence.start_edt, sentence.end_edt] = sentence
        else:
            sent_edt_off_to_sent[
                sentence.sentence_theories[0].token_sequence[0].start_edt, sentence.sentence_theories[0].token_sequence[
                    -1].end_edt] = sentence

    # let's capture the event coref decisions from NLPLingo
    coref_decisions = dict()
    for sent_p in doc_p.sentences.values():
        for event_p in sent_p.events.values():
            for other_event_id, score in event_p.coref_events:
                # if we are dealing with granular events, then (template_anchor) stores the list of other_event_ids that it is coref with
                coref_decisions[
                    other_event_id] = event_p.id  # TODO what if other_event is coref with more than 1 template anchor

    nlplingo_event_to_event_mentions = dict()

    event_id_to_event_mentions = dict()
    event_mention_to_event_id = dict()
    # First pass over sentences, create EventMentions
    for sent_p in doc_p.sentences.values():
        #### This is for debugging; collect events in sent_p
        nlplingo_event_infos = set()
        for offset, event_p in sent_p.events.items():
            out_infos = []
            out_infos.append(doc_p.docid)
            if len(event_p.trigger.labels) == 1:
                statistics['len(event_p.trigger.labels)==1'] += 1
            elif len(event_p.trigger.labels) > 1:
                statistics['len(event_p.trigger.labels)>1'] += 1
            out_infos.append(
                'AA(%d,%d) %s' % (event_p.trigger.start, event_p.trigger.end - 1, list(event_p.trigger.labels)[0]))
            for arg_offset, arg_p in event_p.arguments.items():
                if len(arg_p.labels) == 1:
                    statistics['len(arg_p.labels)==1'] += 1
                elif len(arg_p.labels) > 1:
                    statistics['len(arg_p.labels)>1'] += 1
                out_infos.append('(%d,%d) %s' % (arg_p.start, arg_p.end - 1, list(arg_p.labels)[0]))
            nlplingo_event_infos.add('\t'.join(sorted(out_infos)))

        assert isinstance(sent_p, SentencePrediction)

        sent_start_edt = sent_p.start
        sent_end_edt = sent_p.end - 1
        assert (sent_start_edt, sent_end_edt) in sent_edt_off_to_sent
        serif_sent = sent_edt_off_to_sent[sent_start_edt, sent_end_edt]

        if serif_sent.event_mention_set is None:
            serif_sent.add_new_event_mention_set()

        ref_event_arguments = []
        for anchor_offset, event in sent_p.events.items():  # NOTE: DO NOT use 'anchor_offset' for anything; it might just be a running integer index
            statistics['num_triggers'] += 1

            # We only have one event trigger label
            overlap, anchor_start_token, anchor_end_token = find_tokens_by_offsets(serif_sent, event.trigger.start,
                                                                                   event.trigger.end - 1,
                                                                                   offset_schema=OffsetSchema.EDT)
            if overlap == 1.0:
                statistics['num_triggers_token_full_overlap'] += 1
            elif overlap > 0.0:
                statistics['num_triggers_token_partial_overlap'] += 1
            else:
                statistics['num_triggers_token_no_overlap'] += 1

            anchor_start_token_index = list(serif_sent.token_sequence).index(anchor_start_token)
            anchor_end_token_index = list(serif_sent.token_sequence).index(anchor_end_token)

            for label, score in event.trigger.labels.items():
                event_type_statistics[label] += 1
                em = exist_in_event_mention_set(serif_sent.event_mention_set, label, None, anchor_start_token_index,
                                                anchor_end_token_index, event.trigger.start, event.trigger.end - 1,
                                                trigger_text=event.trigger.text)

                if em is None:
                    em = serif_sent.event_mention_set.add_new_event_mention(label, None, truncate_float(score,
                                                                                                        TRUNCATE_FLOAT_DIGHTS))

                    em.semantic_phrase_start = anchor_start_token_index
                    em.semantic_phrase_end = anchor_end_token_index

                    em.add_new_event_mention_type(label, truncate_float(score, TRUNCATE_FLOAT_DIGHTS))
                    em.model = "NLPLingo"
                else:
                    statistics['exist_in_event_mention_set returns an existing em'] += 1
                event_id_to_event_mentions.setdefault(event.id, list()).append(em)
                event_mention_to_event_id[em] = event.id
                for k, v in event.attributes.items():
                    if k == 'type_slot':
                        em.granular_template_type_attribute = v
                    elif k == 'completion_slot':
                        em.completion = v
                    elif k == 'overtime_slot':
                        em.over_time = True
                    elif k == 'coordinated_slot':
                        em.coordinated = True
                    elif k == "project_type":
                        em.project_type = v

                #     TODO

                nlplingo_event_to_event_mentions.setdefault(event, set()).add(em)

                # do I have a coref decision?
                if event.id in coref_decisions:
                    coref_cluster_id = coref_decisions[event.id]
                else:
                    coref_cluster_id = event.id  # just use my own id
                if coref_cluster_id is not None:
                    em.cluster_id = coref_cluster_id

    # Second pass over sentences, create arguments
    for sent_p in doc_p.sentences.values():
        for anchor_offset, event in sent_p.events.items():
            if event not in nlplingo_event_to_event_mentions:
                continue
            for em in nlplingo_event_to_event_mentions.get(event, ()):
                for mention_offset, arg in event.arguments.items():
                    statistics['num_arguments'] += 1

                    for role, score in arg.labels.items():
                        event_arg_type_statistics[role] += 1

                        arg_serif_sent = get_serif_sentence_that_covers_offset(arg.start, arg.end, serif_doc)

                        if arg_serif_sent is None:
                            logger.warning(
                                'In doc {}, cannot find Serif sentence that covers offsets {},{}, which should not happen'.format(
                                    serif_doc.docid, arg.start, arg.end))
                            continue

                        if role in event_fill_slot_roles:
                            add_event_mention_argument_to_event_mention(arg_serif_sent, em, arg, role, score,
                                                                        statistics,
                                                                        not IS_GRANULAR)
                        elif role in ambiguous_fill_slot_roles and find_matching_event_mention(arg_serif_sent,
                                                                                               arg) is not None:
                            add_event_mention_argument_to_event_mention(arg_serif_sent, em, arg, role, score,
                                                                        statistics,
                                                                        not IS_GRANULAR)
                        else:
                            score = truncate_float(score, TRUNCATE_FLOAT_DIGHTS)
                            new_mention = add_mention_argument_to_event_mention(arg_serif_sent,
                                                                                doc_p.head_extent_mapping,
                                                                                mention_mapping, em, arg, role, score,
                                                                                statistics)
                            if new_mention is None:
                                logger.warning(
                                    "In doc {}, due to we cannot add mention, we have to drop event argument {} {}".format(
                                        serif_doc.docid, arg.start, arg.end))
                                continue
                            event_mention_argument = em.add_new_mention_argument(role, new_mention, score)
                            event_mention_argument.model = "NLPLingo"
                            if arg.ner_score.get(role, None) is not None:
                                event_mention_argument.ner_score = truncate_float(arg.ner_score.get(role, None),
                                                                                  TRUNCATE_FLOAT_DIGHTS)
                            if arg.coarse_filler_score.get(role, None) is not None:
                                event_mention_argument.coarse_score = truncate_float(
                                    arg.coarse_filler_score.get(role, None), TRUNCATE_FLOAT_DIGHTS)
                            if arg.coarse_to_fine_filler_score.get(role, None) is not None:
                                event_mention_argument.coarse_to_fine_score = truncate_float(
                                    arg.coarse_to_fine_filler_score.get(role, None), TRUNCATE_FLOAT_DIGHTS)

        # # This is for debugging ; collect event info from SerifXMLs
        # serifxml_event_infos = set()
        # for em in serif_sent.event_mention_set or []:
        #     out_infos = []
        #     out_infos.append(doc_p.docid)

        #     sps = get_semantic_phrase_start(em, serif_sent)
        #     spe = get_semantic_phrase_end(em, serif_sent)
        #     start = list(serif_sent.token_sequence)[sps].start_edt
        #     end = list(serif_sent.token_sequence)[spe].end_edt

        #     out_infos.append('AA(%d,%d) %s' % (start, end, em.event_type))
        #     for arg in em.arguments:
        #         if arg.mention:
        #             if arg.mention.start_token and arg.mention.end_token:
        #                 out_infos.append('(%d,%d) %s' % (arg.mention.start_token.start_edt, arg.mention.end_token.end_edt, arg.role))
        #             elif arg.mention.syn_node:
        #                 out_infos.append('(%d,%d) %s' % (arg.mention.syn_node.start_token.start_edt, arg.mention.syn_node.end_token.end_edt, arg.role))
        #         elif arg.event_mention:
        #             ema = arg.event_mention
        #             s = ema.document.sentences[ema.anchor_node.sent_no]
        #             asps = get_semantic_phrase_start(ema, s)
        #             aspe = get_semantic_phrase_end(ema, s)
        #             start = list(serif_sent.token_sequence)[asps].start_edt
        #             end = list(serif_sent.token_sequence)[aspe].end_edt
        #             out_infos.append('(%d,%d) %s' % (start, end, arg.role))
        #     serifxml_event_infos.add('\t'.join(sorted(out_infos)))

        # #### This is for debugging; now let's compare the event infos for this sentence (from nlplingo vs from SerifXMLs) ; they should be exactly the same
        # for event_info in nlplingo_event_infos:
        #     if event_info not in serifxml_event_infos:
        #         print('ONLY IN nlplingo', event_info)
        # for event_info in serifxml_event_infos:
        #     if event_info not in nlplingo_event_infos:
        #         print('ONLY IN serifxml', event_info)

    """
    # do a check to ensure all event arguments extent conform to those specified in doc_p.head_extent_mapping
    # This is used during CLIE_MT 2nd run of NLPLingo:
    # * for CLIE_MT, when we do the first call of NLPLingo, we don't perform head to extent expansion. This is because we project based on just trigger and argument heads.
    # * for CLIE_MT, then we do a second call of NLPLingo, to extent the project heads to their full extents (using the following code).
    for serif_sent in serif_doc.sentences:
        for em in serif_sent.event_mention_set:
            # print('type(em.arguments)=', type(em.arguments))
            for arg_index, arg in enumerate(em.arguments):
                # print('type(arg)=', type(arg))
                # print('arg.value.start_edt=', arg.value.start_edt)
                # print('arg.value.end_edt=', arg.value.end_edt)

                # TODO if this is an EventMention, then it seems we will need to replace the anchor(s) of the EventMention with the correct extents
                if isinstance(arg.value, EventMention):
                    continue

                if (arg.value.start_edt, arg.value.end_edt + 1) in doc_p.head_extent_mapping:
                    extent_start, extent_end = doc_p.head_extent_mapping.get(
                        (arg.value.start_edt, arg.value.end_edt + 1))
                    if arg.value.start_edt == extent_start and (arg.value.end_edt + 1) == extent_end:
                        continue

                    # else, we need to expand the extent of this argument
                    arg_p = ArgumentPrediction(extent_start, extent_end)

                    if (arg.value.start_edt, arg.value.end_edt + 1) in doc_p.head_headextent_mapping:
                        head_extent_start, head_extent_end = doc_p.head_headextent_mapping.get(
                            (arg.value.start_edt, arg.value.end_edt + 1))
                        arg_p.head_start = head_extent_start
                        arg_p.head_end = head_extent_end

                    new_mention = add_mention_argument_to_event_mention(serif_sent, doc_p.head_extent_mapping,
                                                                        mention_mapping, em, arg_p, arg.role, arg.score,
                                                                        statistics)
                    print('type(new_mention)=', type(new_mention))
                    new_em_arg = em.construct_event_mention_argument(arg.role, new_mention, arg.score)
                    em.arguments[arg_index] = new_em_arg
                    # print('*** Adjusted head (%d, %d) to extent (%d, %d)' % (arg.value.start_edt, arg.value.end_edt+1, extent_start, extent_end))
    """

    # add any predicted entity mentions
    # @hqiu: I didn't get why we need below code. Comment it for now, as 11/21/22
    # for serif_sent in serif_doc.sentences:
    #     if serif_sent.mention_set is None:
    #         serif_sent.add_new_mention_set()
    #
    # for sent_p in doc_p.sentences.values():
    #     for entity_mention in sent_p.entities.values():
    #         start_char = entity_mention.start
    #         end_char = entity_mention.end - 1  # -1 to be consistent with Serif
    #
    #         max_label = None
    #         max_score = 0
    #         for label, score in entity_mention.entity_type_labels.items():
    #             if score > max_score:
    #                 max_label = label
    #                 max_score = score
    #
    #         serif_sent = get_serif_sentence_that_covers_offset(start_char, end_char + 1, serif_doc)
    #         """:type: serifxml3.theory.sentence.Sentence"""
    #         if serif_sent is None:
    #             logger.warning(
    #                 'cannot find Serif sentence that covers char offsets {}-{}'.format(str(start_char), str(end_char)))
    #             continue
    #
    #         start_token = None
    #         end_token = None
    #         for token in serif_sent.sentence_theories[0].token_sequence:
    #             if start_token is None and token.start_char == start_char:
    #                 start_token = token
    #             if token.end_char == end_char:
    #                 end_token = token
    #
    #         if start_token is None:
    #             logger.warning('cannot find Serif start_token for char {}'.format(start_char))
    #         if end_token is None:
    #             logger.warning('cannot find Serif end_token for char {}'.format(end_char))
    #
    #         if start_token is not None and end_token is not None:
    #             new_mention = serif_sent.mention_set.add_new_mention_from_tokens('NONE', max_label, start_token, end_token)
    #             new_mention.model = "NLPLingo"
    #             new_mention.confidence = max_score
    # @hqiu: I didn't get why we need below code. Comment it for now, as 11/21/22

    # Third pass: Remove similar events
    # Recalibrate leader of clusters
    recalibrate_event_coref_leader_by_weighted_score(doc_p, event_id_to_event_mentions, event_mention_to_event_id)
    # Pass 1, resolve REF_EVENT layer
    old_em_to_new_em_ref_event_pass = dict()
    for sent_p in doc_p.sentences.values():
        if "ref_events" in sent_p.similar_event_clusters:
            for entry in sent_p.similar_event_clusters.get("ref_events", ()):
                src_ems = event_id_to_event_mentions[entry["src"]]
                for dst in entry["dsts"]:
                    dst_ems = event_id_to_event_mentions[dst]
                    for dst_em in dst_ems:
                        for src_em in src_ems:
                            if src_em.event_type != dst_em.event_type:
                                continue
                            old_em_to_new_em_ref_event_pass[dst_em] = src_em
    for serif_sent in serif_doc.sentences:
        for event_mention in serif_sent.event_mention_set or ():
            resolved_arguments = list()
            added_role_argument_pair = set()
            for argument in event_mention.arguments:
                if isinstance(argument.value, EventMention):
                    if argument.event_mention in old_em_to_new_em_ref_event_pass:
                        argument.event_mention = old_em_to_new_em_ref_event_pass[argument.event_mention]
                        if argument.event_mention is not event_mention and (
                                argument.role, argument.event_mention) not in added_role_argument_pair:
                            resolved_arguments.append(argument)
                            added_role_argument_pair.add((argument.role, argument.event_mention))
                    else:
                        resolved_arguments.append(argument)
                else:
                    resolved_arguments.append(argument)
            event_mention.arguments.clear()
            event_mention.arguments.extend(resolved_arguments)

    # Pass 2, resolve event trigger layer
    # Debug code start
    # logger.debug("Current clusters before merging")
    # for sent_p in doc_p.sentences.values():
    #     serif_sent = get_serif_sentence_that_covers_offset(sent_p.start, sent_p.end, serif_doc)
    #     logger.debug("Original event mention set")
    #     print_event_mention_set(serif_sent.event_mention_set)
    #     logger.debug("[SEPARATOR]")
    #     for entry in sent_p.similar_event_clusters.get("event_trigger", ()):
    #         logger.debug("Pending clustering")
    #         print_cluster(entry, event_id_to_event_mentions, event_mention_to_event_id)
    #     logger.debug("[SEPARATOR]")
    # logger.debug("[SEPARATOR]")
    # logger.debug("[SEPARATOR]")
    # logger.debug("[SEPARATOR]")
    # Actual code start
    old_em_to_new_em_event_trigger_pass = dict()
    for sent_p in doc_p.sentences.values():
        if "event_trigger" in sent_p.similar_event_clusters:
            for entry in sent_p.similar_event_clusters.get("event_trigger", ()):
                src_ems = event_id_to_event_mentions[entry["src"]]
                for dst in entry["dsts"]:
                    dst_ems = event_id_to_event_mentions[dst]
                    for dst_em in dst_ems:
                        for src_em in src_ems:
                            if src_em.event_type != dst_em.event_type:
                                continue
                            # merge_argument_as_is(dst_em, src_em)
                            merge_event_mention_argument_as_is(dst_em, src_em)
                            old_em_to_new_em_event_trigger_pass[dst_em] = src_em
    leader_em_to_children_ems = dict()
    for child_em, leader_em in old_em_to_new_em_event_trigger_pass.items():
        leader_em_to_children_ems.setdefault(leader_em, set()).add(child_em)
    for leader_em, children_ems in leader_em_to_children_ems.items():
        merge_event_argument_through_overlapping(children_ems, leader_em)
    # logger.debug("After merging")
    preserved_ems = set()
    sentence_to_preserved_ems = dict()
    original_em_cnt = 0
    for serif_sent in serif_doc.sentences:
        for event_mention in serif_sent.event_mention_set or ():
            original_em_cnt += 1
            if event_mention not in old_em_to_new_em_event_trigger_pass:
                preserved_ems.add(event_mention)
                sentence_to_preserved_ems.setdefault(serif_sent, set()).add(event_mention)
    for event_mention in preserved_ems:
        resolved_arguments = list()
        added_role_event_mention_argument_set = set()
        for argument in event_mention.arguments:
            if isinstance(argument.value, EventMention):
                if argument.event_mention in preserved_ems:
                    if argument.event_mention is not event_mention:  # No self-pointing event argument
                        if (argument.event_mention, argument.role) not in added_role_event_mention_argument_set:
                            resolved_arguments.append(argument)
                            added_role_event_mention_argument_set.add((argument.event_mention, argument.role))
                else:
                    logger.warning("Dropping argument {} {} {} due to disconnected ems".format(
                        serif_doc.docid,
                        event_mention.sentence.sent_no,
                        argument.role)
                    )
            else:
                resolved_arguments.append(argument)
        event_mention.arguments.clear()
        event_mention.arguments.extend(resolved_arguments)
    new_em_cnt = 0
    for serif_sent in serif_doc.sentences:
        if serif_sent in sentence_to_preserved_ems:
            serif_sent.event_mention_set._children.clear()
            serif_sent.event_mention_set._children.extend(sentence_to_preserved_ems[serif_sent])
            new_em_cnt += len(sentence_to_preserved_ems[serif_sent])
    logger.info("{} after coref deduplication model, we decrease num of ems from {} to {}".format(serif_doc.docid,
                                                                                                  original_em_cnt,
                                                                                                  new_em_cnt))
    # print_event_mention_set(serif_sent.event_mention_set)
    # logger.debug("[SEPARATOR]")
