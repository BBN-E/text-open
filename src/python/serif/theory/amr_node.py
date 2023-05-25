from serif.theory.serif_amr_node_theory import SerifAMRNodeTheory
from serif.theory.token import Token
from serif.xmlio import _SimpleAttribute, _ReferenceAttribute, _ReferenceListAttribute, _ChildTheoryElementList


class AMRNode(SerifAMRNodeTheory):
    varname = _SimpleAttribute(is_required=True)  # e.g. w
    content = _SimpleAttribute(is_required=True)  # e.g. want-01

    is_attribute_node = _SimpleAttribute(is_required=True)

    alignment_token_indices = _SimpleAttribute(is_required=False)  # e.g. json.dumps([1,3,4]) -> '["1", "3", "4"]'

    _penman_tree = _SimpleAttribute(str)  # TODO: easier to store penman string for each node than doing recursion (how would we deal with reentrant nodes otherwise?)

    _children = _ReferenceListAttribute('AMRNode')
    _outgoing_amr_rels = _SimpleAttribute(is_required=True)  # store as json, e.g. '[":arg0", ":arg1"]'

    _parents = _ReferenceListAttribute('AMRNode')