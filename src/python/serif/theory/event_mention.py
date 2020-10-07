from serif.theory.enumerated_type import Genericity, Polarity, Tense, Modality, DirectionOfChange
from serif.theory.event_mention_anchor import EventMentionAnchor
from serif.theory.event_mention_factor_type import EventMentionFactorType
from serif.theory.event_mention_type import EventMentionType
from serif.theory.mention import Mention
from serif.theory.proposition import Proposition
from serif.theory.serif_event_mention_theory import SerifEventMentionTheory
from serif.theory.syn_node import SynNode
from serif.theory.value_mention import ValueMention
from serif.xmlio import _SimpleAttribute, _ChildTheoryElementList, _ReferenceAttribute


class EventMention(SerifEventMentionTheory):
    arguments = _ChildTheoryElementList('EventMentionArg')
    score = _SimpleAttribute(float, default=1.0)
    event_type = _SimpleAttribute(is_required=True)
    pattern_id = _SimpleAttribute(is_required=False)
    semantic_phrase_start = _SimpleAttribute(is_required=False)
    semantic_phrase_end = _SimpleAttribute(is_required=False)
    genericity = _SimpleAttribute(Genericity, is_required=True)
    polarity = _SimpleAttribute(Polarity, is_required=True)
    direction_of_change = _SimpleAttribute(DirectionOfChange, is_required=False)
    tense = _SimpleAttribute(Tense, is_required=True)
    modality = _SimpleAttribute(Modality, is_required=True)
    anchor_prop = _ReferenceAttribute('anchor_prop_id',
                                      cls=Proposition)
    anchor_node = _ReferenceAttribute('anchor_node_id',
                                      cls=SynNode)
    model = _SimpleAttribute(is_required=False)
    genericityScore = _SimpleAttribute(float, is_required=False)
    modalityScore = _SimpleAttribute(float, is_required=False)
    event_types = _ChildTheoryElementList('EventMentionType')
    factor_types = _ChildTheoryElementList('EventMentionFactorType')
    anchors = _ChildTheoryElementList('EventMentionAnchor')

    def add_event_mention_anchor(self, em_anchor):
        self.anchors.append(em_anchor)

    def add_new_event_mention_anchor(self, anchor_node, anchor_prop=None):
        em_anchor = self.construct_event_mention_anchor(anchor_node, anchor_prop)
        self.add_event_mention_anchor(em_anchor)
        return em_anchor

    def construct_event_mention_anchor(self, anchor_node, anchor_prop):
        em_anchor = EventMentionAnchor(owner=self)
        em_anchor.anchor_node = anchor_node
        em_anchor.anchor_prop = anchor_prop
        return em_anchor

    def add_new_mention_argument(self, role, mention, score):
        event_mention_arg = self.construct_event_mention_argument(role, mention, score)
        self.add_event_mention_argument(event_mention_arg)
        return event_mention_arg

    def add_new_value_mention_argument(self, role, value_mention, score):
        event_mention_arg = self.construct_event_mention_argument(
            role, value_mention, score)
        self.add_event_mention_argument(event_mention_arg)
        return event_mention_arg

    def add_new_anchor_node_argument(self, role, anchor_node, score):
        event_mention_arg = self.construct_event_mention_argument(role, anchor_node, score)
        self.add_event_mention_argument(event_mention_arg)
        return event_mention_arg

    def add_new_event_mention_argument(self, role, serif_em, score):
        event_mention_arg = self.construct_event_mention_argument(role, serif_em, score)
        self.add_event_mention_argument(event_mention_arg)
        return event_mention_arg

    def add_event_mention_argument(self, event_mention_argument):
        self.arguments.append(event_mention_argument)

    def construct_event_mention_argument(self, role, mention_or_val_mention, score):
        from serif.theory.event_mention_arg import EventMentionArg
        event_mention_argument = EventMentionArg(owner=self)
        event_mention_argument.role = role
        event_mention_argument.score = score
        if isinstance(mention_or_val_mention, Mention):
            event_mention_argument.mention = mention_or_val_mention
        elif isinstance(mention_or_val_mention, ValueMention):
            event_mention_argument.value_mention = mention_or_val_mention
        elif isinstance(mention_or_val_mention, SynNode):
            event_mention_argument.anchor_node = mention_or_val_mention
        elif isinstance(mention_or_val_mention, EventMention):
            event_mention_argument.event_mention = mention_or_val_mention
        else:
            raise ValueError
        event_mention_argument.document.generate_id(event_mention_argument)
        return event_mention_argument

    def add_new_event_mention_type(self, em_type, score):
        event_mention_type = self.construct_event_mention_type(em_type, score)
        self.add_event_mention_type(event_mention_type)
        return event_mention_type

    def add_event_mention_type(self, event_mention_type):
        self.event_types.append(event_mention_type)

    def construct_event_mention_type(self, emf_type, score):
        event_mention_type = EventMentionType(owner=self)
        event_mention_type.event_type = emf_type
        event_mention_type.score = score
        return event_mention_type

    def add_new_event_mention_factor_type(self, emf_type, score):
        event_mention_factor_type = self.construct_event_mention_factor_type(emf_type, score)
        self.add_event_mention_factor_type(event_mention_factor_type)
        return event_mention_factor_type

    def add_event_mention_factor_type(self, event_mention_factor_type):
        self.factor_types.append(event_mention_factor_type)

    def construct_event_mention_factor_type(self, emf_type, score):
        event_mention_factor_type = EventMentionFactorType(owner=self)
        event_mention_factor_type.event_type = emf_type
        event_mention_factor_type.score = score
        return event_mention_factor_type
