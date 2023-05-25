import json
import logging
import re

from serif import Document
from serif.model.ingester import Ingester

logger = logging.getLogger(__name__)


def correct_control_characters(original_text):
    string = original_text.replace("\\r\\n", " \r\n ").replace("\\r", "\r ").replace("\\n", "\n ").replace("\\t", "\t ")
    string = re.sub(r'\\x\w\w', '    ', string)
    string = re.sub(r'\\\w([^\w])', '\1  ', string)
    string = re.sub(r'\\u[abcdef\d][abcdef\d][abcdef\d][abcdef\d]', '      ', string)
    string = re.sub(r"\\'", " '", string)
    return string


class BETTERQueryRelevantDocIngester(Ingester):
    date_re = re.compile("(\d\d\d\d)/(\d\d)/(\d\d)")

    def __init__(self, **kwargs):
        super(BETTERQueryRelevantDocIngester, self).__init__(**kwargs)

    def ingest(self, filepath):
        ret_documents = list()
        entries = json.loads(filepath)
        corpus_jsonl_path = entries['corpus_jsonl_path']
        task_json_path = entries['task_json_path']
        with open(task_json_path) as fp:
            query_dict = json.load(fp)
        care_doc_ids = set()
        for task in query_dict:
            task_docs = task['task-docs']
            care_doc_ids.update(task_docs)
            for request in task['requests']:
                req_docs = request['req-docs']
                care_doc_ids.update(req_docs)
        with open(corpus_jsonl_path, 'r') as fp:
            for i in fp:
                json_doc = json.loads(i.strip())
                doc_id = json_doc["derived-metadata"]["id"]
                full_text = json_doc["derived-metadata"]["text"]
                full_text = correct_control_characters(full_text)
                language = json_doc["derived-metadata"]["language"]
                guessed_publish_date = json_doc["derived-metadata"]["guess-publish-date"]

                if doc_id in care_doc_ids:
                    serif_doc = Document.from_string(full_text, language, doc_id)
                    m = BETTERQueryRelevantDocIngester.date_re.match(guessed_publish_date)
                    if m is not None:
                        year = m.group(1)
                        month = m.group(2)
                        day = m.group(3)
                        date_string = year + "-" + month + "-" + day
                        serif_doc.document_time_start = date_string + "T23:59:59"
                        serif_doc.document_time_end = date_string + "T00:00:00"
                    ret_documents.append(serif_doc)
        return ret_documents
