import logging
import traceback
from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.runtime_control import ProductionLevel
from serif.theory.document import Document

logger = logging.getLogger(__name__)


class CorpusModel(BaseModel):
    def __init__(self, **kwargs):
        super(CorpusModel, self).__init__(**kwargs)

    @abstractmethod
    def process_documents(self, serif_docs):
        pass

    def begin_documents(self, serif_docs):
        pass

    def apply(self, serif_docs):
        """
        :type serif_docs: list(Document)
        :rtype list(Document)
        """
        logger.info("(all/{}) Applying {} to {}".format(len(serif_docs), type(self).__name__,
                                                        ",".join(i.docid for i in serif_docs)))
        resolved_docs = list()
        try:
            self.begin_documents(serif_docs)
            ret_docs = self.process_documents(serif_docs)  # note the plural "documents" here
            for ret_doc in ret_docs or ():
                if isinstance(ret_doc, Document):
                    resolved_docs.append(ret_doc)
        except Exception as e:
            logger.exception(
                "docs: {} model: {}".format(",".join(d.docid for d in serif_docs), type(self).__name__))
            logger.exception(traceback.format_exc())
            if hasattr(self, "argparse") and self.argparse is not None and self.argparse.PRODUCTION_MODE is False:
                raise e
            if hasattr(self,
                       "pyserif_pipeline_config_ins") and self.pyserif_pipeline_config_ins.production_level < ProductionLevel.EXTERNAL_USAGE:
                raise e
        if len(resolved_docs) > 0:
            return resolved_docs
        else:
            return serif_docs

    def reload_model(self):
        pass
