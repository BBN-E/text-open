import serifxml3

from serif.model.name_model import NameModel


class RoundTripperNameModel(NameModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperNameModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides NameModel.add_names_to_sentence
    def add_names_to_sentence(self, sentence):
        # Get matching sentence from self.serif_doc
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.name_theory is not None:
            sentence.name_theory.score = serif_doc_sentence.name_theory.score

        # Create list of tuples, each specifying a Name object. 
        # These will be placed in a NameTheory object on the 
        # sentence.
        name_info = []
        for name in serif_doc_sentence.name_theory or ():
            new_start_token = sentence.token_sequence[name.start_token.index()]
            new_end_token = sentence.token_sequence[name.end_token.index()]
            name_info.extend(NameModel.add_new_name(sentence.name_theory, name.entity_type,
                                                    new_start_token, new_end_token))

        return name_info
