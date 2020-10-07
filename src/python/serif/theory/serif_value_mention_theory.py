from serif.theory.serif_offset_theory import SerifOffsetTheory


class SerifValueMentionTheory(SerifOffsetTheory):
    @property
    def tokens(self):
        from serif.theory.sentence import Sentence
        """The list of tokens covered by this ValueMention"""
        tok_seq = list(self.owner_with_type(Sentence).token_sequence)
        s = tok_seq.index(self.start_token)
        e = tok_seq.index(self.end_token)
        return tok_seq[s:e + 1]

    @property
    def sentence(self):
        if self.sent_no:
            return self.sent_no
        else:
            return self.owner.owner.sent_no

    def get_normalized_time(self):
        """If this is a time ValueMention, return the normalized time
           (if any) from the Value object that contains it. Otherwise
           return None.
        """
        doc = self.document
        for value in doc.value_set or list():
            if value.value_mention == self:
                return value.timex_val
        return None
