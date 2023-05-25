import enum
import logging

from serif.model.document_model import DocumentModel
from serif.model.impl.spacy_adapter.spacy_sentence_splitting_adapter import spacy_sentence_splitter_adder
from serif.model.impl.spacy_adapter.spacy_tokenization_adapter import spacy_tokenizer_adder
from serif.model.impl.spacy_adapter.spacy_ner_adapter import spacy_ner_adder
from serif.model.impl.spacy_adapter.spacy_parser_adapter import spacy_parsing_adder
from serif.model.impl.spacy_adapter.spacy_depparse_adapter import spacy_depparse_adder

from serif.model.impl.stanza_adapter2.utils import build_region_to_text_sections_map

logger = logging.getLogger(__name__)


class SpacyStageToAdd(enum.Enum):
    sentence_splitting = enum.auto()
    tokenization = enum.auto()
    parsing = enum.auto()
    ner = enum.auto()
    depparse = enum.auto()


class SpacyAdapter(DocumentModel):
    def __init__(self, stage_to_add, **kwargs):
        super(SpacyAdapter, self).__init__(**kwargs)

        self.stage_to_add = {SpacyStageToAdd[i.strip()] for i in stage_to_add.split(",")}

    def process_document(self, serif_doc):
        serif_doc.add_new_sentences()
        region_to_text_sections = build_region_to_text_sections_map(serif_doc)
        current_end_char = 0
        for region in serif_doc.regions:
            for idx, original_text in enumerate(region_to_text_sections[region]):
                if len(original_text.strip()) == 0:
                    current_end_char += len(original_text)
                    continue
                spacy_doc = region.aux['spacy_docs'][idx]
                current_end_char, _ = spacy_sentence_splitter_adder(current_end_char, spacy_doc, serif_doc, region)
                current_end_char += 1

        for serif_sentence in serif_doc.sentences:
            if SpacyStageToAdd.tokenization in self.stage_to_add:
                # Tokenization
                spacy_tokenizer_adder(serif_sentence)
            if SpacyStageToAdd.ner in self.stage_to_add:
                # NER
                spacy_ner_adder(serif_sentence)
            if SpacyStageToAdd.parsing in self.stage_to_add:
                # Parsing
                spacy_parsing_adder(serif_sentence)
            if SpacyStageToAdd.depparse in self.stage_to_add:
                # Dependency Parsing
                spacy_depparse_adder(serif_sentence)
