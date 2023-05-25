import os

from serif import Document
from serif.model.ingester import Ingester
from pycube.utils import json_segment

def get_ordered_segment_text(fieldname, segments):
    return [
        x[fieldname]
        for x in sorted(
            segments,
            key=lambda x: int(x['SEGMENT_INDEX'])
        )
    ]

class BBNMTJSONIngester(Ingester):
    def __init__(self, lang, extraction_field, **kwargs):
        super(BBNMTJSONIngester, self).__init__(**kwargs)
        self.lang = lang
        self.extraction_field = extraction_field # SERIF_TOKENIZED_SOURCE,SERIF_TOKENIZED_MT
        self.docid_suffix = kwargs.get('docid_suffix', '')
        self.doc_id_field_name = kwargs.get('doc_id_field_name','DOCUMENT_ID')

    def ingest(self, filepath):
        docid_to_segments = dict()
        documents = list()
        for segment in json_segment.read_json_from_files([filepath]):
            docid = segment[self.doc_id_field_name] + self.docid_suffix
            docid_to_segments.setdefault(docid,list()).append(segment)
        for docid, segments in docid_to_segments.items():
            lines_of_text = get_ordered_segment_text(self.extraction_field, segments)
            serif_doc = Document.from_string("\n".join(lines_of_text), self.lang, docid)
            documents.append(serif_doc)
        return documents