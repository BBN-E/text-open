import json

from serif import Document
from serif.model.ingester import Ingester


class MSMARCOIngester(Ingester):
    def __init__(self, **kwargs):
        super(MSMARCOIngester, self).__init__(**kwargs)

    def ingest(self, json_l_path):
        ret = list()
        with open(json_l_path) as fp:
            for i in fp:
                i = i.strip()
                doc_en = json.loads(i)
                guid = doc_en['guid']
                question, positive_answer, negative_answer = doc_en['texts']
                question_serif_doc = Document.from_string(question, "English", "{}_question".format(guid))
                positive_answer_serif_doc = Document.from_string(positive_answer, "English", "{}_pos_ans".format(guid))
                negative_answer_serif_doc = Document.from_string(negative_answer, "English", "{}_neg_ans".format(guid))
                ret.append(question_serif_doc)
                ret.append(positive_answer_serif_doc)
                ret.append(negative_answer_serif_doc)
        return ret
