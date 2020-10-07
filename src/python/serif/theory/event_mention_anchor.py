from serif.theory.proposition import Proposition
from serif.theory.serif_theory import SerifTheory
from serif.theory.syn_node import SynNode
from serif.xmlio import _ReferenceAttribute


class EventMentionAnchor(SerifTheory):
    anchor_prop = _ReferenceAttribute('anchor_prop_id',
                                      cls=Proposition)
    anchor_node = _ReferenceAttribute('anchor_node_id',
                                      cls=SynNode)
