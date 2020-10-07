from serif.model.sentence_splitter_model import SentenceSplitterModel

import sys
import stanfordnlp

class StanfordNLPSentenceSplitter(SentenceSplitterModel):
    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPSentenceSplitter,self).__init__(**kwargs)
        self.nlp = \
           stanfordnlp.Pipeline(
               processors='tokenize', 
               lang=lang, 
               tokenize_pretokenized=False, 
               models_dir=models_dir,
               use_gpu=True)
        self.vocab_cls = stanfordnlp.models.tokenize.vocab.Vocab(lang=lang)  # we only need to access Vocab.normalize_token()

    def get_sentence_info(self, region):
        regions_starts_ends = []

        text = region.text
        if len(text.strip()) == 0:
            return []

        doc = self.nlp(text)
        
        last_end = -1
        for sentence in doc.sentences:
            start_offset, end_offset = self.get_offsets_for_sentence(sentence, region, last_end + 1)
            last_end = end_offset
            regions_starts_ends.append((region, region.start_char + start_offset, region.start_char + end_offset))

        return regions_starts_ends
    
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
                print("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                       ord(current_char),
                                                                                       stanford_token.text[token_pos],
                                                                                       ord(stanford_token.text[token_pos])))
                print("Sentence:", sentence)
                sys.exit(1)
            
        return start, region_pos - 1

    
