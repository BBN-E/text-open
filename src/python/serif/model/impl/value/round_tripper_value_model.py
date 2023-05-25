import serifxml3
from serif.model.impl.round_tripper_util import find_matching_value_mention
from serif.model.value_model import ValueModel
from serif.theory.sentence import Sentence


class RoundTripperValueModel(ValueModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperValueModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides ValueModel.get_value_info
    def add_new_values_to_document(self, document):
        # Create list of tuples, each specifying a Value object.
        # These will be placed in a ValueSet object on document.
        added_values = []
        for value in self.serif_doc.value_set or ():
            # Values consist of one ValueMention
            sent_no = value.value_mention.owner_with_type(Sentence).sent_no
            new_sentence = document.sentences[sent_no]
            new_value_mention = find_matching_value_mention(
                value.value_mention, new_sentence)
            added_values.extend(
                self.add_new_value(document.value_set, new_value_mention, value.value_type, value.timex_val))

        return added_values
