# Copyright 2019 by Raytheon BBN Technologies Corp.
# All Rights Reserved.
import logging
logger = logging.getLogger(__name__)
import argparse
import os
import sys
import pickle

def load_map(relation_pattern_map_file, relation_count_map):
    """
    Aggregate relation count maps.
    """
    with open(relation_pattern_map_file, 'rb') as f:
        mode_map = pickle.load(f)

    for relation in mode_map:
        if relation not in relation_count_map:
            relation_count_map[relation] = 0
        relation_count_map[relation] += mode_map[relation]

def main(args):
    """
    Aggregate relation count maps.
    """
    i = open(args.label_file)
    relation_count_map = dict()
    for line in i:
        line = line.strip()
        if len(line) == 0 or line.startswith("#"):
            continue
        load_map(line, relation_count_map)
    logging.info('relation_count_map: %s', relation_count_map)

    na_count = 0
    non_na_count = 0

    for relation in relation_count_map:
        if 'Negative' in relation:
            na_count += relation_count_map[relation]
        else:
            non_na_count += relation_count_map[relation]

    logging.info('non_na_count: %s', non_na_count)
    logging.info('na_count: %s', na_count)

def parse(args_list):
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('label_file')
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