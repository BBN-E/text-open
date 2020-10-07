from serif.model.sentence_splitter_model import SentenceSplitterModel


class PunctuationSentenceSplitter(SentenceSplitterModel):
    def __init__(self,**kwargs):
        super(PunctuationSentenceSplitter,self).__init__(**kwargs)

    def get_sentence_info(self, region):
        regions_starts_ends = []

        text = region.text
        chunks = text.split(".")
        char_visited = 0
        for chunk in chunks:
            sentence_text = chunk.strip()

            char_start = region.start_char + char_visited + chunk.find(sentence_text)
            char_end = char_start + len(sentence_text) - 1

            # add ending "."
            if text[char_end + 1 - region.start_char:char_end + 2 - region.start_char] == ".":
                char_end += 1

            char_visited += len(chunk) + 1

            # Do not add empty sentence
            if len(sentence_text) > 0:
                # print(sentence_text)
                regions_starts_ends.append((region, char_start, char_end))
        return regions_starts_ends
