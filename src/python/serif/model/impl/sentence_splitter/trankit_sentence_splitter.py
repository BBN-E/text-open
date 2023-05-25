import trankit

from serif.model.sentence_splitter_model import SentenceSplitterModel


class TrankitSentenceSplitter(SentenceSplitterModel):
    def __init__(self, lang, dir, **kwargs):
        super(TrankitSentenceSplitter, self).__init__(**kwargs)

        self.split_on_newlines = False
        if "split_on_newlines" in kwargs:
            self.split_on_newlines = True

        self.nlp = \
            trankit.Pipeline(lang=lang, cache_dir=dir)

    def add_sentences_to_document(self, serif_doc, region):
        regions_starts_ends = []

        text_sections = [region.text]
        if self.split_on_newlines:
            text_sections = region.text.split("\n")

        last_end = -1

        for text in text_sections:
            if len(text.strip()) == 0:
                last_end += len(text) + 1  # + 1 is for the newline we split on
                continue

            doc = self.nlp.tokenize(text)

            for sentence in doc['sentences']:
                start_offset, end_offset = self.get_offsets_for_sentence(sentence, region, last_end + 1)
                last_end = end_offset
                sentence_start = region.start_char + start_offset
                sentence_end = region.start_char + end_offset
                sentence_text = region.text[sentence_start:sentence_end + 1]
                if len(sentence_text.strip()) != 0:
                    regions_starts_ends.extend(
                        SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, sentence_start,
                                                               sentence_end))

            last_end += 1  # For the newline we split on

        return regions_starts_ends

    def get_offsets_for_sentence(self, sentence, region, start_search):
        start = None

        token_number = 0
        token = sentence['tokens'][token_number]
        token_pos = 0
        region_pos = start_search

        while True:
            if token_pos >= len(token['text']):
                token_number += 1
                if token_number >= len(sentence['tokens']):
                    break
                token = sentence['tokens'][token_number]
                token_pos = 0

            current_char = region.text[region_pos]
            if current_char == token['text'][token_pos]:
                if start is None:
                    start = region_pos
                region_pos += 1
                token_pos += 1
            elif current_char.isspace():
                region_pos += 1
            else:
                print("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                       ord(current_char),
                                                                                       token['text'][token_pos],
                                                                                       ord(token['text'][token_pos])))
                print("Sentence:", sentence)
                raise NotImplementedError("Character mismatch is undesired please fix this.")

        return start, region_pos - 1
