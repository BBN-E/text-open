import json

from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.util.head_finder import *


class SerifAMRNodeTheory(SerifSequenceTheory):

    @property
    def parse(self):
        """The Parse object that contains this AMRNode"""
        from serif.theory.amr_node import AMRNode
        owner = self.owner
        # TODO: currently every AMRNode is owned simply by AMRParse, not by governing node
        # if isinstance(owner, AMRNode):
        #     return owner.parse
        # else:
        #     return owner
        return owner

    @property
    def sent_no(self):
        return self.parse.sent_no

    @property
    def tokens(self):
        if self.alignment_token_indices is not None:
            alignment_token_indices = json.loads(self.alignment_token_indices)
            if self.parse.token_sequence is not None and len(alignment_token_indices) > 0:
                return [self.parse.token_sequence[i] for i in alignment_token_indices]
        return None

    def _pprint_penman_tree(self, penman_tree):
        '''
        ('w', [
                  ('/', 'want-01~e.0,2'),
                  (':arg0', ('b', [
                                    ('/', 'boy~e.1')
                                  ])),
                  (':arg1', ('b2', [
                                     ('/', 'believe-01~e.6'),
                                     (':arg0', ('g', [
                                                        ('/', 'girl~e.4')
                                                     ])),
                                     (':arg1', 'b')
                                   ]))
              ])
        '''
        varname = penman_tree[0]

        ret = '(%s' % (varname)
        if len(penman_tree) > 1:
            children = penman_tree[1]
            for (relation, child) in children:
                if type(child) == str:
                    ret = ret + ' %s %s' % (relation, child)
                else:
                    ret = ret + ' %s %s' % (relation, self._pprint_penman_tree(child))
        ret = ret + ')'

        return ret

    def pprint_penman_tree(self):
        return self._pprint_penman_tree(json.loads(self._penman_tree))

    def __repr__(self):
        return self.pprint_penman_tree()

    def __str__(self):
        return self.pprint_penman_tree()
