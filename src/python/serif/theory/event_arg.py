from serif.xmlio import _SimpleAttribute, _ReferenceAttribute
from serif.theory.serif_theory import SerifTheory
from serif.theory.entity import Entity
from serif.theory.value import Value

class EventArg(SerifTheory):
    role = _SimpleAttribute(default='')
    entity = _ReferenceAttribute('entity_id',
                                 cls=Entity)
    value_entity = _ReferenceAttribute('value_id',
                                       cls=Value)
    score = _SimpleAttribute(float, default=0.0)
    value = property(
        lambda self: self.entity or self.value_entity)
