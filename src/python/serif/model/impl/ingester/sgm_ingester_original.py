import os

from serif import Document
from serif.model.ingester import Ingester


class SgmIngester(Ingester):
    def __init__(self, lang, **kwargs):
        super(SgmIngester, self).__init__(**kwargs)
        self.language = lang

    def ingest(self, filepath):
        return [Document.from_sgm(filepath, self.language)]

