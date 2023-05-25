"""
Copyright 2020 Raytheon BBN Technologies
All Rights Reserved
"""

import enum
import logging
import re
import sys
from collections import defaultdict

import numpy as np

logger = logging.getLogger(__name__)


class OffsetSchema(enum.IntEnum):
    CHAR = enum.auto()
    EDT = enum.auto()


def enumerate_syn_nodes(node):
    """
    Traverses the parse tree in a DFS fashion to enumerate
    all of the different SynNodes in a parse
    :param node: Root SynNode to start traversal for enumeration
    :type node: serifxml3.SynNode
    :return: list[serifxml3.SynNode]

    """
    retVal = dict()

    def visit(node, seen):
        if not node.is_terminal:
            tok_seq = list(node.parse.token_sequence)
            seen[
                (
                    tok_seq.index(node.start_token),
                    tok_seq.index(node.end_token)
                )
            ] = node
            for child in node:
                visit(child, seen)
        return seen

    syn_nodes = visit(node, retVal)
    return [syn_nodes[k] for k in sorted(syn_nodes)]


def find_valid_syn_nodes_by_offsets(
        st,
        start_char,
        end_char,
        overlap_threshold,
        offset_schema=OffsetSchema.CHAR
):
    overlap = 0
    overlap_anchors = []
    if st.parse:
        syn_node_candidates = enumerate_syn_nodes(st.parse.root)
        overlap, overlap_anchors = find_objs_with_best_overlap_by_offsets(
            syn_node_candidates,
            start_char,
            end_char,
            overlap_threshold,
            offset_schema=offset_schema
        )
    return overlap, overlap_anchors


def find_smallest_synnode_starting_at_offset(
        st,
        start_char
):
    smallest_syn_node = None
    if st.parse:
        syn_node_candidates = enumerate_syn_nodes(st.parse.root)
        for syn_node_candidate in syn_node_candidates:
            if syn_node_candidate.start_token.start_char != start_char:
                continue
            if smallest_syn_node is None or len(syn_node_candidate.tokens) < len(smallest_syn_node.tokens):
                smallest_syn_node = syn_node_candidate
    return smallest_syn_node


def find_valid_anchors_by_token_index(
        st,
        start_token,
        end_token,
        overlap_threshold
):
    overlap = 0
    overlap_anchors = []
    if st.parse:
        syn_node_candidates = enumerate_syn_nodes(st.parse.root)
        overlap, overlap_anchors = find_syn_nodes_with_best_overlap_by_token_index(
            syn_node_candidates,
            start_token,
            end_token,
            overlap_threshold
        )
    return overlap, overlap_anchors


def find_valid_mentions_by_token_index(
        mention_set,
        start_token,
        end_token,
        overlap_threshold
):
    syn_node_candidates = [x.syn_node for x in mention_set]
    overlap, overlap_syn_nodes = find_syn_nodes_with_best_overlap_by_token_index(
        syn_node_candidates,
        start_token,
        end_token,
        overlap_threshold
    )
    overlap_mentions = [
        mention_set[syn_node_candidates.index(x)]
        for x in overlap_syn_nodes
    ]
    return overlap, overlap_mentions


def find_valid_event_mention_by_token_index_no_syn_node(
        event_mention_set,
        start_token_index,
        end_token_index,
):
    for em in event_mention_set:
        if em.anchor_node is not None:
            if em.anchor_node.start_token.index() == start_token_index and em.anchor_node.end_token.index() == end_token_index:
                return em
        elif em.semantic_phrase_start is not None and em.semantic_phrase_end:
            if em.semantic_phrase_start == start_token_index and em.semantic_phrase_end == end_token_index:
                return em
    return None


def find_syn_nodes_with_best_overlap_by_token_index(
        objects,
        start_token_index,
        end_token_index,
        overlap_threshold=0.9
):
    best_overlap = 0.
    overlap_to_candidates = defaultdict(list)
    # print('START find_syn_nodes_with_best_overlap_by_token_index')
    for obj in objects:
        tok_seq = list(obj.parse.token_sequence)
        candidate_start_char = tok_seq.index(obj.start_token)
        candidate_end_char = tok_seq.index(obj.end_token)
        overlap = iou_1d(
            start_token_index,
            end_token_index,
            candidate_start_char,
            candidate_end_char
        )
        # print(
        #     "Overlap: {} OBJ: {} SEARCH: {} {} CANDIDATE: {} {}".format(
        #         overlap,
        #         obj.text,
        #         start_token_index,
        #         end_token_index,
        #         candidate_start_char,
        #         candidate_end_char
        #     )
        # )
        if overlap >= overlap_threshold and overlap > 0:
            if overlap >= best_overlap:
                best_overlap = overlap
                overlap_to_candidates[overlap].append(obj)
    # print(
    #     "Selected: " + ", ".join(
    #         [
    #             x.text
    #             for x in overlap_to_candidates[best_overlap]
    #         ]
    #     )
    # )
    # print('END find_syn_nodes_with_best_overlap_by_token_index')
    return (best_overlap, []) if len(overlap_to_candidates) == 0 else (
        best_overlap, overlap_to_candidates[best_overlap])


def find_objs_with_best_overlap_by_offsets(objects, start_char_or_edt, end_char_or_edt, overlap_threshold=0.9,
                                           offset_schema=OffsetSchema.CHAR):
    # print('START find_objs_with_best_overlap_by_offsets')
    best_overlap = 0.
    overlap_to_candidates = defaultdict(list)
    for obj in objects:
        candidate_start_char = None
        candidate_end_char = None
        if offset_schema is OffsetSchema.CHAR:
            candidate_start_char = obj.start_char
            candidate_end_char = obj.end_char + 1
        elif offset_schema is OffsetSchema.EDT:
            candidate_start_char = obj.start_edt
            candidate_end_char = obj.end_edt + 1
        else:
            raise NotImplementedError()
        overlap = iou_1d(
            start_char_or_edt,
            end_char_or_edt,
            candidate_start_char,
            candidate_end_char
        )
        # print(
        #     "Overlap: {} OBJ: {} SEARCH: {} {} CANDIDATE: {} {}".format(
        #         overlap,
        #         obj.text,
        #         start_char,
        #         end_char,
        #         candidate_start_char,
        #         candidate_end_char
        #     )
        # )
        if overlap >= overlap_threshold and overlap > 0:
            if overlap >= best_overlap:
                best_overlap = overlap
                overlap_to_candidates[overlap].append(obj)
    # print(
    #     "Selected: " + ", ".join(
    #         [
    #             x.text
    #             for x in overlap_to_candidates[best_overlap]
    #         ]
    #     )
    # )
    # print('END find_objs_with_best_overlap_by_offsets')
    return (best_overlap, []) if len(overlap_to_candidates) == 0 else (
        best_overlap, overlap_to_candidates[best_overlap])


def find_valid_mentions_for_mention_offset(
        mention_set,
        start_char,
        end_char,
        overlap_threshold,
        offset_schema=OffsetSchema.CHAR
):
    overlap_proportion, overlap_mentions = find_objs_with_best_overlap_by_offsets(
        mention_set,
        start_char,
        end_char,
        overlap_threshold,
        offset_schema=offset_schema
    )
    return overlap_proportion, overlap_mentions


def find_tokens_by_offsets(sentence, start_char_or_edt, end_char_or_edt, offset_schema=OffsetSchema.CHAR):
    """
    Tries to find the best subsequence of tokens in a sentence based off of the provided start_edt and end_edt integer offsets.
    This is done by walking the token sequence linearly util the closest start_edt/end_edt can be found.
    """
    if offset_schema not in {OffsetSchema.EDT, OffsetSchema.CHAR}:
        raise NotImplementedError("Cannot support {}".format(offset_schema))

    found_start_token = None
    found_end_token = None
    found_start_token_cost = sys.maxsize
    found_end_token_cost = sys.maxsize

    if (
            offset_schema is OffsetSchema.CHAR
            and start_char_or_edt < sentence.start_char
            or end_char_or_edt > sentence.end_char
    ):
        # This is only supported under CHAR mode due to `get_original_text_substring` is using CHAR.
        logger.info("Using shrinking strategy for "
                    f'start_char: {start_char_or_edt} '
                    f'end_char: {end_char_or_edt} '
                    f'sentence start_char: {sentence.start_char} '
                    f'sentence end_char: {sentence.end_char}'
                    )
        old_start_char = start_char_or_edt
        while start_char_or_edt < sentence.end_char and sentence.get_original_text_substring(start_char_or_edt,
                                                                                             start_char_or_edt).isalnum() is False:
            start_char_or_edt += 1

        if start_char_or_edt != old_start_char:
            logger.info("Adjusted start_char {} from {}".format(start_char_or_edt, old_start_char))

        old_end_char = end_char_or_edt
        while end_char_or_edt > sentence.start_char and sentence.get_original_text_substring(end_char_or_edt,
                                                                                             end_char_or_edt).isalnum() is False:
            end_char_or_edt -= 1

        if end_char_or_edt != old_end_char:
            logger.info("Adjusted end_char {} from {}".format(end_char_or_edt, old_end_char))

        if (
                start_char_or_edt < sentence.start_char
                or end_char_or_edt > sentence.end_char
        ):
            logger.error("Unrecoverable annotation "
                         f'start_char: {old_start_char} '
                         f'end_char: {old_end_char} '
                         f'sentence start_char: {sentence.start_char} '
                         f'sentence end_char: {sentence.end_char}'
                         )
            return 0.0, None, None

    token_seq = sentence.token_sequence

    for token in token_seq:
        start_token_cost = None
        end_token_cost = None
        if offset_schema is OffsetSchema.EDT:
            start_token_cost = abs(start_char_or_edt - token.start_edt)
            end_token_cost = abs(token.end_edt - end_char_or_edt)
        elif offset_schema is OffsetSchema.CHAR:
            start_token_cost = abs(start_char_or_edt - token.start_char)
            end_token_cost = abs(token.end_char - end_char_or_edt)
        if 0 <= start_token_cost < found_start_token_cost:
            found_start_token = token
            found_start_token_cost = start_token_cost
        if 0 <= end_token_cost < found_end_token_cost:
            found_end_token = token
            found_end_token_cost = end_token_cost
    if found_end_token is None:
        found_end_token = found_start_token

    if offset_schema is OffsetSchema.EDT:
        if found_start_token.start_edt > found_end_token.end_edt:
            found_end_token = found_start_token
    elif offset_schema is OffsetSchema.CHAR:
        if found_start_token.start_char > found_end_token.end_char:
            found_end_token = found_start_token

    overlap = None
    if offset_schema is OffsetSchema.EDT:
        overlap = iou_1d(
            found_start_token.start_edt,
            found_end_token.end_edt,
            start_char_or_edt,
            end_char_or_edt
        )
    elif offset_schema is OffsetSchema.CHAR:
        overlap = iou_1d(
            found_start_token.start_char,
            found_end_token.end_char,
            start_char_or_edt,
            end_char_or_edt
        )

    if offset_schema is OffsetSchema.EDT:
        logger.debug(f'query start: {start_char_or_edt} found start: {found_start_token.start_edt} '
                     f'query end: {end_char_or_edt} found end: {found_end_token.end_edt}')
    elif offset_schema is OffsetSchema.CHAR:
        logger.debug(f'query start: {start_char_or_edt} found start: {found_start_token.start_char} '
                     f'query end: {end_char_or_edt} found end: {found_end_token.end_char}')
    logger.debug(f'overlap: {overlap}')

    return overlap, found_start_token, found_end_token


def find_valid_anchors(st, anchor_text, overlap_threshold=0.9):
    overlap_anchors = []
    if st.parse:
        syn_node_candidates = enumerate_syn_nodes(st.parse.root)
        overlap_proportion, overlap_anchors = find_objects_with_best_text_overlap(
            syn_node_candidates,
            anchor_text,
            overlap_threshold
        )
    return overlap_anchors


def create_mentions_for_text(sentence, anchor_text, overlap_threshold=0.9):
    mentions = []
    syn_nodes = find_valid_anchors(
        sentence.sentence_theories[0],
        anchor_text,
        overlap_threshold
    )
    for syn_node in syn_nodes:
        mention = sentence.mention_set.add_new_mention(
            syn_node,
            'NONE',
            'OTH'
        )
        mentions.append(mention)
    if len(mentions) == 0:
        raise RuntimeError('Couldn\'t create mention for: {}'.format(anchor_text))
    return mentions


def consolidate_serif_offset_theory_objs(objs):
    consolidate_dict = dict()
    for obj in objs:
        consolidate_dict[(obj.start_char, obj.end_char)] = obj
    return list(consolidate_dict.values())


def consolidate_token_pairs(pair_list):
    consolidate_dict = dict()
    for pair in pair_list:
        consolidate_dict[(pair[0].start_char, pair[1].end_char)] = pair
    return list(consolidate_dict.values())


def find_objects_with_best_text_overlap(objects, query_text, overlap_threshold=0.9, usehead=False):
    best_overlap = 0.
    overlap_to_candidates = defaultdict(list)
    # print('START find_objects_with_best_text_overlap')
    if objects is not None:
        for obj in objects:
            if usehead:
                obj_text = obj.head.text
            else:
                obj_text = obj.text
            overlap = 0.
            if query_text in obj_text:
                overlap = float(len(query_text)) / len(obj_text)
            elif obj_text in query_text:
                overlap = float(len(obj_text)) / len(query_text)
            if overlap > overlap_threshold:
                if overlap >= best_overlap:
                    best_overlap = overlap
                    overlap_to_candidates[overlap].append(obj)
        overlap_to_candidates[best_overlap] = consolidate_serif_offset_theory_objs(
            overlap_to_candidates[best_overlap]
        )
    # print(
    #     "Selected: " + ", ".join(
    #         [
    #             x.text
    #             for x in overlap_to_candidates[best_overlap]
    #         ]
    #     )
    # )
    # print('END find_objects_with_best_text_overlap')
    return (best_overlap, []) \
        if len(overlap_to_candidates) == 0 \
        else (best_overlap, overlap_to_candidates[best_overlap])


def find_valid_mentions_for_mention_text(
        mention_set,
        mention_text,
        overlap_threshold=0.9,
        usehead=False
):
    (overlap_proportion, overlap_mentions) = find_objects_with_best_text_overlap(
        mention_set,
        mention_text,
        overlap_threshold,
        usehead
    )

    return overlap_proportion, overlap_mentions


def choose_shortest_mention(mentions):
    shortest_len = 999
    shortest_mention = None
    for m in mentions:
        if len(m.text) < shortest_len:
            shortest_len = len(m.text)
            shortest_mention = m
    return shortest_mention


def find_tokens_by_text(
        sentence,
        text
):
    matches_in_segment = re.finditer(
        re.escape(text),
        sentence.text
    )
    best_overlap = 0
    overlap_to_candidates = defaultdict(list)
    for m in matches_in_segment:
        start_char = sentence.token_sequence[0].start_char + m.start()
        end_char = sentence.token_sequence[0].start_char + m.end() - 1
        overlap, found_start_token, found_end_token = find_tokens_by_offsets(
            sentence,
            start_char,
            end_char
        )
        if found_start_token is not None and found_end_token is not None and overlap > best_overlap:
            overlap_to_candidates[overlap].append(
                (
                    found_start_token,
                    found_end_token
                )
            )
            best_overlap = overlap
    if len(overlap_to_candidates) < 1:
        logger.error("Could not find any overlapping tokens")
    else:
        overlap_to_candidates[best_overlap] = consolidate_token_pairs(overlap_to_candidates[best_overlap])

    return (best_overlap, []) \
        if len(overlap_to_candidates) == 0 \
        else (best_overlap, overlap_to_candidates[best_overlap])


def iou_1d(
        a_lowerbound,
        a_upperbound,
        b_lowerbound,
        b_upperbound
):
    """
    Intersection over the union in one dimension.

    :param a_lowerbound: float
    :param a_upperbound: float
    :param b_lowerbound: float
    :param b_upperbound: float
    :return:
    """
    index_list = [
        a_lowerbound,
        b_lowerbound,
        a_upperbound,
        b_upperbound
    ]
    result = np.argsort(index_list, kind='mergesort')
    if {0, 1} == set(result[:2]):
        intersection = (index_list[result[2]] - index_list[result[1]]) + 1
        union = index_list[result[3]] - index_list[result[0]] + 1
        overlap = float(intersection) / union
    else:
        overlap = 0.
    return overlap
