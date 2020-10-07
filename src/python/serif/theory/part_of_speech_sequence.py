from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.theory.token_sequence import TokenSequence
from serif.theory.pos import POS
from serif.xmlio import _SimpleAttribute, _ReferenceAttribute, _ChildTheoryElementList


class PartOfSpeechSequence(SerifSequenceTheory):
    score = _SimpleAttribute(float)
    token_sequence = _ReferenceAttribute('token_sequence_id',
                                         cls=TokenSequence)
    _children = _ChildTheoryElementList('POS')

    @classmethod
    def from_values(cls, owner=None, score=0):
        ret = cls(owner=owner)
        ret.score = score
        return ret

    def add_new_pos(self,token,tag,upos=None,dep_rel=None):
        pos = POS.from_values(owner=self,token=token,tag=tag,upos=upos,dep_rel=dep_rel)
        self._children.append(pos)
        return pos
