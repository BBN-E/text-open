import os

from serif import Document
from serif.model.ingester import Ingester


class TextIngester(Ingester):
    def __init__(self, lang, **kwargs):
        super(TextIngester, self).__init__(**kwargs)
        self.language = lang

    def ingest(self, filepath):
        docid = TextIngester.get_docid_from_filename(filepath)
        doc = Document.from_text(filepath, self.language, docid)
        if self.docid_to_dct is not None and docid in self.docid_to_dct:
            doc.document_time_start, doc.document_time_end = self.docid_to_dct[docid]
        return [doc]

    @staticmethod
    def get_docid_from_filename(filepath):
        basename = os.path.basename(filepath)
        if basename.endswith(".txt"):
            basename = basename[0:-4]
        return basename

