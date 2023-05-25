import re, json
import logging
from collections import defaultdict

from serif.theory.serif_theory import SerifTheory
from serif.theory.amr_node import AMRNode
from serif.xmlio import ET

logging.getLogger("penman").setLevel(logging.WARNING)  # silence penman's default logging (logging.INFO)


class SerifAMRParseTheory(SerifTheory):
    def _init_from_etree(self, etree, owner):
        SerifTheory._init_from_etree(self, etree, owner)
        # import pdb; pdb.set_trace()
        # if self.root is None:
        #     if self._amr_string is None:
        #         raise ValueError('Parse requires AMRNode or AMRString')
        #     else:
        self._parse_amr_string()
        # assert self.root is not None# or len(self.token_sequence) == 0

    indent = 0

    def _parse_amr_string(self):
        '''
        Sample AMR penman strings:
        (w / want-01~e.0,2 :arg0 (b / boy~e.1) :arg1 (b2 / believe-01~e.6 :arg0 (g / girl~e.4) :arg1 b))
        (d / delay-01~e.1 :arg0 (c / company :name (n / name :op1 "visa")) :arg1 (d2 / deadline~e.3 :quant 2~e.2) :purpose~e.4 (h / help-01~e.5 :arg0 c :arg1 (c2 / cope-01~e.7 :arg0 (p / person~e.6 :arg0-of~e.6 (m / merchandise-01~e.6)) :arg1 (p2 / pandemic~e.10)) :arg2 p))
        '''

        import penman
        from penman.surface import Alignment
        from penman.models import noop

        # define method within scope of Alignment import
        def get_alignment_for_amr_triple(source, relation, target, g):
            '''
            The triple is of the form (source, relation, target), e.g.:
                - ('d', ':instance', 'delay-01') for an AMR instance
                - ('d2', ':quant', '2')          for an AMR attribute

            :param g: penman.graph.Graph
            :return: json.dumps([a1, a2, ..., ak]) where a1...ak are by-token alignment indices
            '''

            alignment = [e for e in g.epidata[(source, relation, target)] if type(e) == Alignment]
            if len(alignment) == 1:
                alignment = alignment[0]
                return json.dumps(alignment.indices)
            else:
                assert len(alignment) == 0
                return json.dumps([])

        var2node = dict()

        g = penman.decode(self._amr_string, model=noop.NoOpModel())  # decode with NoOpModel to preserve inverted roles such as :arg1-of

        t = penman.parse(self._amr_string)

        var2tree = dict()
        for node in t.nodes():
            var2tree[node[0]] = node

        # root node triple
        (root_varname, _instance, root_content) = [instance for instance in g.instances() if instance.source == g.top][0]  # ('w', ':instance', 'want-01')

        # get root node's alignment
        alignment_token_indices = get_alignment_for_amr_triple(source=root_varname,
                                                               relation=_instance,
                                                               target=root_content,
                                                               g=g)

        # create root AMRNode
        root_amr_node = AMRNode(varname=root_varname, content=root_content, is_attribute_node=False,
                                alignment_token_indices=alignment_token_indices,
                                owner=self)
        root_amr_node._penman_tree = json.dumps(var2tree[root_varname])
        root_amr_node.id = '%s.%s' % (self.id, 0)
        self.root = root_amr_node
        self.document.register_id(root_amr_node)
        var2node[root_varname] = root_amr_node

        # iterate over graph vars and create AMRNodes
        node_count = 1
        for instance in g.instances():
            # e.g.  Instance(source='w', role=':instance', target='want-01')
            varname = instance.source
            _instance = instance.role
            content = instance.target

            # if AMRNode hasn't been created for this variable, then find its alignment and create new AMRNode
            if varname not in var2node:

                # get current node's alignment
                alignment_token_indices = get_alignment_for_amr_triple(source=varname,
                                                                       relation=_instance,
                                                                       target=content,
                                                                       g=g)

                # create new AMRNode
                amr_node = AMRNode(varname=varname, content=content, is_attribute_node=False,
                                   alignment_token_indices=alignment_token_indices,
                                   owner=self)
                amr_node._penman_tree = json.dumps(var2tree[varname])
                amr_node.id = '%s.%s' % (self.id, node_count)
                self.document.register_id(amr_node)
                var2node[varname] = amr_node
                node_count += 1

        # iterate over attributes and create AMRNodes for attributes
        attr_varnames, node_attr_edges = [], []
        for attribute in g.attributes():
            # e.g. Attribute(source='n', role=':op1', target='"visa"')
            source_varname = attribute.source  # n
            attr_type = attribute.role         # :op1
            attr_value = attribute.target      # "visa"

            attr_varname = "|".join([source_varname, attr_type])  # "n__:op1"
            attr_varnames.append(attr_varname)  # "n__:op1"
            node_attr_edges.append((source_varname, attr_type, attr_varname))   # "('n', ':op1', 'n__:op1')"

            # get current node's alignment
            alignment_token_indices = get_alignment_for_amr_triple(source=source_varname,
                                                                   relation=attr_type,
                                                                   target=attr_value,
                                                                   g=g)

            # create new AMRNode for attribute
            amr_node = AMRNode(varname=attr_varname, content=attr_value, is_attribute_node=True,
                               alignment_token_indices=alignment_token_indices,
                               owner=self)
            amr_node._penman_tree = json.dumps([attr_varname, []])  # AMRNode for attribute isn't part of penman so it doesn't have penman tree
            amr_node.id = '%s.%s' % (self.id, node_count)
            self.document.register_id(amr_node)
            var2node[attr_varname] = amr_node
            node_count += 1

        # second pass: add parents and relations
        serifxml_amr_edges = [(e.source, e.role, e.target) for e in g.edges()] + node_attr_edges
        for (source, relation, target) in serifxml_amr_edges:
            # e.g.  ('w', ':ARG0', 'b')     for node-to-node edge
            #       ('n', ':op1', 'n__:op1') for attribute edge, 'n__:op1' is the varname for AMRNode with attr value ("visa") as its content

            parent = var2node[source]
            child = var2node[target]

            parent._children.append(child)
            if not parent._outgoing_amr_rels:
                parent._outgoing_amr_rels = json.dumps([relation.lower()])
            else:
                parent._outgoing_amr_rels = json.dumps(json.loads(parent._outgoing_amr_rels) + [relation.lower()])
            child._parents.append(parent)

    @property
    def sent_no(self):
        if self.owner:
            return self.owner.sent_no
        else:
            return None
