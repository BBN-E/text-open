import json
import os
import re
import argparse

import multiprocessing

from collections import defaultdict

from serifxml3 import Document

from serif.theory.mention import Mention


whitespace_re = re.compile(r"\s+")
location_roles = {"has_location", "has_origin_location", "has_destination_location", "Place", "Origin", "Destination"}



def build_mention_to_actor_mentions_map(serif_doc, source_note="geonames"):
    mention_to_actor_mentions_map = defaultdict(list)
    for s in serif_doc.sentences:
        if s.actor_mention_set is not None:
            for am in s.actor_mention_set:
                if am.source_note == source_note:
                    assert am.mention.mention_type.value == "name"  # linking only name mentions
                    mention_to_actor_mentions_map[am.mention].append(am)
    return mention_to_actor_mentions_map

def resolve_link(mention, mention_to_actor_mentions_map):
    entity = mention.entity()
    governed_mentions = entity.mentions
    candidate_actor_mentions = []
    for gm in governed_mentions:
        if gm != mention:
            if gm in mention_to_actor_mentions_map:
                candidate_actor_mentions.extend(mention_to_actor_mentions_map[gm])
    return [m.actor_name for m in candidate_actor_mentions]

def get_serif_list(args):
    serif_list = []
    for root, dirs, files in os.walk(args.input):
        for fname in files:
            fpath = os.path.join(root, fname)
            if fpath.endswith(".xml"):
                serif_list.append(fpath)
    return serif_list


def single_document_handler_stats(serif_path):

    d = Document(serif_path)
    mention_to_actor_mentions_map = build_mention_to_actor_mentions_map(d)

    stats = {"# total event mentions": sum([len(s.event_mention_set) for s in d.sentences]),
             "# event mentions with location argument": 0,
             "# event mentions with name location argument": 0,

             "# total mentions": sum([len(s.mention_set) for s in d.sentences]),
             "# non-name mentions": sum([len([m for m in s.mention_set if m.mention_type.value != "name"]) for s in d.sentences]),
             "# non-name mentions with resolvable link via entity coref": sum([len([m for m in s.mention_set
                                                                                    if (m.mention_type.value != "name" and len(resolve_link(m, mention_to_actor_mentions_map)) > 0)])
                                                                               for s in d.sentences]),

             "# name mentions": sum([len([m for m in s.mention_set if m.mention_type.value == "name"]) for s in d.sentences]),
             "# linked name mentions": 0,
             "# linked name mentions with 1 link": 0,
             "# linked name mentions with >1 link": 0,

             "# total mentions of type {GPE,LOC,FAC}": sum([len([m for m in s.mention_set if m.entity_type in {"GPE","LOC","FAC"}])
                                                            for s in d.sentences]),
             "# name mentions of type {GPE,LOC,FAC}": sum([len([m for m in s.mention_set if (m.entity_type in {"GPE","LOC","FAC"} and m.mention_type.value == "name")])
                                                           for s in d.sentences]),

             "# event argument mentions with location role": 0,
             "# event argument name mentions with location role": 0,
             "# linked event argument name mentions with location role": 0,
             "# linked event argument name mentions with location role with 1 geonames link": 0,
             "# linked event argument name mentions with location role with >1 geonames link": 0,

             "# event argument mentions of type {GPE,LOC,FAC} with location role": 0,
             "# event argument name mentions of type {GPE,LOC,FAC} with location role": 0,
             "# linked event argument name mentions of type {GPE,LOC,FAC} with location role": 0,
             "# linked event argument name mentions of type {GPE,LOC,FAC} with location role with 1 geonames link": 0,
             "# linked event argument name mentions of type {GPE,LOC,FAC} with location role with >1 geonames link": 0}

    stats["# linked name mentions"] = len(mention_to_actor_mentions_map.keys())
    stats["# linked name mentions with 1 link"] = len([m for m in mention_to_actor_mentions_map.keys()
                                                       if len(mention_to_actor_mentions_map[m]) == 1])
    stats["# linked name mentions with >1 link"] = len([m for m in mention_to_actor_mentions_map.keys()
                                                       if len(mention_to_actor_mentions_map[m]) > 1])

    # location event args
    for s in d.sentences:
        for em in s.event_mention_set:
            event_mention_has_location_argument = False
            event_mention_has_name_location_argument = False
            for a in em.arguments:
                if a.role in location_roles:
                    event_mention_has_location_argument = True
                    stats["# event argument mentions with location role"] += 1

                    if a.mention.mention_type.value == "name":
                        event_mention_has_name_location_argument = True
                        stats["# event argument name mentions with location role"] += 1
                        if a.mention in mention_to_actor_mentions_map:
                            assert len(mention_to_actor_mentions_map[a.mention]) > 0
                            stats["# linked event argument name mentions with location role"] += 1
                            if len(mention_to_actor_mentions_map[a.mention]) == 1:
                                stats["# linked event argument name mentions with location role with 1 geonames link"] += 1
                            elif len(mention_to_actor_mentions_map[a.mention]) > 1:
                                stats["# linked event argument name mentions with location role with >1 geonames link"] += 1

                    if a.mention.entity_type in {"GPE","LOC","FAC"}:
                        stats["# event argument mentions of type {GPE,LOC,FAC} with location role"] += 1
                        if a.mention.mention_type.value == "name":
                            stats["# event argument name mentions of type {GPE,LOC,FAC} with location role"] += 1
                            if a.mention in mention_to_actor_mentions_map:
                                assert len(mention_to_actor_mentions_map[a.mention]) > 0
                                stats["# linked event argument name mentions of type {GPE,LOC,FAC} with location role"] += 1
                                if len(mention_to_actor_mentions_map[a.mention]) == 1:
                                    stats["# linked event argument name mentions of type {GPE,LOC,FAC} with location role with 1 geonames link"] += 1
                                elif len(mention_to_actor_mentions_map[a.mention]) > 1:
                                    stats["# linked event argument name mentions of type {GPE,LOC,FAC} with location role with >1 geonames link"] += 1

            if event_mention_has_location_argument:
                stats["# event mentions with location argument"] += 1
            if event_mention_has_name_location_argument:
                stats["# event mentions with name location argument"] += 1

    return stats

def single_document_handler_location_string_and_matches(serif_path):

    d = Document(serif_path)
    mention_to_actor_mentions_map = build_mention_to_actor_mentions_map(d)

    location_argument_string_to_linked_canonical_names = defaultdict(list)
    location_argument_string_freq = defaultdict(int)

    # location event args
    for s in d.sentences:
        for em in s.event_mention_set:
            for a in em.arguments:
                if a.role in location_roles:

                    if a.mention.mention_type.value == "name":
                        if a.mention in mention_to_actor_mentions_map:
                            assert len(mention_to_actor_mentions_map[a.mention]) > 0

                            if a.mention.start_token is not None and a.mention.end_token is not None:
                                mention_text = a.mention.get_original_text_substring(a.mention.start_token.start_char,
                                                                                       a.mention.end_token.end_char)
                            else:
                                mention_text = a.mention.get_original_text_substring(a.mention.syn_node.start_char,
                                                                                       a.mention.syn_node.end_char)
                            mention_text = whitespace_re.sub(" ", mention_text).replace("\n", " ").replace("\t", " ")

                            location_argument_string_freq[mention_text] += 1
                            location_argument_string_to_linked_canonical_names[mention_text].extend([am.actor_name for am in mention_to_actor_mentions_map[a.mention]])

                    # if a.mention.entity_type in {"GPE","LOC","FAC"}:
                    #     if a.mention.mention_type.value == "name":
                    #         if a.mention in mention_to_actor_mentions_map:
                    #             assert len(mention_to_actor_mentions_map[a.mention]) > 0
    # ret = [(k, location_argument_string_freq[k], sorted(list(set(v)))) for k,v in location_argument_string_to_linked_canonical_names.items()]
    return dict(location_argument_string_to_linked_canonical_names), dict(location_argument_string_freq)

def main(args):

    if os.path.isfile(args.input):
        serif_list = [l.strip() for l in open(args.input, "r").readlines()]
    else: # os.path.isdir(args.input)
        serif_list = get_serif_list(args)

    STATS = defaultdict(int)
    LOCATION_ARGUMENT_STRING_TO_LINKED_CANONICAL_NAMES = defaultdict(list)
    LOCATION_ARGUMENT_STRING_FREQ = defaultdict(int)

    if args.mode == "stats":
        single_document_handler = single_document_handler_stats
    elif args.mode == "mention_string_to_canonical_names":
        single_document_handler = single_document_handler_location_string_and_matches

    manager = multiprocessing.Manager()
    with manager.Pool(multiprocessing.cpu_count()) as pool:

        workers = list()

        for serif_path in serif_list:
            workers.append(pool.apply_async(single_document_handler, args=(serif_path,)))

        for idx,i in enumerate(workers):
            i.wait()
            buf = i.get()

            if args.mode == "stats":
                for k,v in buf.items():
                    STATS[k] += v
            elif args.mode == "mention_string_to_canonical_names":
                for k,v in buf[0].items():
                    LOCATION_ARGUMENT_STRING_TO_LINKED_CANONICAL_NAMES[k].extend(v)
                for k,v in buf[1].items():
                    LOCATION_ARGUMENT_STRING_FREQ[k] += v

    if args.mode == "stats":
        for k,v in STATS.items():
            print(k, "\t\t", v)
    elif args.mode == "mention_string_to_canonical_names":

        pprint_list = []
        for k,v in LOCATION_ARGUMENT_STRING_TO_LINKED_CANONICAL_NAMES.items():
            pprint_list.append((k, sorted(list(set(v))), LOCATION_ARGUMENT_STRING_FREQ[k]))
        pprint_list = sorted(pprint_list, key=lambda x: x[-1], reverse=True)

        # with open(args.output, "w") as f:
        #     json.dump(LOCATION_ARGUMENT_STRING_TO_LINKED_CANONICAL_NAMES, f, indent=4, sort_keys=True, ensure_ascii=False)
        with open(args.output, "w") as f:
            f.write("\n".join(["\t\t\t".join([a, " ||| ".join(b), str(c)]) for (a, b, c) in pprint_list]))

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--input", help="serifxmls list or root directory", type=str)
    parser.add_argument("-o", "--output", help="output file", type=str)
    parser.add_argument("--mode", required=True, choices=["stats", "mention_string_to_canonical_names"], type=str)
    args = parser.parse_args()

    main(args)