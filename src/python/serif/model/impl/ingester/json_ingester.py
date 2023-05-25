import os

from serif import Document
from serif.model.ingester import Ingester
from serif.theory.alert_author import ALERTAuthor
import json

class JSONIngester(Ingester):
    def __init__(self, lang, headers, corpus=None, **kwargs):
        super(JSONIngester, self).__init__(**kwargs)
        self.alert_to_corpora_heading = {}
        self.corpora_to_alert_heading = {}
        for header_map in headers.split(","):
            corpora_heading, alert_metadata = header_map.split(":")
            self.alert_to_corpora_heading[alert_metadata] = corpora_heading
            self.corpora_to_alert_heading[corpora_heading] = alert_metadata
        self.language = lang
        self.corpus = corpus

    def ingest(self, filepath):
        docs = []
        docid = JSONIngester.get_docid_from_filename(filepath)

        with open(filepath) as f:
            json_list = list(f)

        for i, str_entry in enumerate(json_list):
            entry = json.loads(str_entry)

            if "docid" in self.alert_to_corpora_heading:
                cur_docid = entry[self.alert_to_corpora_heading["docid"]]
            else:
                cur_docid = docid + "_" + str(i)
            if self.alert_to_corpora_heading["text"] not in entry:
                continue

            text = entry[self.alert_to_corpora_heading["text"]]
            if "title_text" in self.alert_to_corpora_heading:
                if self.alert_to_corpora_heading["title_text"] in entry:
                    text = entry[self.alert_to_corpora_heading["title_text"]] + '\n' + text

            doc = Document.from_string(text, self.language, cur_docid)

            self.get_author_attributes_from_json(entry, doc)
            if self.corpus:
                doc.alert_metadata.corpus = self.corpus

            docs.append(doc)
        return docs

    @staticmethod
    def get_docid_from_filename(filepath):
        basename = os.path.basename(filepath)
        if basename.endswith(".json"):
            basename = basename[0:-5]
        elif basename.endswith(".jsonl"):
            basename = basename[0:-6]
        return basename

    def get_author_attributes_from_json(self, entry, doc):
        doc.add_alert_metadata()
        author = doc.alert_metadata.add_new_author()

        for corpora_header, value in entry.items():
            if not corpora_header in self.corpora_to_alert_heading:
                continue
            alert_header = self.corpora_to_alert_heading[corpora_header]
            if hasattr(author, alert_header):
                setattr(author, alert_header, value)
