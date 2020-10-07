from serif.theory.serif_offset_theory import SerifOffsetTheory
from serif.xmlio import _SimpleAttribute, _ChildTextElement


class OriginalText(SerifOffsetTheory):
    contents = _ChildTextElement('Contents')
    href = _SimpleAttribute()

    @classmethod
    def from_values(cls, owner=None, start_char=0, end_char=0, text=""):
        ret = cls(owner=owner)
        ret.set_offset(start_char, end_char)
        ret.set_text(text)
        return ret

    def set_text(self, text):
        self.contents = text
