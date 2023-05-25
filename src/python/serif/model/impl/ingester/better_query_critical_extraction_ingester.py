
import re,json,copy
import logging

from serif import Document
from serif.model.ingester import Ingester
from serif.io.bpjson.reader import Corpus,__FORMAT_VERSION__,__FORMAT_TYPE__

logger = logging.getLogger(__name__)


class BETTERQueryCriticalExtractionIngester(Ingester):
    date_re = re.compile("(\d\d\d\d)/(\d\d)/(\d\d)")

    def __init__(self,**kwargs):
        super(BETTERQueryCriticalExtractionIngester, self).__init__(**kwargs)

    def ingest(self, filepath):
        ret_documents = list()
        with open(filepath) as fp:
            query_dict = json.load(fp)
        docid_to_annotation = dict()
        for task in query_dict:
            task_docs = task['task-docs']
            task_num = task['task-num']
            for doc_uuid,annotation in task_docs.items():
                resolved_annotation = copy.deepcopy(annotation)
                doc_id_changed = "{}#{}#0".format(task_num,doc_uuid)
                resolved_annotation['doc-id'] = doc_id_changed
                resolved_annotation['entry-id'] = doc_id_changed
                docid_to_annotation[doc_id_changed] = resolved_annotation
            for request in task['requests']:
                req_num = request['req-num']
                req_docs = request['req-docs']
                for doc_uuid,annotation in req_docs.items():
                    resolved_annotation = copy.deepcopy(annotation)
                    doc_id_changed = "{}#{}#0".format(req_num, doc_uuid)
                    resolved_annotation['doc-id'] = doc_id_changed
                    resolved_annotation['entry-id'] = doc_id_changed
                    docid_to_annotation[doc_id_changed] = resolved_annotation
        corpus_obj = Corpus({
            "corpus-id": "From IR",
            "entries": docid_to_annotation,
            "format-type": __FORMAT_TYPE__,
            "format-version": __FORMAT_VERSION__
        })
        for segment in corpus_obj.segments:
            serif_doc = Document.from_string(segment.text, "english", segment.entry_id)
            serif_doc.bp_segment = segment
            ret_documents.append(serif_doc)
        return ret_documents