from serif.theory.enumerated_type import Genericity, Polarity, Tense, Modality
from serif.theory.event_mention import EventMention
from serif.theory.serif_theory import SerifTheory
from serif.xmlio import _ChildTheoryElementList, _SimpleAttribute, _ReferenceListAttribute


class Event(SerifTheory):
    arguments = _ChildTheoryElementList('EventArg')
    event_type = _SimpleAttribute(is_required=True)
    event_mentions = _ReferenceListAttribute('event_mention_ids',
                                             cls=EventMention)
    genericity = _SimpleAttribute(Genericity, is_required=True)
    polarity = _SimpleAttribute(Polarity, is_required=True)
    tense = _SimpleAttribute(Tense, is_required=True)
    modality = _SimpleAttribute(Modality, is_required=True)
    annotation_id = _SimpleAttribute()
