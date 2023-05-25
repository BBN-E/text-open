from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _SimpleAttribute, _ChildTheoryElementList
from serif.theory.alert_author import ALERTAuthor

class ALERTMetadata(SerifSequenceTheory):
    corpus = _SimpleAttribute()
    _children = _ChildTheoryElementList("ALERTAuthor")

    def construct_author(self):
        author = ALERTAuthor(owner=self)
        return author

    def add_new_author(self):
        author = self.construct_author()
        self._children.append(author)
        return author

