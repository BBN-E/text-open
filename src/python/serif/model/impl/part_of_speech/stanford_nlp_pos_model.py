from serif.model.part_of_speech_model import PartOfSpeechModel

import sys
import stanfordnlp

class StanfordNLPPOSModel(PartOfSpeechModel):
    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPPOSModel, self).__init__(**kwargs)
        self.nlp = \
           stanfordnlp.Pipeline(
               processors='tokenize,pos', 
               lang=lang, 
               tokenize_pretokenized=True,
               models_dir=models_dir,use_gpu=True)

    def get_part_of_speech_info(self, sentence):
        part_of_speech_info = []

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
                part_of_speech_info.append((serif_token, word.xpos, word.upos))
                serif_token_count += 1
        return part_of_speech_info

