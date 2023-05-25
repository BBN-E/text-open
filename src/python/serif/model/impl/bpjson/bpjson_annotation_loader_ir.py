import sys, os, logging

# Need the following in PYTHONPATH:
# [BETTER_REPO]/python:[NLPLINGO_REPO]
sys.path.insert(0, os.path.realpath(os.path.join(os.path.realpath(__file__), "..", "..", "..", "..", "..")))
from serif.util.add_bpjson_event_mentions_to_serifxml import *
from serif.model.document_model import DocumentModel


logger = logging.getLogger(__name__)


class BpjsonAnnotationLoaderIR(DocumentModel):
    def __init__(self, **kwargs):
        super(BpjsonAnnotationLoaderIR, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        segment = serif_doc.bp_segment

        abstract_events = segment.abstract_events
        basic_events = segment.basic_events
        granular_templates = segment.granular_templates

        statistics = defaultdict(int)

        entity_cache = None  # SpanSet ID to Entity
        event_mention_cache = None  # BP JSON Event ID to EventMention

        if len(abstract_events) > 0:
            abstract_events_bp_json_to_serifxml(serif_doc, segment, statistics)

        if len(basic_events) > 0:
            entity_cache, event_mention_cache = basic_events_bpjson_to_serifxml(segment, serif_doc, statistics)

        if len(granular_templates) > 0:
            if entity_cache is None or event_mention_cache is None:
                logger.error("Need basic events to have granular events")
            granular_templates_bpjson_to_serifxml(segment, serif_doc, entity_cache, event_mention_cache, statistics)

        return serif_doc


if __name__ == "__main__":
    pass
