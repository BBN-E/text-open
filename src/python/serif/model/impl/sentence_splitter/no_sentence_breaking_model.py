from serif.model.sentence_splitter_model import SentenceSplitterModel


class NoSentenceBreakingModel(SentenceSplitterModel):
    def __init__(self, **kwargs):
        super(NoSentenceBreakingModel, self).__init__(**kwargs)

    def add_sentences_to_document(self, serif_doc, region):
        text = region.text
        start = 0
        for char in text:
            if not char.isspace():
                break
            start += 1

        end = len(region.text) - 1
        for char in region.text[::-1]:  # reversed region
            if not char.isspace():
                break
            end -= 1

        if len(region.text.strip()) == 0:
            return []
        else:
            return SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, region.start_char + start,
                                                          region.start_char + end)
