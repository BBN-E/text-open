"""
This is for providing a flexible lifecycle hooks for implement model persistence as well as potential online decoding.
"""

import dataclasses
import importlib
import logging
import os
import sys
import typing

logger = logging.getLogger(__name__)
current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

from serifxml3 import Document
from serif.model.base_model import BaseModel
from serif.model.ingester import Ingester
from serif.util.file_utils import fopen
from serif.runtime_control import ProductionLevel


@dataclasses.dataclass
class PySerifPipelineConfig(object):
    models: typing.List[typing.Union[BaseModel, Ingester]]
    model_name_to_cls: typing.Dict[str, typing.Union[BaseModel, Ingester]]
    production_level: ProductionLevel
    input_list_path: typing.Optional[str]
    output_dir: typing.Optional[str]
    output_prefix: str
    output_suffix: str

    def __init__(self, models, model_name_to_cls):
        self.models = models
        self.model_name_to_cls = model_name_to_cls
        self.production_level = ProductionLevel[os.environ.get("PRODUCTION_LEVEL", "DEV")]
        self.output_prefix = ""
        self.output_suffix = ".xml.gz"
        """
        There could be more attribute bind to this class for downstream usage
        """

    @staticmethod
    def from_config_dict(config_dict):
        for java_classpath in config_dict.get("class_loader_paths", dict()).get("java"):
            l = list(os.environ.get("CLASSPATH", "").split(":"))
            l = list(filter(lambda a: a != "", l))
            l = [java_classpath] + l
            os.environ['CLASSPATH'] = ":".join(l)
        model_name_to_cls = dict()
        for implementations_path in config_dict.get("class_loader_paths", dict()).get("python"):
            parent, filename = os.path.split(implementations_path)
            _, package_name = os.path.split(parent)
            module_name = os.path.splitext(filename)[0]
            # inserting at 1 allows implementation to import abstract class
            sys.path.insert(1, parent)
            implementations = importlib.import_module(
                module_name, package=package_name)
            for cls_name in dir(implementations):
                if cls_name not in model_name_to_cls:
                    model_name_to_cls[cls_name] = getattr(implementations, cls_name)
                else:
                    logger.warning(
                        "{} has been imported. Ignoring {}.{}".format(cls_name, implementations_path, cls_name))
        models = list()
        pyserif_pipeline_config_ins = PySerifPipelineConfig(models=models, model_name_to_cls=model_name_to_cls)
        for model_config in config_dict.get("models", ()):
            model_name = model_config["model_name"]
            kwargs = model_config["kwargs"]
            model_ins = model_name_to_cls[model_name](**kwargs)
            model_ins.pyserif_pipeline_config_ins = pyserif_pipeline_config_ins
            models.append(model_ins)
        pyserif_pipeline_config_ins.output_prefix = ""
        pyserif_pipeline_config_ins.output_suffix = ".xml.gz"
        if "batch_processing_config" in config_dict:
            pyserif_pipeline_config_ins.output_prefix = config_dict["batch_processing_config"].get("output_prefix",
                                                                                                   pyserif_pipeline_config_ins.output_prefix)
            pyserif_pipeline_config_ins.output_suffix = config_dict["batch_processing_config"].get("output_suffix",
                                                                                                   pyserif_pipeline_config_ins.output_suffix)
        log_format = '[%(asctime)s] {P%(process)d:%(module)s:%(lineno)d} %(levelname)s - %(message)s'
        log_level = "INFO"
        if "logging_config" in config_dict:
            logger_config_dict = config_dict["logging_config"]
            log_format = logger_config_dict.get("log_format", log_format)
            log_level = logger_config_dict.get("log_level", log_level)
        logging.basicConfig(level=logging.getLevelName(log_level.upper()),
                            format=log_format)
        return pyserif_pipeline_config_ins


@dataclasses.dataclass
class PySerifPipeline(object):
    _pyserif_pipeline_config: PySerifPipelineConfig

    def __init__(self, pyserif_pipeline_config):
        self._pyserif_pipeline_config = pyserif_pipeline_config

    @property
    def pyserif_pipeline_config(self):
        return self._pyserif_pipeline_config

    def load_models(self):
        for model in self._pyserif_pipeline_config.models:
            model.load_model()

    def unload_models(self):
        for model in self._pyserif_pipeline_config.models:
            model.unload_model()

    @staticmethod
    def from_config_dict(config_dict):
        pyserif_pipeline_config_ins = PySerifPipelineConfig.from_config_dict(config_dict)
        pyserif_pipeline_ins = PySerifPipeline(pyserif_pipeline_config_ins)
        return pyserif_pipeline_ins

    def process_serif_docs(self, serif_docs):
        for i, model in enumerate(self._pyserif_pipeline_config.models):
            logger.info('Processing: Applying {}'.format(type(model).__name__))
            serif_docs = model.apply(serif_docs)
        return serif_docs

    def process_txts(self, doc_id_to_txt, lang):
        serif_docs = list()
        for doc_id, text in doc_id_to_txt.items():
            serif_doc = Document.from_string(text, lang, doc_id)
            serif_docs.append(serif_doc)
        return self.process_serif_docs(serif_docs)

    def batch_processing(self):
        logger.info("ProductionLevel: {}".format(self._pyserif_pipeline_config.production_level.name))
        line_to_process = list()
        os.makedirs(self._pyserif_pipeline_config.output_dir, exist_ok=True)
        with fopen(self._pyserif_pipeline_config.input_list_path) as fp:
            for i in fp:
                i = i.strip()
                line_to_process.append(i)
        serif_docs = list()
        for idx, input_file_path in enumerate(line_to_process):
            logger.info("({}/{}) Ingesting: {}".format(idx + 1, len(line_to_process), input_file_path))
            ingester = self._pyserif_pipeline_config.models[0]
            documents = ingester.ingest(input_file_path)
            serif_docs.extend(documents)

        for i, model in enumerate(self._pyserif_pipeline_config.models[1:]):
            logger.info('Loading resources for {}'.format(type(model).__name__))
            model.load_model()
            logger.info('Processing: Applying {}'.format(type(model).__name__))
            serif_docs = model.apply(serif_docs)
            logger.info('Releasing resources for {}'.format(type(model).__name__))
            model.unload_model()
        for serif_doc in serif_docs:
            logger.info('Writing SerifXML file {}'.format(serif_doc.docid))
            filename = self._pyserif_pipeline_config.output_prefix + serif_doc.docid + self._pyserif_pipeline_config.output_suffix
            output_file = os.path.join(self._pyserif_pipeline_config.output_dir, filename)
            serif_doc.save(output_file)


def batch_processing_main():
    import yaml
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("config_file_path")
    parser.add_argument("input_list_path")
    parser.add_argument("output_dir")
    args = parser.parse_args()
    with open(args.config_file_path) as fp:
        config_dict = yaml.full_load(fp)
    pyserif_pipeline = PySerifPipeline.from_config_dict(config_dict)
    pyserif_pipeline.pyserif_pipeline_config.input_list_path = args.input_list_path
    pyserif_pipeline.pyserif_pipeline_config.output_dir = args.output_dir
    pyserif_pipeline.batch_processing()


if __name__ == "__main__":
    batch_processing_main()
