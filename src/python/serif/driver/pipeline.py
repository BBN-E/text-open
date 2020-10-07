# Copyright 2019 by Raytheon BBN Technologies Corp.
# All Rights Reserved.
import os, sys, logging

logger = logging.getLogger(__name__)

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

import argparse
import importlib
import os
import sys

from serif import Document
import time

def get_docid_from_filename(filepath):
    basename = os.path.basename(filepath)
    if basename.endswith(".txt"):
        basename = basename[0:-4]
    return basename


def main(args):
    if not os.path.isdir(args.output_directory):
        os.makedirs(args.output_directory)

    files_to_process = []
    # Assume batch file
    i = open(args.input_file)
    for line in i:
        line = line.strip()
        if len(line) == 0 or line.startswith("#"):
            continue
        items = line.split("\t")
        files_to_process.append((items[0], items[1]))

    for idx, input_file in enumerate(files_to_process):
        file_type = input_file[0]
        input_file_path = input_file[1]
        logger.info("({}/{})Loading: {}".format(idx + 1, len(files_to_process), input_file_path))
        if file_type.lower() == "serifxml":
            # TODO: check if (1) serifxml is loaded in correctly, and (2) later stages won't fail
            document = Document(input_file_path)
        elif file_type.lower() == "sgm_arabic":
            document = Document.from_sgm(input_file_path, "Arabic")
        elif file_type.lower() == "sgm_english":
            document = Document.from_sgm(input_file_path, "English")
        elif file_type.lower() == "sgm":
            document = Document.from_sgm(input_file_path, "Unknown")
        elif file_type.lower() == "text_arabic":
            document = Document.from_text(input_file_path, "Arabic", get_docid_from_filename(input_file_path))
        elif file_type.lower() == "text_english":
            document = Document.from_text(input_file_path, "English", get_docid_from_filename(input_file_path))
        elif file_type.lower() == "text":
            document = Document.from_text(input_file_path, "Unknown", get_docid_from_filename(input_file_path))
        else:
            print("Bad filetype: " + file_type)
            sys.exit(1)

        logger.info('Processing: Begin {}'.format(document.docid))
        for model in args.models:
            
            try:
                logger.info('Processing: Applying {}'.format(type(model).__name__))
                ret = model.process(document)
                if isinstance(ret, Document):
                    document = ret
            except AssertionError as error:
                logger.exception(error)
            except Exception as exception:
                logger.exception(exception)

        logger.info('Writing SerifXML file {}'.format(document.docid))
        filename = document.docid + ".xml"
        output_file = os.path.join(args.output_directory, filename)
        document.save(output_file)
        
    
def read_config(arguments):
    """
    Adapted from HUME
    :param arguments:
    """
    models = []
    class_name_to_class = dict()
    implementations_path = ''

    model_types = [
        'BASE_MODEL',
        'JAVA_BASE_MODEL',
        'SENTENCE_SPLITTING_MODEL',
        'TOKENIZE_MODEL',
        'PART_OF_SPEECH_MODEL',
        'DEPENDENCY_MODEL',
        'PARSE_MODEL',
        'NAME_MODEL',
        'MENTION_MODEL',
        'RELATION_MENTION_MODEL',
        'EVENT_MENTION_MODEL',
        'ENTITY_MODEL',
        'RELATION_MODEL',
        'EVENT_MODEL',
        'EVENT_EVENT_RELATION_MENTION_MODEL'
    ]

    config_path = os.path.abspath(arguments.config)
    with open(config_path, 'r') as f:
        for line in f:
            line = line.strip()
            # comment or empty
            if len(line) == 0 or line.startswith('#'):
                continue

            # location of user-specified model implementations
            if line.startswith('IMPLEMENTATIONS'):
                implementations_path = os.path.realpath(next(f).strip())
                parent, filename = os.path.split(implementations_path)
                _, package_name = os.path.split(parent)
                module_name = os.path.splitext(filename)[0]
                # inserting at 1 allows implementation to import abstract class
                sys.path.insert(1, parent)
                try:
                    implementations = importlib.import_module(
                        module_name, package=package_name)
                    for cls_name in dir(implementations):
                        if cls_name not in class_name_to_class:
                            class_name_to_class[cls_name] = getattr(implementations,cls_name)
                except ImportError as e:
                    logger.critical('Implementations at {} could not be imported; '
                                    'make sure that the file exists and is a python '
                                    'module (sister to an __init__.py file).'
                                    .format(os.path.join(parent, filename)))
                    raise e
                continue
            if line.startswith('JAVA_CLASSPATH'):
                java_class_path = os.path.realpath(next(f).strip())
                l = set(os.environ.get("CLASSPATH","").split(":"))
                l.discard("")
                l.add(java_class_path)
                os.environ['CLASSPATH'] = ":".join(l)
                continue


            # Set up a model
            model_line = False
            is_barrier = False
            for model_type in model_types:
                if line.startswith(model_type + ' '):
                    model_line = True
                    model_line_split = line.split()
                    model_classname = model_line_split[1]

                    try:
                        _Model = class_name_to_class[model_classname]
                        # _Model = getattr(implementations, model_classname)
                    except KeyError as e:
                        logger.critical("No " + model_type + " subclass {} could be found at {}"
                                        .format(model_classname, implementations_path))
                        raise e
                    else:
                        models.append((_Model, {}))
                        break
            if model_line:
                continue

            last_model_kwargs = models[-1][1]
            kwarg_info = line.split()
            kwarg = kwarg_info[0]
            if len(kwarg_info) == 1:  # boolean
                last_model_kwargs[kwarg] = True
            elif len(kwarg_info) == 2:  # keyword and arg
                last_model_kwargs[kwarg_info[0]] = kwarg_info[1]
            else:
                raise IOError(
                    'Config file model {} parameter "{}" should consist of '
                    'the name of the keyword and up to one value; got '
                    'these values: {}'
                        .format(models[-1][0].__name__, line, kwarg_info[1:]))

    for model in models:
        for _, kwargs in models:
            kwargs['argparse'] = arguments

    arguments.models = [m(**kwargs) for m, kwargs in models]
    arguments.implementations = implementations

def parse(args_list):
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('config')
    arg_parser.add_argument('input_file')
    arg_parser.add_argument('output_directory')
    _args = arg_parser.parse_args(args_list)
    read_config(_args)
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
