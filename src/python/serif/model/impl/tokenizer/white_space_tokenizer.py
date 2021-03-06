from serif.model.tokenizer_model import TokenizerModel


class WhiteSpaceTokenizer(TokenizerModel):

    def __init__(self,**kwargs):
        super(WhiteSpaceTokenizer,self).__init__(**kwargs)

    def get_token_info(self, sentence):
        texts_starts_ends = []

        text = sentence.text
        chunks = text.split(" ")
        char_visited = 0
        for i in range(0, len(chunks)):
            chunk = chunks[i]
            token_text = chunk.strip()

            if i == len(chunks) - 1 and token_text[-1] == ".":
                char_start = sentence.start_char + char_visited + chunk.find(token_text)
                char_end = char_start + len(token_text) - 2
                token_text = token_text[0:-1]
                print(token_text)
                if len(token_text) > 0:
                    texts_starts_ends.append((token_text, char_start, char_end))

                # adding the ending "."
                char_start = char_end + 1
                char_end = char_start
                texts_starts_ends.append((".", char_start, char_end))
            else:
                char_start = sentence.start_char + char_visited + chunk.find(token_text)
                char_end = char_start + len(token_text) - 1
                print(token_text)

                char_visited += len(chunk) + 1

                # Do not add empty sentence
                if len(token_text) > 0:
                    texts_starts_ends.append((token_text, char_start, char_end))

        return texts_starts_ends
