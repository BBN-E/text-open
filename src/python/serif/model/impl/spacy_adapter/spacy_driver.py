import spacy

import enum
import logging
import re

from serif.model.document_model import DocumentModel
from serif.model.impl.stanza_adapter2.utils import build_region_to_text_sections_map

logger = logging.getLogger(__name__)


class SpacyStageToAdd(enum.Enum):
    sentence_splitting = enum.auto()
    tokenization = enum.auto()
    parsing = enum.auto()
    ner = enum.auto()
    universal_dependency = enum.auto()


class SpacyDriver(DocumentModel):
    def __init__(self, lang, stage_to_add, **kwargs):
        super(SpacyDriver, self).__init__(**kwargs)
        self.lang = lang
        self.stage_to_add = {SpacyStageToAdd[i.strip()] for i in stage_to_add.split(",")}

    def load_model(self):
        self.nlp = spacy.load("en_core_web_sm")
        if SpacyStageToAdd.parsing in self.stage_to_add:
            import benepar
            self.nlp.add_pipe('benepar', config={'model': 'benepar_en3'})

    def unload_model(self):
        del self.nlp
        self.nlp = None

    def process_document(self, serif_doc):

        region_to_text_sections = build_region_to_text_sections_map(serif_doc)
        for region, text_sections in region_to_text_sections.items():
            if hasattr(region, "aux") is False:
                region.aux = dict()
            region.aux.setdefault("spacy_docs", list())

            for original_text in text_sections:
                stripped_text = original_text.strip()
                if len(stripped_text) == 0:
                    region.aux["spacy_docs"].append(None)
                    continue

                whitespace_removed_text = re.sub(pattern="\s+", repl=" ", string=stripped_text)
                try:
                    spacy_doc = self.nlp(whitespace_removed_text)
                except ValueError:
                    # Skip sentences over 512 character limit
                    spacy_doc = self.nlp(whitespace_removed_text, disable=["benepar"])

                region.aux["spacy_docs"].append(spacy_doc)
