from serif.theory.event_mention import EventMention
from serif.theory.parse import Parse
from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _SimpleAttribute, _ReferenceAttribute, _ChildTheoryElementList

from serif.theory.enumerated_type import Genericity,Polarity,Tense,Modality


class EventMentionSet(SerifSequenceTheory):
    score = _SimpleAttribute(float)
    parse = _ReferenceAttribute('parse_id', cls=Parse)
    _children = _ChildTheoryElementList('EventMention')

    def add_event_mention(self, event_mention):
        self._children.append(event_mention)

    def add_new_event_mention(self, event_type, anchor_node, score):
        event_mention = self.construct_event_mention(
            event_type, anchor_node, score)
        self.add_event_mention(event_mention)
        return event_mention

    def construct_event_mention(self, event_type, anchor_node, score):
        event_mention = EventMention(owner=self)
        event_mention.event_type = event_type
        event_mention.anchor_node = anchor_node
        event_mention.score = score
        event_mention.genericity = Genericity.Specific
        event_mention.polarity = Polarity.Positive
        event_mention.tense = Tense.Present
        event_mention.modality = Modality.Asserted
        event_mention.document.generate_id(event_mention)
        return event_mention
