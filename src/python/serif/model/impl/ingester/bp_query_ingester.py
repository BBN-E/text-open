import os,json
import logging

from serif import Document
from serif.model.ingester import Ingester

logger = logging.getLogger(__name__)


class BpQueryIngester(Ingester):
    def __init__(self, lang, **kwargs):
        super(BpQueryIngester, self).__init__(**kwargs)
        self.lang = lang


    def create_serif_doc_from_str(self,raw_sentences,docid):
        serif_doc = Document.from_string(" ".join(raw_sentences), self.lang, docid)
        # Load and create sentences
        sentences = serif_doc.add_new_sentences()
        region = serif_doc.regions[0]
        offs_so_far = 0
        for sentence in raw_sentences:
            if len(sentence) > 0:
                sentences.add_new_sentence(
                    start_char=offs_so_far,
                    end_char=offs_so_far+max(len(sentence)-1, 0),
                    region=region
                )
            offs_so_far = offs_so_far+len(sentence)+1 # We're using ` ` for joining string
        return serif_doc


    def ingest(self, filepath):
        with open(filepath) as fp:
            query_dict = json.load(fp)
        documents = list()
        for task in query_dict:
            task_num = task['task-num']

            if "task-stmt" in task:
                serif_doc = self.create_serif_doc_from_str([task['task-stmt']], "{}-{}".format(task_num,'task-stmt'))
                documents.append(serif_doc)

            if "task-title" in task:
                serif_doc = self.create_serif_doc_from_str([task['task-title']], "{}-{}".format(task_num, 'task-title'))
                documents.append(serif_doc)

            if "task-narr" in task:
                serif_doc = self.create_serif_doc_from_str([task['task-narr']], "{}-{}".format(task_num, 'task-narr'))
                documents.append(serif_doc)

            if "task-in-scope" in task:
                serif_doc = self.create_serif_doc_from_str([task['task-in-scope']], "{}-{}".format(task_num, 'task-in-scope'))
                documents.append(serif_doc)

            if "task-not-in-scope" in task:
                serif_doc = self.create_serif_doc_from_str([task['task-not-in-scope']], "{}-{}".format(task_num, 'task-not-in-scope'))
                documents.append(serif_doc)

            serif_doc = self.create_serif_doc_from_str([task_num], "{}-{}".format(task_num, 'task-num'))
            documents.append(serif_doc)

            for request in task['requests']:
                req_num = request['req-num']

                if "req-text" in request:
                    serif_doc = self.create_serif_doc_from_str([request['req-text']],"{}-{}".format(req_num,'req-text'))
                    documents.append(serif_doc)

                serif_doc = self.create_serif_doc_from_str([req_num],"{}-{}".format(req_num,'req-num'))
                documents.append(serif_doc)

        return documents


if __name__ == "__main__":
    ingester = BpQueryIngester('english')
    f = "/d4m/better/data/ir_docker_full_121720/turkey-run-hitl-tasks.json"
    documents = ingester.ingest(f)
    output = "/home/hqiu/tmp/out3"
    for d in documents:
        d.save(os.path.join(output,"{}.xml".format(d.docid)))
        d_modified = Document(os.path.join(output,"{}.xml".format(d.docid)))