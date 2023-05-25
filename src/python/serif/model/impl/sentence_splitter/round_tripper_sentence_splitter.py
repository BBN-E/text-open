import serifxml3

from serif.model.sentence_splitter_model import SentenceSplitterModel


class RoundTripperSentenceSplitter(SentenceSplitterModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperSentenceSplitter, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides SentenceSplitterModel.get_sentence_info
    def add_sentences_to_document(self, serif_doc, region):
        # Get matching region from self.serif_doc
        serif_doc_region = None
        for r in self.serif_doc.regions:
            if (r.start_char == region.start_char and
                    r.end_char == region.end_char):
                serif_doc_region = r
                break
        if serif_doc_region is None:
            raise Exception("Could not find matching region")

        # Create list of tuples, each of which specifies a 
        # Sentence object. These will placed in a 
        # Sentences object stored on the Document.
        sentence_info = []
        for sentence in self.serif_doc.sentences:
            if sentence.region == serif_doc_region:
                sentence_info.extend(
                    SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, sentence.start_char,
                                                           sentence.end_char))

        return sentence_info
