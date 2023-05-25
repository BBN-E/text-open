import serifxml3

from serif import Document
from serif.model.ingester import Ingester

class RoundTripperIngester(Ingester):
    def __init__(self, **kwargs):
        super(RoundTripperIngester, self).__init__(**kwargs)

    # Overrides Ingester.ingest
    # An Ingester is a special kind of pyserif model which
    # takes in a file in some format and returns a Document object.
    # non-Ingester models takes in a Document object and
    # adds to it.
    def ingest(self, filepath):
        input_serifxml_document = serifxml3.Document(filepath)
        # Assumes one region and at most one metadata span in input document
        region = input_serifxml_document.regions[0]
        span = None
        if len(input_serifxml_document.metadata) > 0:
            span = input_serifxml_document.metadata[0]

        original_text = input_serifxml_document.original_text.text
        language = input_serifxml_document.language
        docid = input_serifxml_document.docid

        new_document = RoundTripperIngester.construct_new_document(original_text, language, docid,
                                                                   source_type=input_serifxml_document.source_type,
                                                                   is_downcased=input_serifxml_document.is_downcased,
                                                                   time_start=input_serifxml_document.document_time_start,
                                                                   time_end=input_serifxml_document.document_time_end,
                                                                   date_time=input_serifxml_document.date_time)

        # Fix region and metadata span offsets
        new_document.construct_regions(region.start_char, region.end_char, region.tag)
        if span is not None:
            new_document.construct_metadata(
                span.start_char, span.end_char, span.span_type, span.region_type)
        return [new_document]

    @staticmethod
    def construct_new_document(original_text, language, docid, *, source_type='UNKNOWN', is_downcased=False,
                               time_start=None, time_end=None, date_time=None):
        new_document = Document.from_string(original_text, language, docid)
        new_document.source_type = source_type
        new_document.is_downcased = is_downcased
        if time_start is not None:
            new_document.document_time_start = time_start
        if time_end is not None:
            new_document.document_time_end = time_end
        if date_time is not None:
            new_document.date_time = date_time
        return new_document
