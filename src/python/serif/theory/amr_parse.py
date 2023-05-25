from serif.theory.serif_amr_parse_theory import SerifAMRParseTheory
from serif.theory.token_sequence import TokenSequence
from serif.xmlio import _SimpleAttribute, _ReferenceAttribute, _ChildTextElement, _ChildTheoryElement

class AMRParse(SerifAMRParseTheory):
    score = _SimpleAttribute(float)
    token_sequence = _ReferenceAttribute('token_sequence_id', cls=TokenSequence)
    root = _ChildTheoryElement('AMRNode')
    _amr_string = _ChildTextElement('AMRString')

    @classmethod
    def from_values(cls, owner=None, score=score, token_sequence=None, amr_string=None):
        ret = cls(owner=owner)
        ret.document.generate_id(ret)
        ret.set_score(score)
        ret.set_token_sequence(token_sequence)
        ret.set_amr_string(amr_string)
        ret._parse_amr_string()
        return ret

    def set_score(self, score):
        self.score = score

    def set_amr_string(self, amr_string):
        self._amr_string = amr_string

    def set_token_sequence(self, token_sequence):
        self.token_sequence = token_sequence
