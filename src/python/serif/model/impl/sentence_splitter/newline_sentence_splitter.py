from serif.model.sentence_splitter_model import SentenceSplitterModel


class NewlineSentenceSplitter(SentenceSplitterModel):
    '''
    This should be used when each sentence is on a separate line of document -- useful for converting pre-tokenized corpora to serifxml via pipeline
    '''

    universal_newline_set = {"\v", "\x0b", "\f", "\x0c", "\x1c", "\x1d", "\x1e", "\x85", "\u2028", "\u2029"}

    def __init__(self, **kwargs):
        """
        :param kwargs:
            create_empty:
                This is very useful if you want to strictly assume number of sentences matches with your input for alignment purpose. When specified, it will create empty sentence as place holders for empty lines.
                Usage:
                ```
                SENTENCE_SPLITTING_MODEL NewlineSentenceSplitter
                create_empty
                ```
        """
        super(NewlineSentenceSplitter, self).__init__(**kwargs)
        if "create_empty" in kwargs:
            self.create_empty = True
        else:
            self.create_empty = False
        if kwargs.get("use_universal_newline",
                      "true").lower() == "true":  # https://docs.python.org/3/library/stdtypes.html?highlight=splitlines#str.splitlines
            self.use_universal_newline = True
        else:
            self.use_universal_newline = False

    def add_sentences_to_document(self, serif_doc, region):
        '''assumes each line already stripped for accurate char offsets'''

        added_sentences = list()
        text = region.text
        if self.use_universal_newline is False:
            for universal_newline_char in self.universal_newline_set:
                text = text.replace(universal_newline_char, " ")
        sentences = text.splitlines()
        char_visited = 0

        for sentence in sentences:

            sentence_text = sentence.strip()

            char_start = region.start_char + char_visited + sentence.find(sentence_text)
            char_end = char_start + len(sentence_text) - 1

            char_visited += len(sentence) + 1

            # Do not add empty sentence
            if len(sentence_text) > 0 or self.create_empty is True:
                # print(sentence_text)
                added_sentences.extend(
                    SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, char_start, char_end))

        return added_sentences
