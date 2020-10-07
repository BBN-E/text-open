# Copyright 2019 by Raytheon BBN Technologies Corp.
# All Rights Reserved.
import os, sys, logging
logger = logging.getLogger(__name__)
import argparse
import json, codecs
import os
import sys
import random
import pickle
from collections import Counter

def load_map(relation_pattern_map_file, final_map, unique_ids):
    """
    Add relation_pattern docids from relation_pattern_map_file to the final_map.
    TODO: fix unique_ids hack by ingesting the entire document list...
    :param json_file:
    :param final_json_objs:
    :return:
    """

    with open(relation_pattern_map_file, 'rb') as f:
        mode_map = pickle.load(f)

    for relation in mode_map:
        if relation not in final_map:
            final_map[relation] = dict()
        for pattern in mode_map[relation]:
            if pattern not in final_map[relation]:
                final_map[relation][pattern] = []
            final_map[relation][pattern].extend(mode_map[relation][pattern])
            for docid in mode_map[relation][pattern]:
                unique_ids.add(docid)

HUMAN_LABEL = 'HumanLabeled'

def distribute(num_na, num_docs):
    base, extra = divmod(num_na, num_docs)
    return [base + (i < extra) for i in range(num_docs)]

def main(args):
    if not os.path.isdir(args.output_directory):
        os.makedirs(args.output_directory)

    """
    Aggregate relation pattern docid maps.  
    """
    i = open(args.label_file)
    final_relation_pattern_map = dict()
    unique_ids = set()
    for line in i:
        line = line.strip()
        if len(line) == 0 or line.startswith("#"):
            continue
        load_map(line, final_relation_pattern_map, unique_ids)
    # print('final_relation_pattern_map', final_relation_pattern_map)

    """
    Sample relations according to learnit_pattern_bound and serialize to disk
    """
    sampled_map = dict()
    examples_per_learnit_pattern = int(args.learnit_pattern_bound)
    for relation in final_relation_pattern_map:
        if relation not in sampled_map:
            sampled_map[relation] = dict()
        for pattern in final_relation_pattern_map[relation]:
            if pattern not in sampled_map[relation]:
                sampled_map[relation][pattern] = []
            if HUMAN_LABEL in pattern:
                sampled_map[relation][pattern].extend(final_relation_pattern_map[relation][pattern])
            else:
                # print(final_relation_pattern_map[relation][pattern])
                num_examples = min(len(final_relation_pattern_map[relation][pattern]), examples_per_learnit_pattern)
                sampled_map[relation][pattern] = random.sample(final_relation_pattern_map[relation][pattern], num_examples)

    """
    Calculate number of NA examples needed for each docid and serialize to disk
    """
    na_count = 0
    non_na_count = 0

    serialized_sample_map = dict()
    relation_count_map = dict()
    for relation in sampled_map:
        # print('relation', relation)
        for pattern in sampled_map[relation]:
            # print('pattern', pattern)
            # assert(HUMAN_LABEL in pattern)
            samples = sampled_map[relation][pattern]
            sample_set = set(samples)
            counter = Counter(samples)
            for docid in sample_set:
                if docid not in serialized_sample_map:
                    serialized_sample_map[docid] = dict()
                if relation not in serialized_sample_map[docid]:
                    serialized_sample_map[docid][relation] = dict()
                if pattern not in serialized_sample_map[docid][relation]:
                    serialized_sample_map[docid][relation][pattern] = counter[docid]
            if 'Negative' in relation:
                na_count += len(samples)
            else:
                non_na_count += len(samples)

            if relation not in relation_count_map:
                relation_count_map[relation] = dict()
            if pattern not in relation_count_map[relation]:
                relation_count_map[relation][pattern] = 0
            relation_count_map[relation][pattern] += len(samples)

    na_to_non_na_ratio = int(args.na_to_non_na_ratio)
    print('na_to_non_na_ratio', na_to_non_na_ratio)
    num_na_to_add = (non_na_count * na_to_non_na_ratio) - na_count
    print('non_na_count', non_na_count)
    print('num_na_to_add', num_na_to_add)

    print('relation_count_map', relation_count_map)

    na_partitions = distribute(num_na_to_add, len(unique_ids))
    sorted_ids = sorted(list(unique_ids))

    docid_to_na_map = dict()
    for idx, id in enumerate(sorted_ids):
        docid_to_na_map[id] = na_partitions[idx]

    with open(args.output_directory + '/docid_to_na_count.map', 'wb') as f:
        pickle.dump(docid_to_na_map, f)

    sampled_map_file = args.output_directory + '/' + 'sampled_map.pkl'
    with open(sampled_map_file, 'wb') as f:
        pickle.dump(serialized_sample_map, f)

def parse(args_list):
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('label_file')
    arg_parser.add_argument('output_directory')
    arg_parser.add_argument('learnit_pattern_bound')
    arg_parser.add_argument('na_to_non_na_ratio')
    _args = arg_parser.parse_args(args_list)
    return _args

if __name__ == '__main__':
    log_format = '[%(asctime)s] {P%(process)d:%(module)s:%(lineno)d} %(levelname)s - %(message)s'
    try:
        logging.basicConfig(level=logging.getLevelName(os.environ.get('LOGLEVEL', 'INFO').upper()),
                            format=log_format)
    except ValueError as e:
        logging.error(
            "Unparseable level {}, will use default {}.".format(os.environ.get('LOGLEVEL', 'INFO').upper(),
                                                                logging.root.level))
        logging.basicConfig(format=log_format)
    main(parse(sys.argv[1:]))