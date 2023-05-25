import serifxml3

from serif.model.parser_model import ParserModel


class RoundTripperParserModel(ParserModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperParserModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides ParserModel.get_parse_info
    # Doesn't return a list of tuples like in most other 
    # models, instead returns a sexp treebank parse string
    def add_parse_to_sentence(self, sentence):
        # Get matching sentence from self.serif_doc
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]
        return self.add_new_parse(sentence,
                                  serif_doc_sentence.parse.root._treebank_str(),
                                  serif_doc_sentence.parse.score)
