# Copyright 2019 by Raytheon BBN Technologies Corp.
# All Rights Reserved.
import os, sys, logging
logger = logging.getLogger(__name__)
import argparse
import json, codecs
import os
import sys

def main(args):
    if not os.path.isdir(args.output_directory):
        os.makedirs(args.output_directory)

    # 1. Read in target template and write in the 'data' field with the training_file_list_json
    target_template = args.target_template
    target_json_data = json.load(codecs.open(target_template, 'r', 'utf-8'))

    training_file_list_json = args.training_file_list_json
    training_file_list_json_data = json.load(codecs.open(training_file_list_json, 'r', 'utf-8'))
    data_entry = training_file_list_json_data['data']
    print('data_entry', data_entry)

    target_json_data['data'] = data_entry

    # 2. Write in the proper ontology file
    print('args.ontology_path', args.ontology_path)
    target_json_data['extractors'][0]['domain_ontology'] = args.ontology_path

    # 3. Dump the resulting file
    json.dump(target_json_data,open(args.output_directory + '/' + 'target.json','w'),indent=4, separators=(',', ': '))

def parse(args_list):
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('ontology_path')
    arg_parser.add_argument('training_file_list_json')
    arg_parser.add_argument('target_template')
    arg_parser.add_argument('output_directory')
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