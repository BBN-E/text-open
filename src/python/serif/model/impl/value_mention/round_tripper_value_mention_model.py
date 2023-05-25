import serifxml3
from serif.model.impl.round_tripper_util import find_matching_token
from serif.model.value_mention_model import ValueMentionModel


class RoundTripperValueMentionModel(ValueMentionModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperValueMentionModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides ValueMentionModel.add_value_mentions_to_sentence
    def add_value_mentions_to_sentence(self, sentence):
        # Get matching sentence from self.serif_doc
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.value_mention_set is not None:
            sentence.value_mention_set.score = serif_doc_sentence.value_mention_set.score

        # Create list of tuples, each of which specifies a 
        # ValueMention object. These will be placed in
        # ValueMentionSet object on the sentence
        ret = []
        for value_mention in serif_doc_sentence.value_mention_set or ():
            new_start_token = find_matching_token(value_mention.start_token, sentence.token_sequence)
            new_end_token = find_matching_token(value_mention.end_token, sentence.token_sequence)

            ret.extend(self.add_new_value_mention(sentence.value_mention_set, value_mention.value_type, new_start_token,
                                                  new_end_token, sent_no=value_mention.sent_no))

        return ret
