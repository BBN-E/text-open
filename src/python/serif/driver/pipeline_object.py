import argparse
import importlib
import logging
import os
import sys

from serif.model.ingester import Ingester

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

class Pipeline():
    models = []
    ingester = None
    ready = False
    output_directory = None
    serif_docs = list()
    implementations = None
    logger = None
    processing = False
    
    def initialize(self, config_file, output_dir, logger=None):
        # this is to avoid passing args to create object
        try:
            self.logger = logger
            if self.logger is None:
                self.logger = logging.getLogger(__name__)
            self.output_directory = output_dir
            if not os.path.isdir(self.output_directory):
                os.makedirs(self.output_directory)
            self.apply_config(config_file)
            self.ingester = self.models.pop(0)
            for i, model in enumerate(self.models):
                self.logger.info('Loading resources for {}'.format(type(model).__name__))
                model.load_model()
            self.ready = True
        except Exception as e:
            # this could cause a problem if the exception relates to the logger...
            self.logger.error('Initialization error: {}'.format(e))
            raise e


    def process_file(self, file_to_process):
        try:
            self.processing = True
            self.logger.info("({}/{}) Ingesting: {}".format(1, 1, file_to_process))
            document = self.ingester.ingest(file_to_process)
            serif_docs = document
            
            for model in self.models:
                self.logger.info('Processing: Applying {}'.format(type(model).__name__))
                serif_docs = model.apply(serif_docs)
                # self.logger.info('Result: {}'.format(serif_docs))
            # the return is an array with the doc, so take the only entry    
            serif_doc = serif_docs.pop()
            self.logger.info('Writing SerifXML file {}'.format(serif_doc.docid))
            filename = serif_doc.docid + ".xml"
            output_file = os.path.join(self.output_directory, filename)
            serif_doc.save(output_file)
            serif_content = ''
            with open(output_file) as sfile:
                self.logger.info('opening file for serifxml content')        
                serif_content = sfile.read()
            self.logger.info('serif content: {} '.format(serif_content)) 
            self.processing = False
            return serif_content
        except Exception as e:
            self.logger.error('Processing error: {}'.format(e))
            raise e



    def release_resources(self):
        self.ready = False
        for model in self.models:
            self.logger.info('Releasing resources for {}'.format(type(model).__name__))
            model.unload_model()
    
    
    def apply_config(self, config_file):
        """
        Adapted from HUME
        :param arguments:
        """
        models = []
        class_name_to_class = dict()
        implementations_path = ''

        model_types = [
            'INGESTER',
            'BASE_MODEL',
            'JAVA_BASE_MODEL',
            'SENTENCE_SPLITTING_MODEL',
            'TOKENIZE_MODEL',
            'PART_OF_SPEECH_MODEL',
            'VALUE_MENTION_MODEL',
            'TIME_VALUE_MENTION_MODEL',
            'DEPENDENCY_MODEL',
            'PARSE_MODEL',
            'AMR_MODEL',
            'NAME_MODEL',
            'MENTION_MODEL',
            'ACTOR_MENTION_MODEL',
            'MENTION_COREF_MODEL',
            'EVENT_MENTION_COREF_MODEL',
            'RELATION_MENTION_MODEL',
            'EVENT_MENTION_MODEL',
            'ENTITY_MODEL',
            'VALUE_MODEL',
            'RELATION_MODEL',
            'EVENT_MODEL',
            'EVENT_EVENT_RELATION_MENTION_MODEL',
            'ACTOR_ENTITY_MODEL'
        ]

        config_path = os.path.abspath(config_file)
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
                                class_name_to_class[cls_name] = getattr(implementations, cls_name)
                    except ImportError as e:
                        self.logger.critical('Implementations at {} could not be imported; '
                                        'make sure that the file exists and is a python '
                                        'module (sister to an __init__.py file).'
                                        .format(os.path.join(parent, filename)))
                        raise e
                    continue
                if line.startswith('JAVA_CLASSPATH'):
                    java_class_path = os.path.realpath(next(f).strip())
                    l = set(os.environ.get("CLASSPATH", "").split(":"))
                    l.discard("")
                    l.add(java_class_path)
                    os.environ['CLASSPATH'] = ":".join(l)
                    continue

                # Set up a model
                model_line = False
                for model_type in model_types:
                    if line.startswith(model_type + ' '):
                        model_line = True
                        model_line_split = line.split()
                        model_classname = model_line_split[1]

                        if model_type == 'INGESTER' and len(models) > 0:
                            err_msg = '''Only one INGESTER should be specified and it must be 
                                the first model listed'''
                            self.logger.error(err_msg)
                            raise IOError(err_msg)
                        try:
                            _Model = class_name_to_class[model_classname]
                            # _Model = getattr(implementations, model_classname)
                        except KeyError as e:
                            self.logger.critical("No " + model_type + " subclass {} could be found at {}"
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
                    err_msg = '''Config file model {} parameter "{}" should consist of '
                    the name of the keyword and up to one value; got '
                    these values: {}'''.format(models[-1][0].__name__, line, kwarg_info[1:])
                    self.logger.error(err_msg)
                    raise IOError(err_msg)
        
        for model in models:
            for _, kwargs in models:
                kwargs['argparse'] = self
        
        self.models = [m(**kwargs) for m, kwargs in models]
        self.implementations = implementations
        
        if not isinstance(self.models[0], Ingester):
            err_msg = '''First model in config file must be an Ingester  
            for instance: "INGESTER SerifxmlIngester" or 
            or "INGESTER TextIngester" followed by any arguments'''
            self.logger.error(err_msg)
            raise IOError(err_msg)


