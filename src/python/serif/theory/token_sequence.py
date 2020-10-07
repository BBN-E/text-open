from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.theory.token import Token
from serif.xmlio import _SimpleAttribute, _ChildTheoryElementList


class TokenSequence(SerifSequenceTheory):
    score = _SimpleAttribute(float)

    _children = _ChildTheoryElementList('Token')

    # def __init__(self, score, owner=None):
    #     super().__init__(owner=owner)
    #     self.score = score

    @classmethod
    def from_values(cls, owner=None, score=0):
        ret = cls(owner=owner)
        ret.set_score(score)
        return ret

    def set_score(self, score):
        self.score = score

    def add_new_token(self, start_char, end_char, text, lemma=None):
        token = self.construct_token(start_char, end_char, text, lemma)
        self.add_token(token)
        return token

    def construct_token(self, start_char, end_char, text, lemma):
        token = Token.from_values(self, start_char, end_char, text, lemma)
        token.document.generate_id(token)
        return token
    
    def add_token(self, token):
        self._children.append(token)
        
    def sentence(self):
        """Returns the sentence that this TokenSeqence belongs to"""
        return self.owner

    def index(self, token):
        """Returns the index of the Token in the TokenSequence or None
           if the Token is not in the Sequence. This is slow -- Order n
           with the length of the TokenSequence.
        """
        try: 
            i = self._children.index(token)
            return i
        except ValueError:
            return None



