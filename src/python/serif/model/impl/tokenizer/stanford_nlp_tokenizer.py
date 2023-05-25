import stanfordnlp
from serif.model.tokenizer_model import TokenizerModel


class StanfordNLPTokenizer(TokenizerModel):
    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPTokenizer, self).__init__(**kwargs)
        self.nlp = \
            stanfordnlp.Pipeline(
                processors='tokenize',
                lang=lang,
                tokenize_pretokenized=False,
                models_dir=models_dir,
                use_gpu=True)

        self.vocab_cls = stanfordnlp.models.tokenize.vocab.Vocab(
            lang=lang)  # we only need to access Vocab.normalize_token()

    def add_tokens_to_sentence(self, sentence):
        ret = []

        text = sentence.text
        doc = self.nlp(text)

        stanford_tokens = []
        # Grab all tokens from StanfordNLP sentences in order to handle case
        # where StanfordNLP split up text into multiple sentences
        for stanford_sentence in doc.sentences:
            stanford_tokens.extend(stanford_sentence.tokens)

        last_end = -1
        for stanford_token in stanford_tokens:
            start_offset, end_offset = self.get_offsets_for_token(stanford_token, sentence, last_end + 1)
            last_end = end_offset
            ret.extend(TokenizerModel.add_new_token(sentence.token_sequence, stanford_token.text,
                                                    sentence.start_char + start_offset,
                                                    sentence.start_char + end_offset))

        return ret

    def get_offsets_for_token(self, stanford_token, sentence, start_search):
        start = None

        token_pos = 0
        sentence_pos = start_search

        def stanford_normalize_char(ch):
            """Stanford's normalizer is designed to work on token and strips leading space.
            Adding an 'X' to avoid that to work on single character.

            See https://github.com/stanfordnlp/stanza/blob/master/stanza/models/tokenize/vocab.py#L29
            """
            return self.vocab_cls.normalize_token('X' + current_char)[1:]

        while True:
            if token_pos >= len(stanford_token.text):
                break

            current_char = sentence.text[sentence_pos]

            # A tokenized stanford_token can contain space inside like ": )".
            # To cope with that we only skip leading spaces
            if start is None and current_char.isspace():
                sentence_pos += 1
            # stanford_token.text is normalized so that TAB becomes ' ':
            # we need to keep that in mind when comparing characters
            elif stanford_normalize_char(current_char) != stanford_token.text[token_pos]:
                raise ValueError("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                                  ord(current_char),
                                                                                                  stanford_token.text[
                                                                                                      token_pos],
                                                                                                  ord(
                                                                                                      stanford_token.text[
                                                                                                          token_pos])))
            else:
                if start is None:
                    start = sentence_pos
                sentence_pos += 1
                token_pos += 1

        return start, sentence_pos - 1
