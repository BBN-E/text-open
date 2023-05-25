import os, re

from serif import Document
from serif.model.ingester import Ingester

class BrandeisArticle(object):
    def __init__(self):
        self.text = ""
        self.doc_id = None
        self.event_spans = set()

class BrandeisCorpus(Ingester):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)

    def ingest(self, filepath):
        doc_id_re = re.compile(r"<doc id=(.+)>")
        current_article_object = None
        current_serif_object = None
        ret = list()
        with open(filepath) as fp:
            in_text = False
            in_edge_list = False
            doc_id_str = None
            for i in fp:
                stripped = i.strip()
                if "filename" in stripped and "SNT_LIST" in stripped:
                    if current_serif_object is not None:
                        current_serif_object.brandeis_article = current_article_object
                        ret.append(current_serif_object)
                    current_serif_object = None
                    current_article_object = BrandeisArticle()
                    _,_,_,doc_id_str,_ = stripped.split(":")
                    doc_id = doc_id_re.findall(doc_id_str)[0]
                    current_article_object.doc_id = doc_id
                    in_text = True
                elif stripped != "EDGE_LIST" and in_text is True:
                    current_article_object.text += i
                elif stripped == "EDGE_LIST":
                    current_serif_object = Document.from_string(current_article_object.text, "English", doc_id)
                    in_text = False
                    in_edge_list = True
                elif in_edge_list is True:
                    if "\t" in stripped:
                        span_pair, role = stripped.split("\t")
                        if role == "Event":
                            sent_idx,start_token_idx,end_token_idx = span_pair.split("_")
                            sent_idx = int(sent_idx)
                            start_token_idx = int(start_token_idx)
                            end_token_idx = int(end_token_idx)
                            current_article_object.event_spans.add((sent_idx,start_token_idx,end_token_idx))
                elif len(stripped) < 1:
                    continue
                else:
                    raise NotImplementedError("You should not reach here")

        if current_serif_object is not None:
            current_serif_object.brandeis_article = current_article_object
            ret.append(current_serif_object)
        return ret
