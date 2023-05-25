import serifxml3

from serif.model.part_of_speech_model import PartOfSpeechModel


class RoundTripperPOSModel(PartOfSpeechModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperPOSModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides PartOfSpeechModel.add_pos_to_sentence
    def add_pos_to_sentence(self, sentence):
        # Get matching sentence from self.serif_doc
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.pos_sequence is not None:
            sentence.pos_sequence.score = serif_doc_sentence.pos_sequence.score

        # Create list of tuples, each specifying a POS object.
        # These will be placed in a PartOfSpeechSequence object 
        # on the sentence.
        part_of_speech_info = []
        for pos in serif_doc_sentence.pos_sequence:
            new_token = sentence.token_sequence[pos.token.index()]
            part_of_speech_info.extend(
                PartOfSpeechModel.add_new_pos(sentence.pos_sequence, new_token, pos.tag, upos=pos.upos,
                                              dep_rel=pos.dep_rel))

        return part_of_speech_info
