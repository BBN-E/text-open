from serif.theory.enumerated_type import MentionType
from serif.theory.serif_mention_theory import SerifMentionTheory
from serif.theory.syn_node import SynNode
from serif.xmlio import _ReferenceAttribute, _SimpleAttribute


class Mention(SerifMentionTheory):
    syn_node = _ReferenceAttribute('syn_node_id', cls=SynNode,
                                   is_required=True)
    mention_type = _SimpleAttribute(MentionType, is_required=True)
    entity_type = _SimpleAttribute(is_required=True)
    entity_subtype = _SimpleAttribute(default='UNDET')
    is_metonymy = _SimpleAttribute(bool, default=False)
    intended_type = _SimpleAttribute(default='UNDET')
    role_type = _SimpleAttribute(default='UNDET')
    link_confidence = _SimpleAttribute(float, default=1.0)
    confidence = _SimpleAttribute(float, default=1.0)
    parent_mention = _ReferenceAttribute('parent', cls='Mention')
    child_mention = _ReferenceAttribute('child', cls='Mention')
    next_mention = _ReferenceAttribute('next', cls='Mention')
    model = _SimpleAttribute()
    pattern = _SimpleAttribute()
