import sys

import stanfordnlp
from serif.model.part_of_speech_model import PartOfSpeechModel


class StanfordNLPPOSModel(PartOfSpeechModel):
    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPPOSModel, self).__init__(**kwargs)
        self.nlp = \
            stanfordnlp.Pipeline(
                processors='tokenize,pos',
                lang=lang,
                tokenize_pretokenized=True,
                models_dir=models_dir, use_gpu=True)

    def add_pos_to_sentence(self, sentence):
        ret = []

        serif_token_sequence = sentence.token_sequence
        text = ""
        for token in serif_token_sequence:
            if len(text) != 0:
                text += " "
            text += token.text
        doc = self.nlp(text)

        serif_token_count = 0
        for stanford_sentence in doc.sentences:
            for stanford_token in stanford_sentence.tokens:
                serif_token = serif_token_sequence[serif_token_count]
                if serif_token.text != stanford_token.text:
                    print("Token mismatch! " + serif_token.text + " " + stanford_token.text)
                    sys.exit(1)
                word = stanford_token.words[-1]
                ret.extend(PartOfSpeechModel.add_new_pos(sentence.pos_sequence, serif_token, word.xpos, upos=word.upos))
                serif_token_count += 1
        return ret
