# Copyright 2019 by Raytheon BBN Technologies Corp.
# All Rights Reserved.
import os, sys, logging
logger = logging.getLogger(__name__)
import argparse
import json
import os
import sys
import random

def main(args):
    """
    Emits a json that contains a set of Serif file lists, in nlplingo format, that are split according to a given train_ratio.
    train_ratio of 0.8 means 0.8 train / 0.1 dev / 0.1 test
    """
    if not os.path.isdir(args.output_directory):
        os.makedirs(args.output_directory)

    train_ratio = float(args.train_ratio)
    logging.info('train_ratio: %s', train_ratio)
    val_ratio = (1 - train_ratio) / (2.0)

    train_examples = []
    val_examples = []
    test_examples = []

    """
    Construct final Serif lists
    """
    i = open(args.serif_files)

    filenames = []
    for line in i:
        line = line.strip()
        if len(line) == 0 or line.startswith("#"):
            continue
        filenames.append('SERIF:' + line)

    random.shuffle(filenames)
    num_files = len(filenames)
    num_train = int(num_files * train_ratio)
    num_val = int(num_files * val_ratio)
    num_test = num_files - (num_train + num_val)
    logging.info('num_train: %s', num_train)
    logging.info('num_val: %s', num_val)
    logging.info('num_test: %s', num_test)
    train_examples.extend(filenames[:num_train])
    val_examples.extend(filenames[num_train:num_train + num_val])
    test_examples.extend(filenames[num_train + num_val:])

    train_list = args.output_directory + '/train.list'
    dev_list = args.output_directory + '/dev.list'
    test_list = args.output_directory + '/test.list'

    with open(train_list, 'w') as fp:
        for example in train_examples:
            fp.write(str(example) + "\n")

    with open(dev_list, 'w') as fp:
        for example in val_examples:
            fp.write(str(example) + "\n")

    with open(test_list, 'w') as fp:
        for example in test_examples:
            fp.write(str(example) + "\n")

    filelist_data = dict()
    filelist_dict = dict()
    filelist_dict['train'] = dict()
    filelist_dict['dev'] = dict()
    filelist_dict['test'] = dict()

    filelist_dict['train']['filelist'] = train_list
    filelist_dict['dev']['filelist'] = dev_list
    filelist_dict['test']['filelist'] = test_list

    filelist_data['data'] = filelist_dict

    json.dump(filelist_data,open(args.output_directory + '/train_dev_test.json','w'),indent=4, separators=(',', ': '))

def parse(args_list):
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('serif_files')
    arg_parser.add_argument('output_directory')
    arg_parser.add_argument('train_ratio')
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