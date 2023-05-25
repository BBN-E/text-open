import serifxml3

from serif.model.tokenizer_model import TokenizerModel


class RoundTripperTokenizer(TokenizerModel):

    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperTokenizer, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides TokenizerModel.add_tokens_to_sentence
    def add_tokens_to_sentence(self, sentence):
        # Get matching sentence from self.serif_doc
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.token_sequence is not None:
            sentence.token_sequence.score = serif_doc_sentence.token_sequence.score

        # Create list of Token objects.
        # These will be placed in a TokenSequence object on the 
        # sentence.
        ret = []
        old_to_new_token_mapping = dict()
        for token in serif_doc_sentence.token_sequence:
            new_tokens = TokenizerModel.add_new_token(sentence.token_sequence, token.text,
                                                      token.start_char, token.end_char, lemma=token.lemma,
                                                      original_token_index=token.original_token_index)
            old_to_new_token_mapping[token.id] = new_tokens
            ret.extend(new_tokens)

        # Do a second pass over the TokenSequence to update any pointers between Tokens
        for token in serif_doc_sentence.token_sequence:
            new_tokens = old_to_new_token_mapping[token.id]
            if token.head is not None:
                new_head_tokens = old_to_new_token_mapping[token.head.id]
                new_head_token = new_head_tokens[0] if len(new_head_tokens) > 0 else None
                if new_head_token is not None:
                    for new_token in new_tokens:
                        TokenizerModel.add_token_head(new_token, new_head_token)

        return ret
