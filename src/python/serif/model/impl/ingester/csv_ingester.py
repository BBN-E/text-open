import os

from serif import Document
from serif.model.ingester import Ingester
from serif.theory.alert_author import ALERTAuthor
import csv
import sys

csv.field_size_limit(sys.maxsize)


class CSVIngester(Ingester):
    def __init__(self, lang, headers, csv_file=None, corpus=None, **kwargs):
        super(CSVIngester, self).__init__(**kwargs)
        self.headers = headers.split(",")
        self.language = lang
        self.corpus = corpus

    def ingest(self, filepath):
        docs = []
        docid = CSVIngester.get_docid_from_filename(filepath)

        with open(filepath) as f:
            attributes_reader = csv.reader((line.replace('\0','') for line in f))
            for i, row in enumerate(attributes_reader):
                assert len(row) == len(self.headers)
                if "docid" in self.headers:
                    cur_docid = row[self.headers.index("docid")]
                else:
                    cur_docid = docid + "_" + str(i)
                doc = Document.from_string(row[self.headers.index("text")], self.language, cur_docid)

                self.get_author_attributes_from_csv(row, doc)
                if self.corpus:
                    doc.alert_metadata.corpus = self.corpus

                docs.append(doc)
        return docs

    @staticmethod
    def get_docid_from_filename(filepath):
        basename = os.path.basename(filepath)
        if basename.endswith(".csv"):
            basename = basename[0:-4]
        return basename

    def get_author_attributes_from_csv(self, row, doc):
        doc.add_alert_metadata()
        author = doc.alert_metadata.add_new_author()

        for header, field in zip(self.headers, row):
            if hasattr(author, header):
                setattr(author, header, field)