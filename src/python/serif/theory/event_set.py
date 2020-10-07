from serif.theory.event import Event
from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _ChildTheoryElementList
from serif.theory.enumerated_type import Genericity, Polarity, Tense, Modality

class EventSet(SerifSequenceTheory):
    _children = _ChildTheoryElementList('Event')

    def add_event(self, event):
        self._children.append(event)

    def add_new_event(self, event_mentions, event_type):
        event = self.construct_event(event_mentions, event_type)
        self.add_event(event)
        return event

    def construct_event(self, event_mentions, event_type):
        event = Event(owner=self)
        event.event_type = event_type
        event.event_mentions = event_mentions
        event.genericity = Genericity.Specific
        event.modality = Modality.Other
        event.tense = Tense.Unspecified
        event.polarity = Polarity.Positive
        event.document.generate_id(event)
        return event
