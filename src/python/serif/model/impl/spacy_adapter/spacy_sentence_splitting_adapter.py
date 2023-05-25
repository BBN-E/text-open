import logging

from serif.model.impl.stanza_adapter2.utils import get_offsets_for_token
from serif.model.sentence_splitter_model import SentenceSplitterModel

logger = logging.getLogger(__name__)


def get_offsets_for_token(spacy_sentence, region_text, sentence_offset):
    serif_start_char = None
    char_pos = sentence_offset
    token_to_char_offsets = {}

    for spacy_token in spacy_sentence:
        while region_text[char_pos].isspace():
            char_pos += 1

        if serif_start_char is None:
            serif_start_char = char_pos
        end_offset = char_pos + len(spacy_token) - 1
        token_to_char_offsets[spacy_token] = char_pos, end_offset
        char_pos += len(spacy_token)

    return token_to_char_offsets, serif_start_char, char_pos


def spacy_sentence_splitter_adder(current_end_char, spacy_doc, serif_doc, region):
    ret = list()
    for spacy_sentence in spacy_doc.sents:
        while region.text[current_end_char].isspace():
            current_end_char += 1

        token_to_char_offsets, serif_start_char, serif_end_char = get_offsets_for_token(spacy_sentence, region.text,
                                                                                        current_end_char)
        sentence_text = region.text[serif_start_char:serif_end_char]
        current_end_char = serif_end_char

        if len(sentence_text.strip()) != 0:
            ret.extend(SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, serif_start_char,
                                                              serif_end_char-1))
            # Attaching spacy_sentence into serif_sentence
            serif_sentence = ret[-1]
            if hasattr(serif_sentence, "aux") is False:
                serif_sentence.aux = dict()
            serif_sentence.aux["spacy_sentence"] = spacy_sentence
            serif_sentence.aux["spacy_token_to_char_offsets"] = token_to_char_offsets

    return current_end_char, ret
