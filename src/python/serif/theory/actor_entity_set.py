from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _ChildTheoryElementList


class ActorEntitySet(SerifSequenceTheory):
    _children = _ChildTheoryElementList('ActorEntity')
