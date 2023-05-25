import os

from serif import Document
from serif.model.ingester import Ingester


class SgmIngester(Ingester):
    def __init__(self, lang, **kwargs):
        super(SgmIngester, self).__init__(**kwargs)
        self.language = lang

    def ingest(self, filepath):
        doc = Document.from_sgm(filepath, self.language)
        docid = SgmIngester.get_docid_from_filename(filepath)
        if self.docid_to_dct is not None and docid in self.docid_to_dct:
            doc.document_time_start, doc.document_time_end = self.docid_to_dct[docid]
        return [doc]

    @staticmethod
    def get_docid_from_filename(filepath):
        basename = os.path.basename(filepath)
        if basename.endswith(".sgm"):
            basename = basename[0:-4]
        return basename

