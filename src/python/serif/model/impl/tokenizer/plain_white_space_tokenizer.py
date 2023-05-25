from serif.model.tokenizer_model import TokenizerModel


class PlainWhiteSpaceTokenizer(TokenizerModel):
    '''Does nothing but return a tokenization according to whitespace (doesn't attempt to split off final punctuation)'''

    def __init__(self,**kwargs):
        super(PlainWhiteSpaceTokenizer,self).__init__(**kwargs)

    def add_tokens_to_sentence(self, sentence):
        ret = []

        text = sentence.text
        chunks = text.split(" ")
        char_visited = 0
        for i,chunk in enumerate(chunks):
            token_text = chunk.strip()

            char_start = sentence.start_char + char_visited + chunk.find(token_text)
            char_end = char_start + len(token_text) - 1


            char_visited += len(chunk) + 1

            # Do not add empty sentence
            if len(token_text) > 0:
                ret.extend(TokenizerModel.add_new_token(sentence.token_sequence, token_text, char_start, char_end))

        return ret
