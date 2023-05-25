import logging
import re

import stanza
import torch

from serif.model.sentence_splitter_model import SentenceSplitterModel

logger = logging.getLogger(__name__)

TAG_PATTERN = re.compile('<[^<>]+>', re.MULTILINE)


class DummyRegion(object):
    def __init__(self, document, text):
        self.document = document
        self.text = text


def get_offsets_for_token(vocab_cls, stanford_token, sentence, start_search):
    start = None
    token_pos = 0
    sentence_pos = start_search

    def stanford_normalize_char(current_char):
        """Stanford's normalizer is designed to work on token and strips leading space.
        Adding an 'X' to avoid that to work on single character.

        See https://github.com/stanfordnlp/stanza/blob/master/stanza/models/tokenize/vocab.py#L29
        """
        return vocab_cls.normalize_token('X' + current_char)[1:]

    while True:
        if token_pos >= len(stanford_token.text):
            break

        current_char = sentence[sentence_pos]

        # A tokenized stanford_token can contain space inside like ": )".
        # To cope with that we only skip leading spaces
        if start is None and current_char.isspace():
            sentence_pos += 1
        # stanford_token.text is normalized so that TAB becomes ' ':
        # we need to keep that in mind when comparing characters
        elif stanford_token.text[token_pos].isspace() or sentence[sentence_pos].isspace():
            while token_pos < len(stanford_token.text) and stanford_token.text[token_pos].isspace():
                token_pos += 1
            while sentence_pos < len(sentence) and sentence[sentence_pos].isspace():
                sentence_pos += 1
        elif stanford_normalize_char(current_char) != stanford_token.text[token_pos]:
            if current_char == " " and stanford_token.text[
                token_pos] == "_":  # this is OK, we did a substitution in get_tokenized_sentence_text()
                pass
            else:
                logger.critical("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                                 ord(current_char),
                                                                                                 stanford_token.text[
                                                                                                     token_pos],
                                                                                                 ord(
                                                                                                     stanford_token.text[
                                                                                                         token_pos])))
            logger.critical("Sentence: {}".format(sentence))
            raise AssertionError()
        else:
            if start is None:
                start = sentence_pos
            sentence_pos += 1
            token_pos += 1
    return start, sentence_pos - 1

def remove_xml_marking(text):
    buffer = ""
    current_end = 0
    for i in TAG_PATTERN.finditer(text):
        start_char, end_char = i.span()
        buffer += text[current_end:start_char] + (" " * (end_char - start_char))
        current_end = end_char
    buffer += text[current_end:]
    assert len(buffer) == len(text)
    return buffer

class StanzaSentenceSplitter(SentenceSplitterModel):
    def __init__(self, lang, dir, **kwargs):
        super(StanzaSentenceSplitter, self).__init__(**kwargs)
        # stanza.download(lang=lang,
        #                 dir=models_dir,
        #                 package='default',
        #                 processors=dict(),
        #                 logging_level='INFO',
        #                 verbose=None)
        # print("dir", dir)

        self.split_on_newlines = False
        if "split_on_newlines" in kwargs:
            self.split_on_newlines = True
        self.lang = lang
        self.dir = dir
        self.drop_xml_marking_before_processing = False
        if "drop_xml_marking_before_processing" in kwargs:
            self.drop_xml_marking_before_processing = True

    def load_model(self):
        self.nlp = \
            stanza.Pipeline(
                processors='tokenize',
                lang=self.lang,
                tokenize_pretokenized=False,
                dir=self.dir)
        if hasattr(stanza.models, "tokenize"):
            self.vocab_cls = stanza.models.tokenize.vocab.Vocab(lang=self.lang)
        else:
            self.vocab_cls = stanza.models.tokenization.vocab.Vocab(
                lang=self.lang)  # we only need to access Vocab.normalize_token()

    def unload_model(self):
        del self.nlp
        del self.vocab_cls
        self.nlp = None
        self.vocab_cls = None
        torch.cuda.empty_cache()

    def add_sentences_to_document(self, serif_doc, region):
        ret = []

        text_sections = [serif_doc.get_original_text_substring(region.start_char, region.end_char)]
        if self.split_on_newlines:
            text_sections = text_sections[0].splitlines()

        last_end = -1

        escaped_region = region

        if self.drop_xml_marking_before_processing is True:
            escaped_region = DummyRegion(serif_doc, remove_xml_marking(
                                             serif_doc.get_original_text_substring(region.start_char, region.end_char)))

        for text in text_sections:
            if len(text.strip()) == 0:
                last_end += len(text) + 1  # + 1 is for the newline we split on
                continue

            modified_text = text
            if self.drop_xml_marking_before_processing is True:
                modified_text = remove_xml_marking(text)

            doc = self.nlp(modified_text)

            for sentence in doc.sentences:
                start_offset, end_offset = get_offsets_for_token(self.vocab_cls, sentence, escaped_region.text,
                                                                 last_end + 1)
                last_end = end_offset
                sentence_start = region.start_char + start_offset
                sentence_end = region.start_char + end_offset
                sentence_text = serif_doc.get_original_text_substring(sentence_start, sentence_end)
                if len(sentence_text.strip()) != 0:
                    ret.extend(SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, sentence_start,
                                                                      sentence_end))

            last_end += 1  # For the newline we split on

        return ret
