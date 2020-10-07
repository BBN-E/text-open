from serif.model.sentence_splitter_model import SentenceSplitterModel


class NewlineSentenceSplitter(SentenceSplitterModel):
    '''
    This should be used when each sentence is on a separate line of document -- useful for converting pre-tokenized corpora to serifxml via pipeline
    '''

    def __init__(self,**kwargs):
        super(NewlineSentenceSplitter,self).__init__(**kwargs)

    def get_sentence_info(self, region):
        '''assumes each line already stripped for accurate char offsets'''

        regions_starts_ends = []

        text = region.text
        sentences = text.split("\n")
        char_visited = 0

        for sentence in sentences:

            sentence_text = sentence.strip()

            char_start = region.start_char + char_visited + sentence.find(sentence_text)
            char_end = char_start + len(sentence_text) - 1

            char_visited += len(sentence) + 1

            # Do not add empty sentence
            if len(sentence_text) > 0:
                # print(sentence_text)
                regions_starts_ends.append((region, char_start, char_end))

        return regions_starts_ends
