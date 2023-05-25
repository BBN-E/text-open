import os

from serif import Document
from serif.model.ingester import Ingester
from serif.theory.alert_author import ALERTAuthor
import json
import sys


class PANIngester(Ingester):
    def __init__(self, lang, truth_filepath, corpus=None, **kwargs):
        super(PANIngester, self).__init__(**kwargs)
        self.language = lang
        self.author_filepath = truth_filepath
        self.corpus = corpus

    def create_document(self, docid, document_text, author_id):
        doc = Document.from_string(document_text, self.language, docid)
        doc.add_alert_metadata()
        doc.alert_metadata.corpus = self.corpus
        author = doc.alert_metadata.add_new_author()
        author.author_id = author_id
        return doc

    def ingest_pan_2021(self, filepath):
        with open(filepath) as f1:
            documents_json = list(f1)
        with open(self.author_filepath) as f2:
            authors_json = list(f2)

        docs = []
        for document_pair_json, author_pair_json in zip(documents_json, authors_json):
            document_info = json.loads(document_pair_json)
            document_id = document_info["id"]
            document_pair = document_info["pair"]
            author_pair = json.loads(author_pair_json)["authors"]

            doc0 = self.create_document(document_id + "_0", document_pair[0], author_pair[0])
            doc1 = self.create_document(document_id + "_1", document_pair[1], author_pair[1])

            docs.append(doc0)
            docs.append(doc1)

        return docs

    def ingest(self, filepath):

        docs = self.ingest_pan_2021(filepath)
        return docs


