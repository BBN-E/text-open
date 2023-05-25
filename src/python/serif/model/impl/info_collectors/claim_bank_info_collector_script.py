import json
import os
import re
import argparse

import multiprocessing
from multiprocessing.managers import BaseManager, DictProxy

from collections import defaultdict

from serifxml3 import Document

from serif.theory.mention import Mention


whitespace_re = re.compile(r"\s+")


def build_mention_to_actor_mentions_map(serif_doc):
    mention_to_actor_mentions_map = defaultdict(list)
    for s in serif_doc.sentences:
        if s.actor_mention_set is not None:
            for am in s.actor_mention_set:
                mention_to_actor_mentions_map[am.mention].append(am)
    return mention_to_actor_mentions_map

def modal_dfs(mtrm, *, conceivers=(), depth=0, event_mention_to_conceivers_map=dict()):
    '''
    :param mtrm: ModalTemporalRelationMention
    :param conceivers: list of conceivers governing current node

    This will do a dfs starting from the root of the modal parse, and at each call it will try to assign the kb element
    corresponding to the current event node with attributes coming from conceivers that are its predecessors in the parse
    '''

    node = mtrm.node  # contains serifxml object
    node_type = node.modal_temporal_node_type  # 'ROOT_NODE', 'AUTHOR_NODE', Conceiver, Event
    rel = node.relation_type  # 'Depends-on', 'pos', 'pp', 'neg', ...

    # print("^^^^^^^")
    # print("\t" * depth + f"node_type: {node_type}")
    # print("\t" * depth + f"node.event_mention: {[node.event_mention]}")
    # print("\t" * depth + f"node.value: {[node.value]}")
    # if hasattr(node.value, 'event_type'):
    #     print("\t" * depth + f"node.value.event_type: {[node.value.event_type]}")
    # print("\t" * depth + f"rel: {rel}")

    # add Conceiver info to current Event
    if (node_type == "Event" or node_type == "Event_SIP") and node.event_mention is not None:  # and node.value.event_type != "MTDP_EVENT":
        # print("\t" * depth + f"conceivers: {str(conceivers)}")
        event_mention_to_conceivers_map[
            (node.value.event_type, node.event_mention.id, node_type, node.document.docid)] = conceivers

    if len(mtrm.children) > 0:  # non-terminal node

        for child_mtrm in mtrm.children:
            # we're looking at the triples (node, child_rel, child_node)

            child_node = child_mtrm.node  # contains serifxml object
            child_node_type = child_node.modal_temporal_node_type  # 'ROOT_NODE', 'AUTHOR_NODE', Conceiver, Event
            child_rel = child_node.relation_type  # 'Depends-on', 'pos', 'pp', 'neg', ...

            child_conceivers = list(conceivers)
            if node_type == "Conceiver":
                # if type(node.value) is not str: print(node.value.text)
                child_conceivers.append((node.value, child_rel))

            modal_dfs(child_mtrm, conceivers=child_conceivers, depth=depth + 1,
                      event_mention_to_conceivers_map=event_mention_to_conceivers_map)

    return event_mention_to_conceivers_map

def get_canonical_name_for_conceiver(conceiver, mention_to_actor_mentions_map,
                                     use_conceiver_mention_only=True):
    ret = {"canonical_name": None,  # e.g. "Donald Trump"
           "conceiver_mention_type": None,  # "name", "desc", "pron" etc.
           "entity_type": None,  # e.g. "PERSON", "ORGANIZATION", "MTDP_CONCEIVER"
           "qid": None,
           "actor_name": None}

    if type(conceiver) is str and conceiver == "AUTHOR_NODE":
        ret["conceiver_mention_type"] = "AUTHOR"

    elif type(conceiver) == Mention:

        if use_conceiver_mention_only:  # look only at current mention for canonical name

            if conceiver.start_token is not None and conceiver.end_token is not None:
                conceiver_text = conceiver.get_original_text_substring(conceiver.start_token.start_char,
                                                                       conceiver.end_token.end_char)
            else:
                conceiver_text = conceiver.get_original_text_substring(conceiver.syn_node.start_char,
                                                                       conceiver.syn_node.end_char)
            conceiver_text = whitespace_re.sub(" ", conceiver_text).replace("\n", " ").replace("\t", " ")
            if len(conceiver_text) < 2:
                return ret

            ret["canonical_name"] = conceiver_text
            ret["entity_type"] = conceiver.entity_type
            ret["conceiver_mention_type"] = conceiver.mention_type.value

            actor_mentions_for_conceiver = mention_to_actor_mentions_map.get(conceiver, [])
            qids = [am.actor_db_name for am in actor_mentions_for_conceiver]
            actor_names = [am.actor_name for am in actor_mentions_for_conceiver]

            if len(qids) >= 1:
                ret["qid"] = qids
                ret["actor_name"] = actor_names

        # else:  # look at governing entity for canonical name
        #
        #     entity = conceiver.entity()
        #     if entity is not None:
        #
        #         longest_name = ""
        #         for mention in entity.mentions:
        #             if mention.mention_type.value == "name":
        #                 mention_head_text = mention.get_original_text_substring(mention.head.start_char, mention.head.end_char)
        #                 mention_head_text = whitespace_re.sub(" ", mention_head_text).replace("\n", " ").replace("\t", " ")
        #                 if len(mention_head_text) > len(longest_name):
        #                     longest_name = mention_head_text
        #         if longest_name != "":
        #             ret["canonical_name"] = longest_name

    return ret

def get_serif_list(args):
    serif_list = []
    for root, dirs, files in os.walk(args.input):
        for fname in files:
            fpath = os.path.join(root, fname)
            if fpath.endswith(".xml"):
                serif_list.append(fpath)
    return serif_list

def single_document_handler_get_conceiver_event_counts(serif_path):

    serif_doc = Document(serif_path)

    CONCEIVER_TO_EVENT_TYPE_COUNTS = dict()
    STATS = {"# event mentions": sum(len(s.event_mention_set) for s in serif_doc.sentences),
             "# event mentions with type MTDP_EVENT": sum(len([e for e in s.event_mention_set if e.event_type == "MTDP_EVENT"]) for s in serif_doc.sentences),
             "# event mentions with type not MTDP_EVENT": sum(len([e for e in s.event_mention_set if e.event_type != "MTDP_EVENT"]) for s in serif_doc.sentences),
             "# event mentions with conceiver": 0,
             # "# event mentions with author conceiver": 0,
             "# event mentions with non-author conceiver": 0,
             "# event mentions with conceiver of type MTDP_CONCEIVER": 0,
             "# event mentions with non-author name mention conceiver": 0,

             "# conceivers": 0,
             # "# author conceivers": 0,
             "# non-author conceivers": 0,
             "# non-author conceivers of type MTDP_CONCEIVER": 0,
             "# non-author name mention conceivers": 0}

    mention_to_actor_mentions_map = build_mention_to_actor_mentions_map(serif_doc)
    event_mention_to_conceivers_map = dict()

    if hasattr(serif_doc, "modal_temporal_relation_mention_set") and \
            serif_doc.modal_temporal_relation_mention_set is not None:

        if serif_doc.modal_temporal_relation_mention_set.modal_root:
            modal_root = serif_doc.modal_temporal_relation_mention_set.modal_root
            assert modal_root.node.value == "ROOT_NODE"
            event_mention_to_conceivers_map = modal_dfs(modal_root, conceivers=[], depth=0,
                                                        event_mention_to_conceivers_map=event_mention_to_conceivers_map)

            for (event_type, emid, mdp_node_type, docid), conceivers in event_mention_to_conceivers_map.items():

                conceiver_infos = [
                    get_canonical_name_for_conceiver(c, mention_to_actor_mentions_map=mention_to_actor_mentions_map, use_conceiver_mention_only=True)
                    for (c, _) in conceivers]

                if len(conceiver_infos) > 0:
                    STATS["# event mentions with conceiver"] += 1
                STATS["# conceivers"] += len(conceiver_infos)

                non_author_conceiver_infos = [i for i in conceiver_infos if i["conceiver_mention_type"] != "AUTHOR"]
                if len(non_author_conceiver_infos) > 0:
                    STATS["# event mentions with non-author conceiver"] += 1
                STATS["# non-author conceivers"] += len(non_author_conceiver_infos)

                non_author_conceivers_MTDP_CONCEIVER_infos = [i for i in non_author_conceiver_infos if i["entity_type"] == "MTDP_CONCEIVER"]
                if len(non_author_conceivers_MTDP_CONCEIVER_infos) > 0:
                    STATS["# event mentions with conceiver of type MTDP_CONCEIVER"] += 1
                STATS["# non-author conceivers of type MTDP_CONCEIVER"] += len(non_author_conceivers_MTDP_CONCEIVER_infos)

                non_author_name_mention_conceiver_infos = [i for i in non_author_conceiver_infos if i["conceiver_mention_type"] == "name"]
                if len(non_author_name_mention_conceiver_infos) > 0:
                    STATS["# event mentions with non-author name mention conceiver"] += 1
                STATS["# non-author name mention conceivers"] += len(non_author_name_mention_conceiver_infos)

                event_mention_to_conceivers_map[(event_type, emid, mdp_node_type, docid)] = conceiver_infos

                if event_type != "MTDP_EVENT":
                    for conceiver_info in non_author_conceiver_infos:
                        # actor_name = max(conceiver_info["actor_name"], key=lambda x: len(x))
                        if conceiver_info["actor_name"] is not None:
                            for actor_name in conceiver_info["actor_name"]:

                                if actor_name not in CONCEIVER_TO_EVENT_TYPE_COUNTS:
                                    CONCEIVER_TO_EVENT_TYPE_COUNTS[actor_name] = dict()
                                if event_type not in CONCEIVER_TO_EVENT_TYPE_COUNTS[actor_name]:
                                    CONCEIVER_TO_EVENT_TYPE_COUNTS[actor_name][event_type] = 0
                                CONCEIVER_TO_EVENT_TYPE_COUNTS[actor_name][event_type] += 1

    return CONCEIVER_TO_EVENT_TYPE_COUNTS, STATS

def single_document_handler_get_conceiver_canonical_name_list(serif_path):

    serif_doc = Document(serif_path)
    name_mentions_canonical_names = list()
    all_mentions_canonical_names = list()

    event_mention_to_conceivers_map = dict()

    if hasattr(serif_doc, "modal_temporal_relation_mention_set") and \
            serif_doc.modal_temporal_relation_mention_set is not None:

        if serif_doc.modal_temporal_relation_mention_set.modal_root:
            modal_root = serif_doc.modal_temporal_relation_mention_set.modal_root
            assert modal_root.node.value == "ROOT_NODE"
            event_mention_to_conceivers_map = modal_dfs(modal_root, conceivers=[], depth=0,
                                                        event_mention_to_conceivers_map=event_mention_to_conceivers_map)

            for _, conceivers in event_mention_to_conceivers_map.items():

                conceiver_infos = [
                    get_canonical_name_for_conceiver(c, mention_to_actor_mentions_map=dict(), use_conceiver_mention_only=True)
                    for (c, _) in conceivers]

                name_mentions_canonical_names.extend([i["canonical_name"] for i in conceiver_infos
                                                      if (i["conceiver_mention_type"] == "name" and i["canonical_name"] is not None)])
                all_mentions_canonical_names.extend([i["canonical_name"] for i in conceiver_infos
                                                     if (i["conceiver_mention_type"] != "AUTHOR" and i["canonical_name"] is not None)])

    return name_mentions_canonical_names

def main(args):

    # setup for different options
    if args.mode == "conceiver_event_counts":
        CONCEIVER_TO_EVENT_TYPE_COUNTS = defaultdict(lambda: defaultdict(int))
        STATS = defaultdict(int)
        single_document_handler = single_document_handler_get_conceiver_event_counts
    elif args.mode == "conceiver_canonical_names":
        CONCEIVER_CANONICAL_NAMES = list()
        single_document_handler = single_document_handler_get_conceiver_canonical_name_list

    serif_list = get_serif_list(args)#[:1000]
    # serif_list = ["/d4m/ears/expts/48311.claim_bank_en_zh.v1/expts/claim-bank/entity_linking/en/split/0/ENG_NW_WM_aylien_3000119133_3.xml"]

    manager = multiprocessing.Manager()
    with manager.Pool(multiprocessing.cpu_count()) as pool:

        workers = list()

        for serif_path in serif_list:
            workers.append(pool.apply_async(single_document_handler, args=(serif_path,)))

        for idx,i in enumerate(workers):
            i.wait()
            buf = i.get()

            # aggregate output
            if args.mode == "conceiver_event_counts":
                # print(CONCEIVER_TO_EVENT_TYPE_COUNTS)
                for conceiver, event_type_count in buf[0].items():
                    for event_type, count in event_type_count.items():
                        CONCEIVER_TO_EVENT_TYPE_COUNTS[conceiver][event_type] += count
                for stat, count in buf[1].items():
                    STATS[stat] += count
            elif args.mode == "conceiver_canonical_names":
                CONCEIVER_CANONICAL_NAMES.extend(buf)

    # write/print output
    if args.mode == "conceiver_event_counts":
        conceivers_sorted_inverse = sorted(list(CONCEIVER_TO_EVENT_TYPE_COUNTS.keys()),
                                           key=lambda x: sum(v for k, v in CONCEIVER_TO_EVENT_TYPE_COUNTS[x].items()),
                                           reverse=True)
        pprint_lines = []
        for c in conceivers_sorted_inverse:
            conceiver_pprint_line = "\t\t".join([str(sum(v for k, v in CONCEIVER_TO_EVENT_TYPE_COUNTS[c].items())), c, " ".join(
                [k + ":" + str(v) for k, v in CONCEIVER_TO_EVENT_TYPE_COUNTS[c].items()])])
            pprint_lines.append(conceiver_pprint_line)
        with open(args.output, "w") as f:
            f.write("\n".join(pprint_lines))
        for k, v in STATS.items():
            print(k, "\t\t", v)
    elif args.mode == "conceiver_canonical_names":
        with open(args.output, "w") as f:
            f.write("\n".join(sorted(CONCEIVER_CANONICAL_NAMES)))

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--input", help="serifxmls_root", type=str)
    parser.add_argument("-o", "--output", help="output directory", type=str)
    parser.add_argument("--mode", required=True, choices=["conceiver_event_counts", "conceiver_canonical_names"], type=str)
    args = parser.parse_args()

    main(args)