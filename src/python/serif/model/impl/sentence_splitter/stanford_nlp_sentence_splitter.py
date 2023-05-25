import stanfordnlp
from serif.model.sentence_splitter_model import SentenceSplitterModel


class StanfordNLPSentenceSplitter(SentenceSplitterModel):
    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPSentenceSplitter, self).__init__(**kwargs)
        self.nlp = \
            stanfordnlp.Pipeline(
                processors='tokenize',
                lang=lang,
                tokenize_pretokenized=False,
                models_dir=models_dir,
                use_gpu=True)
        self.vocab_cls = stanfordnlp.models.tokenize.vocab.Vocab(
            lang=lang)  # we only need to access Vocab.normalize_token()
        self.split_on_newlines = False
        if "split_on_newlines" in kwargs:
            self.split_on_newlines = True

    def add_sentences_to_document(self, serif_doc, region):
        ret = []

        text_sections = [region.text]
        if self.split_on_newlines:
            text_sections = region.text.split("\n")

        last_end = -1
        for text in text_sections:
            if len(text.strip()) == 0:
                last_end += len(text) + 1  # + 1 is for the newline we split on
                continue

            doc = self.nlp(text)

            for sentence in doc.sentences:
                start_offset, end_offset = self.get_offsets_for_sentence(sentence, region, last_end + 1)
                last_end = end_offset

                sentence_start = region.start_char + start_offset
                sentence_end = region.start_char + end_offset
                sentence_text = region.text[sentence_start:sentence_end + 1]
                if len(sentence_text.strip()) != 0:
                    ret.extend(SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, sentence_start,
                                                                      sentence_end))

            last_end += 1  # For the newline we split on

        return ret

    def get_offsets_for_sentence(self, sentence, region, start_search):
        start = None

        token_number = 0
        token = sentence.tokens[token_number]
        token_pos = 0
        region_pos = start_search

        def stanford_normalize_char(ch):
            """Stanford's normalizer is designed to work on token and strips leading space.
            Adding an 'X' to avoid that to work on single character.

            See https://github.com/stanfordnlp/stanza/blob/master/stanza/models/tokenize/vocab.py#L29
            """
            return self.vocab_cls.normalize_token('X' + current_char)[1:]

        while True:
            if token_pos >= len(token.text):
                token_number += 1
                if token_number >= len(sentence.tokens):
                    break
                token = sentence.tokens[token_number]
                token_pos = 0

            current_char = region.text[region_pos]
            if stanford_normalize_char(current_char) == token.text[token_pos]:
                if start is None:
                    start = region_pos
                region_pos += 1
                token_pos += 1
            elif current_char.isspace():
                region_pos += 1
            else:
                raise ValueError("Character mismatch in tokenizer! {} (ord={}) != {} (ord={})".format(current_char,
                                                                                                      ord(current_char),
                                                                                                      token.text[
                                                                                                          token_pos],
                                                                                                      ord(token.text[
                                                                                                              token_pos])))

        return start, region_pos - 1
