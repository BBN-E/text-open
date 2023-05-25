"""
Copyright 2020 Raytheon BBN Technologies
All Rights Reserved
"""

from collections import defaultdict

import argparse
import logging
import os
from itertools import chain
from nlplingo.common.utils import IntPair
from nlplingo.text.text_span import LabeledTextSpan, LabeledTextFrame

import serifxml3
from serif.io.bpjson.reader import Corpus
from serif.util.better_serifxml_helper \
    import find_valid_syn_nodes_by_offsets, \
    find_valid_anchors, \
    find_valid_anchors_by_token_index, \
    find_valid_mentions_for_mention_text, \
    find_tokens_by_text, \
    find_tokens_by_offsets, \
    iou_1d

logger = logging.getLogger(__name__)

USE_SPAN_HSTRING = False


def get_frame_annotations_from_bp(bp_corpus):
    """Extract trigger and argument annotations as LabeledTextFrame
    In preparing the extractions from the BP JSON file, each sentence will be treated as its own doc.
    The sentence's entry-id will be treated as its docid. This is to be consistent with the SerifXML files provided,
    where each SerifXML file only contains 1 single sentence from the BP JSON file.

    :rtype: dict[str, list[list[nlplingo.text.text_span.LabeledTextFrame]]]
    """
    ret = dict()

    for _, doc in bp_corpus.docs.items():
        for sentence in doc.sentences:
            sentence_spans = []

            for _, abstract_event in sentence.abstract_events.items():  # for each abstract event in sentence
                anchor_spans = []
                for anchor_span in abstract_event.anchors.spans:  # for each anchor span in event
                    text = anchor_span.string
                    label = '{}.{}'.format(abstract_event.helpful_harmful, abstract_event.material_verbal)
                    anchor_spans.append(LabeledTextSpan(IntPair(None, None), text, label))

                argument_spans = []
                for arg in abstract_event.agents:
                    for span in arg.spans:
                        argument_spans.append(LabeledTextSpan(IntPair(None, None), span.string, 'AGENT'))
                for arg in abstract_event.patients:
                    for span in arg.spans:
                        argument_spans.append(LabeledTextSpan(IntPair(None, None), span.string, 'PATIENT'))
                sentence_spans.append(LabeledTextFrame(anchor_spans, argument_spans))

            ret[sentence.entry_id] = [sentence_spans]
    return ret


def serif_event_tostring(event_mention, serif_sent):
    ret = []

    if event_mention.semantic_phrase_start is not None and event_mention.semantic_phrase_end is not None:
        start_index = int(event_mention.semantic_phrase_start)
        end_index = int(event_mention.semantic_phrase_end)
        start = serif_sent.token_sequence[start_index:end_index + 1][0].start_edt
        end = serif_sent.token_sequence[start_index:end_index + 1][-1].end_edt
        if start is None and end is None:  # PySerif doesn't populate edt
            start = serif_sent.token_sequence[start_index:end_index + 1][0].start_char
            end = serif_sent.token_sequence[start_index:end_index + 1][-1].end_char

        text = serif_sent.get_original_text_substring(start, end)
        ret.append('anchors: {}[{}]'.format(event_mention.event_type, text))
    else:
        ret.append('anchors: {}[{}]'.format(event_mention.event_type, event_mention.anchor_node.text))

    args_by_roles = defaultdict(list)
    for argument in event_mention.arguments:
        args_by_roles[argument.role].append(argument)
    for role in args_by_roles:
        arg_text = ' ||| '.join(sorted(set('[{}]'.format(arg.value.text) for arg in args_by_roles[
            role])))  # remove duplicates to make automatic comparison easier
        ret.append('{}: {}'.format(role, arg_text))
    return '\n'.join(ret)


def labeled_text_frame_tostring(annotation):
    """
    :type annotation: nlplingo.text.text_span.LabeledTextFrame
    """
    ret = []

    for span in annotation.anchor_spans:
        lines = []
        anchor_texts = []
        anchor_texts.append('{}[{}]'.format(span.label, span.text))
        lines.append('anchors: {}'.format(' ||| '.join(anchor_texts)))
        args_by_roles = defaultdict(list)
        for argument in annotation.argument_spans:
            args_by_roles[argument.label].append(argument)
        for role in args_by_roles:
            arg_text = ' ||| '.join(sorted(set('[{}]'.format(arg.text) for arg in args_by_roles[
                role])))  # remove duplicates to make automatic comparison easier
            lines.append('{}: {}'.format(role, arg_text))
        ret.append('\n'.join(lines))
    return ret


def compare_annotations(doc_annotation, doc, diff_docids):
    assert len(doc.sentences) == len(doc_annotation)

    for i, sentence in enumerate(doc.sentences):
        # print(doc.docid, sentence.text)

        # annotations from Serif
        serif_strings = []
        for event_mention in sentence.event_mention_set:
            # print('## SERIF event')
            serif_string = serif_event_tostring(event_mention, sentence)
            # print(serif_string)
            serif_strings.append(serif_string)

        # annotations from BP
        bp_strings = []
        sentence_annotation = doc_annotation[i]
        for annotation in sentence_annotation:
            # print('## BP event')
            bp_string = labeled_text_frame_tostring(annotation)
            # print('\n'.join(bp_string))
            bp_strings.extend(bp_string)
        # print('')

        if '\n'.join(sorted(serif_strings)) != '\n'.join(sorted(bp_strings)):
            strings1 = sorted(set(serif_strings).difference(set(bp_strings)))
            strings2 = sorted(set(bp_strings).difference(set(serif_strings)))
            diff_docids[doc.docid] = sentence.text + '\n#### SERIF\n' + '\n========\n'.join(
                strings1) + '\n#### BP\n' + '\n========\n'.join(strings2)


def get_sentence_by_offsets(doc, start, end):
    for s in doc.sentences:
        if (s.token_sequence[0].start_char <= start and
                s.token_sequence[-1].end_char >= (end - 1)):
            return s

    # Start and end is not contained within a single sentence
    # Return earliest sentence that contains any of the span
    for s in doc.sentences:
        if (s.token_sequence[-1].end_char > start):
            return s
    logger.error("Cannot find sentence {} {} under doc {}".format(start, end, doc.docid))
    return None


def get_mention_type(synclass):
    if synclass == 'name':
        return "name"
    elif synclass == 'pronoun':
        return "pron"
    else:
        return "desc"


def find_or_create_mention_from_span(sentence, span):
    matching_mention = None

    if USE_SPAN_HSTRING:
        span_start = span.hstart
        span_end = span.hend
    else:
        span_start = span.start
        span_end = span.end

    # First look for existing Mention
    ms = sentence.mention_set
    for mention in ms:
        overlap = iou_1d(
            span_start,
            span_end - 1,
            mention.start_char,
            mention.end_char
        )
        if overlap == 1.0:
            matching_mention = mention
            matching_mention.mention_type = get_mention_type(span.synclass)
            break

    # Look for exactly overlapping SynNode and make Mention out of it
    if matching_mention is None:
        overlap, overlapping_syn_nodes = find_valid_syn_nodes_by_offsets(
            sentence, span_start, span_end, 0.99)
        if len(overlapping_syn_nodes) > 0:
            matching_mention = ms.add_new_mention(
                overlapping_syn_nodes[0],
                get_mention_type(span.synclass),
                "UNDET")

    # Make Mention from tokens    
    if matching_mention is None:
        overlap, start_token, end_token = find_tokens_by_offsets(
            sentence,
            span_start,
            span_end - 1)
        if start_token is not None and end_token is not None:
            matching_mention = ms.add_new_mention_from_tokens(
                get_mention_type(span.synclass),
                "UNDET", start_token, end_token)

    return matching_mention


def basic_event_spans_to_serifxml(doc, bp_segment):
    es = doc.add_new_entity_set()
    span_to_mention = dict()
    span_set_id_to_entity = dict()

    for span_set_id, span_set in bp_segment.span_sets.items():
        span_set_mentions = []
        for span in span_set.spans:
            if span.synclass == 'event-anchor':
                pass
            else:
                sentence = get_sentence_by_offsets(doc, span.start, span.end)
                if sentence is not None:
                    mention = find_or_create_mention_from_span(sentence, span)
                    if mention is not None:

                        # capture 'hstring' from BP-JSON
                        if (span.hstart != -1 and span.hstart is not None) and (
                                span.hend != -1 and span.hend is not None):
                            _, hstart_token, hend_token = find_tokens_by_offsets(sentence, span.hstart, span.hend - 1)
                            if hstart_token is not None and hend_token is not None:
                                if hstart_token.start_char < mention.start_token.start_char:
                                    hstart_token = mention.start_token
                                if hend_token.end_char > mention.end_token.end_char:
                                    hend_token = mention.end_token
                                mention.head_start_token = hstart_token
                                mention.head_end_token = hend_token

                        span_set_mentions.append(mention)
                        span_to_mention[span] = mention
                    else:
                        logger.error(
                            "Failed to create span {} {} {} {}".format(doc.docid, span.start, span.end, span.string))
                else:
                    logger.error(
                        "Failed to create span {} {} {} {}".format(doc.docid, span.start, span.end, span.string))

        # create entity out of spanset's mentions
        if len(span_set_mentions) > 0:
            entity = find_partial_coreference(
                bp_segment, span_set, span_set_id_to_entity)
            if entity is None:
                entity = es.add_new_entity(span_set_mentions, "NONE", "UNDET", False)
                span_set_id_to_entity[span_set.ss_id] = entity
            else:
                entity.mentions.extend(span_set_mentions)
                span_set_id_to_entity[span_set.ss_id] = entity

    return span_to_mention, span_set_id_to_entity


def find_partial_coreference(
        bp_segment, span_set, span_set_id_to_entity):
    # Check to see if span_set contains any previously created entities
    if span_set.ss_id in bp_segment.relations:
        for contained_span_set in bp_segment.relations[span_set.ss_id]:
            if contained_span_set.ss_id in span_set_id_to_entity:
                return span_set_id_to_entity[contained_span_set.ss_id]

    # Check to see if any previously created entity contains span_set
    for previously_processed_span_set_id, entity in span_set_id_to_entity.items():
        if previously_processed_span_set_id in bp_segment.relations:
            for contained_span_set in bp_segment.relations[previously_processed_span_set_id]:
                if contained_span_set == span_set:
                    return entity

    return None


def get_best_syn_node_for_span(doc, anchor_span):
    sentence = get_sentence_by_offsets(doc, anchor_span.start, anchor_span.end)
    if sentence is None:
        return None
    overlap, matching_syn_nodes = find_valid_syn_nodes_by_offsets(
        sentence, anchor_span.start, anchor_span.end, 0.99)

    if len(matching_syn_nodes) > 0:
        if matching_syn_nodes[0].is_terminal:
            return matching_syn_nodes[0].parent
        return matching_syn_nodes[0]

    overlap, start_token, end_token = find_tokens_by_offsets(
        sentence,
        anchor_span.start,
        anchor_span.end
    )
    if start_token is not None and end_token is not None:
        start_token_index = sentence.token_sequence.index(start_token)
        overlap, matching_syn_nodes = find_valid_anchors_by_token_index(
            sentence, start_token_index, start_token_index, 0.99)
        return matching_syn_nodes[0]
    else:
        return None


def abstract_events_bp_json_to_serifxml(
        doc,
        bp_sentence,
        statistics
):
    """
    :param doc:
    :param bp_sentence:
    :param statistics:
    :return:
    """

    def abstract_event_arg_ref_events_bpjson_to_serifxml(
            event_mention,
            role_,
            ref_event_mention,
            doc_,
            statistics_
    ):
        """

        :param event_mention:
        :type event_mention: serif.theory.event_mention.EventMention
        :param role_:
        :type role_: str
        :param ref_event_mentions:
        :type ref_event_mention: serif.theory.event_mention.EventMention
        :param doc_:
        :type doc_: serif.theory.document.Document
        :param statistics_:
        :type statistics_: dict[str,int]
        :return:
        :rtype: (dict[str,int], serif.theory.document.Document)
        """

        statistics_['abstract_event:ref_event:event_mention_exact_match'] += 1
        event_mention.add_new_event_mention_argument(
            role_,
            ref_event_mention,
            1.0
        )
        return statistics_, doc_

    logger.info("Handling Abstract {}".format(doc.docid))

    # print('Finished gathering')
    sentence = doc.sentences[0]
    event_mention_set = sentence.add_new_event_mention_set()
    event_id_to_event_mention = dict()
    for _, abstract_event in bp_sentence.abstract_events.items():
        event_type = '{}.{}'.format(
            abstract_event.helpful_harmful,
            abstract_event.material_verbal
        )
        roles = list(
            chain(
                ['AGENT'] * len(abstract_event.agents),
                ['PATIENT'] * len(abstract_event.patients)
            )
        )
        argument_span_sets = list(
            chain(
                abstract_event.agents,
                abstract_event.patients
            )
        )

        semantic_phrase_token_pair = None
        valid_anchors = []  # List of SynNode, semantic phrase start token, semantic phrase end token

        for anchor_span in abstract_event.anchors.spans:

            if len(anchor_span.string) == 0:
                continue

            # Case 1, we have offsets
            if anchor_span.start != -1:
                anchor_node = get_best_syn_node_for_span(doc, anchor_span)
                if anchor_node is not None:
                    overlap, start_token, end_token = find_tokens_by_offsets(
                        sentence,
                        anchor_span.start,
                        anchor_span.end - 1)
                    if start_token is not None and end_token is not None:
                        valid_anchors.append((anchor_node, start_token, end_token))
                        continue

            # Case 2, no start and end offsets, use text
            valid_anchor_nodes = find_valid_anchors(
                sentence.sentence_theories[0],
                anchor_span.string,
                0.99
            )
            _, anchor_token_pairs = find_tokens_by_text(
                sentence,
                anchor_span.string,
            )
            if len(anchor_token_pairs) > 0:
                start_token = anchor_token_pairs[0][0]
                end_token = anchor_token_pairs[0][1]

                if len(valid_anchor_nodes) == 0:
                    # Couldn't find SynNode that covers the whole span,
                    # use the SynNode over the first token for the anchor node
                    anchor_node = start_token.syn_node
                    if anchor_node.is_terminal:
                        anchor_node = anchor_node.parent
                    valid_anchors.append((anchor_node, start_token, end_token))
                else:
                    valid_anchors.append((valid_anchor_nodes[0], start_token, end_token))

        if len(valid_anchors) == 0:
            continue

        # Use first valid anchor as the anchor on the EventMention object
        em = event_mention_set.add_new_event_mention(event_type, valid_anchors[0][0], 1.0)
        em.add_new_event_mention_type(event_type, 1.0)
        em.semantic_phrase_start = sentence.token_sequence.index(valid_anchors[0][1])
        em.semantic_phrase_end = sentence.token_sequence.index(valid_anchors[0][2])
        event_id_to_event_mention[abstract_event.event_id] = em

        # Store all valid anchors as EventMentionAnchor object 
        for anchor_node, start_token, end_token in valid_anchors:
            em_anchor = em.add_new_event_mention_anchor(anchor_node)
            em_anchor.semantic_phrase_start = \
                sentence.token_sequence.index(start_token)
            em_anchor.semantic_phrase_end = \
                sentence.token_sequence.index(end_token)

            # Deal with arguments
        for role, argument_span_set in zip(roles, argument_span_sets):
            statistics['num_arguments'] += 1
            mention_token_pairs = []
            for span in argument_span_set.spans:

                if USE_SPAN_HSTRING is True and span.hstring is not None:
                    annotation_text = span.hstring
                else:
                    annotation_text = span.string

                if len(annotation_text) == 0:  # Sometimes bpjson files have blank data so skip
                    continue

                # If we have offsets (as in Arabic BP JSON), use those
                if USE_SPAN_HSTRING:
                    span_start = span.hstart
                else:
                    span_start = span.start

                if span_start != -1:
                    mention = find_or_create_mention_from_span(sentence, span)
                    if mention is not None:
                        em.add_new_mention_argument(role, mention, 1.0)
                        continue

                # Check for existing mention
                overlap_proportion, matched_mentions = find_valid_mentions_for_mention_text(
                    doc.sentences[0].mention_set, annotation_text, 0.99)
                if len(matched_mentions) > 0:
                    for m in matched_mentions:
                        em.add_new_mention_argument(role, m, 1.0)
                    continue

                # Check for syn node match
                matched_syn_nodes = find_valid_anchors(
                    sentence, annotation_text, 0.99)
                if len(matched_syn_nodes) > 0:
                    for s in matched_syn_nodes:
                        mention = sentence.mention_set.add_new_mention(s, "desc", "UNDET")
                        em.add_new_mention_argument(role, mention, 1.0)
                    continue

                # Create mention with token offsets
                overlap_proportion, mention_token_pairs = find_tokens_by_text(
                    sentence, annotation_text)
                for mention_token_pair in mention_token_pairs:
                    mention = sentence.mention_set.add_new_mention_from_tokens(
                        "desc", "UNDET", mention_token_pair[0], mention_token_pair[1])
                    em.add_new_mention_argument(role, mention, 1.0)
                continue

    for _, abstract_event in bp_sentence.abstract_events.items():
        for ref_event_id in abstract_event.ref_events:
            statistics['abstract_event:ref_event:num_events'] += 1

            if abstract_event.event_id not in event_id_to_event_mention or ref_event_id not in event_id_to_event_mention:
                continue

            statistics, doc = abstract_event_arg_ref_events_bpjson_to_serifxml(
                event_id_to_event_mention[abstract_event.event_id],
                'REF_EVENT',
                event_id_to_event_mention[ref_event_id],
                doc,
                statistics
            )

    return


def basic_events_bpjson_to_serifxml(
        bp_segment,
        doc,
        statistics
):
    """
    :param bp_segment:
    :type bp_segment: serif.io.bpjson.abstract_events.Segment
    :param doc:
    :type doc: serif.theory.document.Document
    :param statistics:
    :type statistics: dict[str,int]
    :return:
    :rtype: (dict[str,int], serif.theory.document.Document)
    """

    def basic_event_arg_entities_bpjson_to_serifxml(
            event_mention,
            role_,
            argument_span_set_,
            doc_,
            span_to_mention,
            statistics_
    ):
        """
        :param event_mention:
        :type event_mention: serif.theory.event_mention.EventMention
        :param role_:
        :type role_: str
        :param argument_span_set_:
        :type argument_span_set_: serif.io.bpjson.abstract_events.SpanSet
        :param doc_:
        :type doc_: serif.theory.document.Document
        :param statistics_:
        :type statistics_: dict[str,int]
        :returns:
        :rtype: (dict[str,int], serif.theory.document.Document)
        """
        for span in argument_span_set_.spans:
            # Because the annotation only has a span set (not a span), we need to 
            # pick a span from the span set that is in the same sentence as an 
            # anchor
            sentence = None
            for event_mention_anchor in event_mention.anchors:
                anchor_sentence = doc_.sentences[event_mention_anchor.anchor_node.sent_no]
                if (anchor_sentence.token_sequence[0].start_char <= span.start and
                        anchor_sentence.token_sequence[-1].end_char >= (span.end - 1)):
                    sentence = anchor_sentence
                    break

            if sentence is None:
                continue

            if span in span_to_mention:
                event_mention.add_new_mention_argument(role_, span_to_mention[span], 1.0)

        return statistics_, doc_

    def basic_event_arg_ref_events_bpjson_to_serifxml(
            event_mention,
            role_,
            ref_event_mention,
            doc_,
            statistics_
    ):
        """

        :param event_mention:
        :type event_mention: serif.theory.event_mention.EventMention
        :param role_:
        :type role_: str
        :param ref_event_mentions:
        :type ref_event_mention: serif.theory.event_mention.EventMention
        :param doc_:
        :type doc_: serif.theory.document.Document
        :param statistics_:
        :type statistics_: dict[str,int]
        :return:
        :rtype: (dict[str,int], serif.theory.document.Document)
        """

        statistics_['basic_event:ref_event:event_mention_exact_match'] += 1
        event_mention.add_new_event_mention_argument(
            role_,
            ref_event_mention,
            1.0
        )
        return statistics_, doc_

    def basic_event_bpjson_to_serifxml(
            basic_event_,
            doc_,
            statistics_
    ):
        """
        :param basic_event_:
        :type basic_event_: serif.io.bpjson.reader.BasicEvent
        :param doc_:
        :type doc_: serif.theory.document.Document
        :param statistics_:
        :type statistics_: dict[str, int]
        :return:
        :rtype: (dict[str, int], serif.theory.document.Document, list[serif.theory.event_mention.EventMention])
        """
        added_ems = []
        event_type = basic_event_.event_type
        statistics_['basic_event:count'] += 1
        main_anchor_span = basic_event_.anchors.spans[0]  # type=serif.io.bpjson.reader.Span

        # Get sentence from first anchor
        sentence = get_sentence_by_offsets(doc, main_anchor_span.start, main_anchor_span.end)

        if sentence is None:
            return statistics_, doc_, None

        statistics_['basic_event:event_span:num_anchors'] += len(basic_event_.anchors.spans)
        if sentence.event_mention_set is None:
            sentence.add_new_event_mention_set()
        event_mention_set = sentence.event_mention_set

        overlap, start_token, end_token = find_tokens_by_offsets(
            sentence,
            main_anchor_span.start,
            main_anchor_span.end - 1)
        if start_token is None or end_token is None:
            statistics_['basic_event:event_span:anchor_tokens_partial_match'] += 1
            return statistics_, doc_, None

        # EventMention requires a syn_node by semantic_phrase_start/end
        # is used downstream.
        main_anchor_node = get_best_syn_node_for_span(doc_, main_anchor_span)
        if main_anchor_node is None:
            statistics_['basic_event:event_span:anchor_tokens_partial_match'] += 1
            return statistics_, doc_, None
        em = event_mention_set.add_new_event_mention(event_type, main_anchor_node, 1.0)
        em.pattern_id = basic_event_.event_id
        # print (em.pattern_id)
        em.add_new_event_mention_type(event_type, 1.0)
        em.semantic_phrase_start = \
            sentence.token_sequence.index(start_token)
        em.semantic_phrase_end = \
            sentence.token_sequence.index(end_token)
        em.state_of_affairs = basic_event_.state_of_affairs

        # capture 'hstring' from BP-JSON annotation into pair of token indices: (head_start, head_end)
        if (main_anchor_span.hstart is not None and main_anchor_span.hstart != -1) and (
                main_anchor_span.hend is not None and main_anchor_span.hend != -1):
            if main_anchor_span.hstart != main_anchor_span.start or main_anchor_span.hend != main_anchor_span.end:
                print('main_anchor_span head and span offsets are different! hstart={} start={} hend={} end={}'.format(
                    main_anchor_span.hstart, main_anchor_span.start, main_anchor_span.hend, main_anchor_span.end))
            _, hstart_token, hend_token = find_tokens_by_offsets(sentence, main_anchor_span.hstart,
                                                                 main_anchor_span.hend - 1)
            if hstart_token is not None and hend_token is not None:
                hstart_index = sentence.token_sequence.index(hstart_token)
                hend_index = sentence.token_sequence.index(hend_token)
                if hstart_index < em.semantic_phrase_start:  # sanity check: head cannot go beyond full extent
                    hstart_index = em.semantic_phrase_start
                if hend_index > em.semantic_phrase_end:  # sanity check: head cannot go beyond full extent
                    hend_index = em.semantic_phrase_end
                em.head_start = hstart_index
                em.head_end = hend_index

        # Add all anchors as EventMentionAnchor elements below EventMention
        for anchor_span in basic_event_.anchors.spans:
            anchor_node = get_best_syn_node_for_span(doc_, anchor_span)
            if anchor_node is not None:
                sentence = doc.sentences[anchor_node.sent_no]
                overlap, start_token, end_token = find_tokens_by_offsets(
                    sentence,
                    anchor_span.start,
                    anchor_span.end - 1)
                if start_token is not None and end_token is not None:
                    em_anchor = em.add_new_event_mention_anchor(anchor_node)
                    em_anchor.semantic_phrase_start = \
                        sentence.token_sequence.index(start_token)
                    em_anchor.semantic_phrase_end = \
                        sentence.token_sequence.index(end_token)

                    # capture 'hstring' from BP-JSON annotation into pair of token indices: (head_start, head_end)
                    if (anchor_span.hstart is not None and anchor_span.hstart != -1) and (
                            anchor_span.hend is not None and anchor_span.hend != -1):
                        _, hstart_token, hend_token = find_tokens_by_offsets(sentence, anchor_span.hstart,
                                                                             anchor_span.hend - 1)
                        if hstart_token is not None and hend_token is not None:
                            hstart_index = sentence.token_sequence.index(hstart_token)
                            hend_index = sentence.token_sequence.index(hend_token)
                            if hstart_index < em_anchor.semantic_phrase_start:  # sanity check: head cannot go beyond full extent
                                hstart_index = em_anchor.semantic_phrase_start
                            if hend_index > em_anchor.semantic_phrase_end:  # sanity check: head cannot go beyond full extent
                                hend_index = em_anchor.semantic_phrase_end
                            em_anchor.head_start = hstart_index
                            em_anchor.head_end = hend_index

        if overlap == 1.0:
            statistics_['basic_event:event_span:anchor_tokens_exact_match'] += 1
        else:
            statistics_['basic_event:event_span:anchor_tokens_partial_match'] += 1

        return statistics_, doc_, em

    logger.info("Handling Basic {}".format(doc.docid))

    event_id_to_event_mention = dict()
    for _, basic_event in bp_segment.basic_events.items():
        statistics, doc, added_event_mention = basic_event_bpjson_to_serifxml(
            basic_event,
            doc,
            statistics
        )
        if added_event_mention is not None:
            event_id_to_event_mention[basic_event.event_id] = added_event_mention

    span_to_mention, span_set_id_to_entity = basic_event_spans_to_serifxml(doc, bp_segment)
    # TODO: partial coreference

    for _, basic_event in bp_segment.basic_events.items():
        roles = list(
            chain(
                ['AGENT'] * len(basic_event.agent_span_sets),
                ['PATIENT'] * len(basic_event.patient_span_sets),
                ['MONEY'] * len(basic_event.money_span_sets),
            )
        )

        argument_span_sets = list(
            chain(
                basic_event.agent_span_sets,
                basic_event.patient_span_sets,
                basic_event.money_span_sets
            )
        )

        for role, argument_span_set in zip(roles, argument_span_sets):
            if basic_event.event_id in event_id_to_event_mention:
                statistics['basic_event:entity_arg_spansets'] += 1
                statistics, doc = basic_event_arg_entities_bpjson_to_serifxml(
                    event_id_to_event_mention[basic_event.event_id],
                    role,
                    argument_span_set,
                    doc,
                    span_to_mention,
                    statistics
                )

        for ref_event_id in basic_event.ref_events:
            if basic_event.event_id in event_id_to_event_mention and ref_event_id in event_id_to_event_mention:
                statistics['basic_event:ref_event:num_events'] += 1
                statistics, doc = basic_event_arg_ref_events_bpjson_to_serifxml(
                    event_id_to_event_mention[basic_event.event_id],
                    'REF_EVENT',
                    event_id_to_event_mention[ref_event_id],
                    doc,
                    statistics
                )

    return span_set_id_to_entity, event_id_to_event_mention


def granular_templates_bpjson_to_serifxml(
        bp_segment,
        doc,
        span_set_id_to_entity,
        event_id_to_event_mention,
        statistics
):
    """
    :param bp_segment:
    :type bp_segment: serif.io.bpjson.abstract_events.Segment
    :param doc:
    :type doc: serif.theory.document.Document
    :param statistics:
    :type statistics: dict[str,int]
    :return:
    :rtype: (dict[str,int], serif.theory.document.Document)
    """

    # for certain types_roles, we want to recast them from arguments to anchors
    # types_roles_to_swap = set(['Corruplate corrupt-event', 'Epidemiplate outbreak-event', 'Protestplate protest-event', 'Terrorplate terror-event'])
    types_roles_to_swap = set()  # do not do any swap
    logger.info("Handling Granular {}".format(doc.docid))

    event_set = doc.add_new_event_set()

    for events_ids in bp_segment.coref_events:
        event_mentions = []
        for event_id in events_ids:
            if event_id in event_id_to_event_mention:
                event_mentions.append(event_id_to_event_mention[event_id])
            else:
                logger.warning(
                    "doc: {} event_id: {} doesn't exist in added event mention set, we have to drop it".format(
                        doc.docid, event_id))

        if len(event_mentions) > 0:
            event_set.add_new_event(event_mentions, event_mentions[0].event_type)

    for granular_template in bp_segment.granular_templates.values():
        anchor_span_set = granular_template.anchor_span_set
        event = event_set.add_new_event([], granular_template.event_type)
        event.completion = granular_template.completion
        event.coordinated = granular_template.coordinated
        event.project_type = granular_template.project_type
        event.over_time = granular_template.over_time
        event.granular_template_type_attribute = granular_template.type_

        # Arguments
        for role, args in granular_template.role_to_args.items():
            # first check whether this is (granular event-type, role) that we want to recast from being an argument to an anchor
            type_role = '%s %s' % (granular_template.event_type, role)
            if type_role in types_roles_to_swap:
                continue

            for arg in args:
                if "event" in arg:
                    bp_json_event = arg["event"]
                    if bp_json_event.event_id in event_id_to_event_mention:
                        event.add_new_argument(role, event_id_to_event_mention[bp_json_event.event_id], 1.0)
                    else:
                        logger.error(
                            "Due to upstream failed to create event mention, we cannot add template doc {} arg {}".format(
                                doc.docid, arg))
                        continue
                else:
                    span_set = arg["span_set"]
                    if span_set.ss_id in span_set_id_to_entity:
                        event.add_new_argument(role, span_set_id_to_entity[span_set.ss_id], 1.0)
                    else:
                        logger.error(
                            "Due to upstream failed to create mention, we cannot add template doc {} arg {}".format(
                                doc.docid, arg))
                        continue

                if "irrealis" in arg:
                    event.arguments[-1].irrealis = arg["irrealis"]

                if "time-attachments" in arg:
                    entities = []
                    for span_set_id in arg["time-attachments"]:
                        entities.append(span_set_id_to_entity[span_set_id])
                    event.arguments[-1].time_attachments = entities

        # Anchors
        for anchor_span in anchor_span_set.spans:
            anchor_node = get_best_syn_node_for_span(doc, anchor_span)
            if anchor_node is not None:
                event_anchor = event.add_new_event_anchor(anchor_node)

        # Recasting some arguments as anchors
        for role, args in granular_template.role_to_args.items():
            type_role = '%s %s' % (granular_template.event_type, role)
            if type_role in types_roles_to_swap:
                for arg in args:
                    if "event" in arg:
                        bp_json_event = arg["event"]
                        em = event_id_to_event_mention[bp_json_event.event_id]
                        for anchor in em.anchors:
                            event.add_new_event_anchor(anchor.anchor_node)


def _main(args):
    raise NotImplemented("This is not actively maintained")

    bp_corpus = Corpus.from_file(args.bp_json_file)
    entry_id_to_sentence = dict()
    entry_id_to_segment = dict()
    # for _, doc in bp_corpus.docs.items():
    #     for sentence in doc.sentences:
    #         entry_id_to_sentence[sentence.entry_id] = sentence
    #     for segment in doc.segments:
    #         entry_id_to_segment[segment.entry_id] = segment
    for segment in bp_corpus.segments:
        entry_id_to_segment[segment.entry_id] = segment

    file_paths = [x.strip() for x in open(args.input_list).readlines()]
    os.makedirs(args.output_serifxml_dir, exist_ok=True)

    # annotations = get_frame_annotations_from_bp(bp_corpus)
    """:type: dict[str, list[list[nlplingo.text.text_span.LabeledTextFrame]]]"""
    diff_docids = dict()

    statistics = defaultdict(int)
    basic_events_statistics = defaultdict(int)

    for file_path in file_paths:
        doc = serifxml3.Document(file_path)

        # abstract events
        if doc.docid in entry_id_to_sentence:
            abstract_events_bp_json_to_serifxml(
                doc,
                entry_id_to_sentence[doc.docid],
                statistics
            )
        # basic events
        if doc.docid in entry_id_to_segment:
            basic_events_statistics, doc_ = basic_events_bpjson_to_serifxml(
                entry_id_to_segment[doc.docid],
                doc,
                basic_events_statistics
            )

        doc.save(
            os.path.join(
                args.output_serifxml_dir,
                "{}.xml".format(
                    doc.docid
                )
            )
        )

        # compare BP against annotations that we create in SerifXML doc
        # currently on check for abstract events
        # if doc.docid in annotations:
        #     doc_annotation = annotations[doc.docid]
        #     compare_annotations(doc_annotation, doc, diff_docids)

    for k in statistics:
        c = statistics[k]
        if k != 'num_arguments':
            logger.info('{} {} ={}%'.format(k, str(c), '%.1f' % (100 * float(c) / float(statistics['num_arguments']))))
        else:
            logger.info('{} {}'.format(k, str(c)))

    for k in sorted(basic_events_statistics):
        c = basic_events_statistics[k]
        if k != 'basic_event:entity_arg:num_arguments':
            if 'basic_event:entity_arg:' in k:
                logger.info(
                    '{} {} ={}%'.format(
                        k,
                        str(c),
                        '%.1f' % (
                                100 * float(c) /
                                float(
                                    basic_events_statistics['basic_event:entity_arg:num_arguments']
                                )
                        )
                    )
                )
            else:
                logger.info('{} {}'.format(k, str(c)))
        else:
            logger.info('{} {}'.format(k, str(c)))

    # print('#### {} docids with differences'.format(str(len(diff_docids)))) Could reveal hidden data
    # for docid in sorted(diff_docids):
    #     print('******** ' + docid)
    #     print(diff_docids[docid])


def _parser_setup():
    parser = argparse.ArgumentParser(
        description="Add event mentions from bp_json format to serifxml files in list."
                    "This program assumes that the serifxml docid is the "
                    "entry_id in bp_json."
    )
    parser.add_argument(
        '--input_list',
        required=True,
        help='Path to file containing a list of SERIFXML paths '
             'separated by newlines.'
    )
    parser.add_argument(
        '--bp_json_file',
        required=True,
        help='Path to BPJSON file to read in'
    )
    parser.add_argument(
        '--output_serifxml_dir',
        required=True,
        help='Path to directory to write out SERIFXML files to'
    )
    return parser


if __name__ == '__main__':
    _main(_parser_setup().parse_args())
