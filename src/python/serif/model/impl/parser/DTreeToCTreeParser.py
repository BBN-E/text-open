
from serif.model.parser_model import ParserModel

class DTreeToCTreeParser(ParserModel):
    def __init__(self, **kwargs):
        super(DTreeToCTreeParser, self).__init__(**kwargs)

    def get_parse_info(self, sentence):
        tree = dep_parse_to_constituency_parse(sentence)
        return tree

#####################################################################################################################

class DependencyTree():

    def __init__(self, sentence):
        '''
        :param sentence: serifxml Sentence
        '''
        self.sentence = sentence
        self.root_node = self.build_dependency_tree_from_serifxml_sentence(sentence)

    def build_dependency_tree_from_serifxml_sentence(self, sentence):
        '''
        :param sentence: serifxml Sentence with dependency parse
        :return: DependencyNode root node of constructed tree
        '''
        self.root_node = DependencyNode(token=None, token_idx=-1, head=None, dep_rel=None,
                                        span=[0, len(sentence.token_sequence) - 1], children=[])
        # doesn't contain root node
        self.token_2_node = dict()

        # for tokens that don't get assigned a parent in initialization pass
        unadopted_tokens = set()

        # initialize tokens as nodes, determine children for each node
        for i, token in enumerate(sentence.token_sequence):

            # create node based on token
            node = DependencyNode(token=token,
                                  token_idx=i,
                                  head=None, # need to create head node from token.head first
                                  dep_rel=token.dep_rel,
                                  span=[i,i],
                                  children=[])
            self.token_2_node[token] = node

            ## find parent node for created node ##
            # child of root
            if not token.head and token.dep_rel == 'root':
                node.head = self.root_node
                node.dep_rel = token.dep_rel
                self.root_node.add_child(node)
            # child of node that's already been created
            elif token.head in self.token_2_node.keys():
                node.head = self.token_2_node[token.head]
                node.dep_rel = token.dep_rel
                self.token_2_node[token.head].add_child(node)
            # parent node doesn't exist yet
            else:
                unadopted_tokens.add(token)

        # attach all nodes that weren't attached in first pass
        for token in unadopted_tokens:
            node = self.token_2_node[token]
            node.head = self.token_2_node[node.token.head]
            self.token_2_node[node.token.head].add_child(node)

        # set spans for each node
        for i, token in enumerate(sentence.token_sequence):
            node = self.token_2_node[token]
            self.token_2_node[token].span = self.get_span(node)

        return self.root_node

    def find_leftmost_spanning_token_idx(self, node):
        '''
        :param node: DependencyNode (within DependencyTree object)
        :return: index of node's leftmost spanned token in sentence
        '''
        if not node.children:
            return node.token_idx
        else:
            leftmost_candidates = [self.find_leftmost_spanning_token_idx(c) for c in node.children]
            leftmost_candidates.append(node.token_idx)
            return min(leftmost_candidates)

    def find_rightmost_spanning_token_idx(self, node):
        '''
        :param node: DependencyNode (within DependencyTree object)
        :return: index of node's rightmost spanned token in sentence
        '''
        if not node.children:
            return node.token_idx
        else:
            rightmost_candidates = [self.find_rightmost_spanning_token_idx(c) for c in node.children]
            rightmost_candidates.append(node.token_idx)
            return max(rightmost_candidates)

    def get_span(self, node):
        '''
        :param node: DependencyNode (within DependencyTree object)
        :return: [span_left, span_right]
        '''
        return [self.find_leftmost_spanning_token_idx(node), self.find_rightmost_spanning_token_idx(node)]

    def get_all_spans(self):
        '''
        :return: list of (token_idx, (span_left, span_right))
                 [(0, (0_left, 0_right)), (1, (1_left, 1_right)), ...]
        '''
        return (sorted([(i, tuple(self.get_span(node)))
                        for i,node in enumerate(self.linearize())], key=lambda x: x[0]))

    def linearize(self):
        '''
        :return: list[DependencyNodes] in linear order
        '''
        return sorted(self.token_2_node.values())

    def to_conllu(self):
        '''
        :return: conllu-formatted sentence
        '''
        return '\n'.join((node.to_conllu(return_string=True)) for i,node in enumerate(self.linearize()))


#####################################################################################################################

class DependencyNode():

    # TODO getter and setter class methods
    def __init__(self, token, token_idx, head, dep_rel, span, children, upos=None, xpos=None):
        '''
        :type token:        serif.theory.token.Token
        :type token_idx:    int
        :type head:         DependencyNode or None
        :type dep_rel:      string or None
        :type span:         list[int, int]
        :param children:    list[DependencyNode]
        '''
        self.token = token
        self.token_idx = token_idx
        self.head = head
        self.dep_rel = dep_rel
        self.span = span
        self.children = children
        self.upos = upos
        self.xpos = xpos

    # TODO ordered list of children
    def add_child(self, node):
        self.children.append(node)

    def left_children(self):
        return sorted([c for c in self.children if c < self])

    def right_children(self):
        return sorted([c for c in self.children if c > self])

    def to_conllu(self, return_string=False):
        '''
        :type token: serif.theory.token.Token
        :return: dict with token's (node's) attributes
        '''
        attributes = dict()
        attributes["ID"] = self.token.index() + 1 # note conllu indexation starts from 1, internal indexing from 0
        attributes["FORM"] = self.token.text
        attributes["LEMMA"] = '_'
        attributes["UPOS"] = self.token.upos
        attributes["XPOS"] = self.token.xpos
        attributes["FEATS"] = '_'
        attributes["HEAD"] = self.token.head.index() + 1 if self.token.head else '_'
        attributes["DEPREL"] = self.token.dep_rel
        attributes["DEPS"] = '_'
        attributes["MISC"] = '_'
        if return_string:
            return "\t".join([str(attributes["ID"]), attributes["FORM"], attributes["LEMMA"], attributes["UPOS"], attributes["XPOS"],
                             attributes["FEATS"], str(attributes["HEAD"]), attributes["DEPREL"], attributes["DEPS"], attributes["MISC"]])
        return attributes

    # rich comparison methods, compare by token index in sentence (assume same sentence)
    def __lt__(self, other): return self.token_idx < other.token_idx
    def __le__(self, other): return self.token_idx <= other.token_idx
    def __eq__(self, other): return self.token_idx == other.token_idx
    def __ne__(self, other): return self.token_idx != other.token_idx
    def __gt__(self, other): return self.token_idx > other.token_idx
    def __ge__(self, other): return self.token_idx >= other.token_idx

    def __str__(self):
        token = self.token.text if self.token else None
        head =  self.head.token.text if self.head and self.head.token else None
        return "token {}\ntoken_idx {}\nspan {}\nhead {}\nchildren {}\n".format(token,
                                                                                self.token_idx,
                                                                                self.span,
                                                                                head,
                                                                                sorted([c.token_idx for c in self.children]))


###################################################################################################################
#########                   DTREE TO CTREE CONVERSION ALGORITHM METHODS                             ###############
###################################################################################################################


# OBSOLETE, use DependencyTree.get_all_spans() instead
# def dep_parse_to_dep_spans(sentence):
#     '''
#     :param sentence: that has dependency parse but no constituency parse
#     :return: [(token_idx, (span_start, span_end)), ...]
#     '''
#
#     # start with no assumptions about token span
#     token_2_dep_span = {token:[i,i] for i,token in enumerate(sentence.token_sequence)}
#
#     # 1. determine dependency span of each token
#     for i, token in enumerate(sentence.token_sequence):
#
#         head_token = token.head
#
#         # if we're at the root token, make it span entire token sequence
#         if not token.head and token.dep_rel == 'root':
#             token_2_dep_span[token] = [0, len(sentence.token_sequence)-1]
#             continue
#
#         # expand head token's dep span based on whether current token is left or right child
#         # current token is left child
#         if i < token_2_dep_span[head_token][0]:
#             token_2_dep_span[head_token][0] = i
#         # current token is right child
#         elif i > token_2_dep_span[head_token][1]:
#             token_2_dep_span[head_token][1] = i
#
#     dep_spans = [tuple([i, tuple(token_2_dep_span[token])]) for i,token in enumerate(sentence.token_sequence)]
#     return dep_spans


###################################################################################################################

# TODO complete list of projections
upos_projections = {"NOUN"  : "NP",
                    "PROPN" : "NP",
                    "VERB"  : "VP",
                    "ADJ"   : "ADJP"}

###################################################################################################################

def dep_parse_to_constituency_parse(sentence):
    '''
    :param sentence: that has dependency parse but no constituency parse
    :return: constituency parse for this sentence (minimally representing NP's accurately)
    Based on simple algorithm from "Language Independent Dependency to Constituent Tree Conversion", p.423
    '''

    #dep_span_tups = dep_parse_to_dep_spans(sentence)
    dtree = DependencyTree(sentence)
    dep_span_tups = dtree.get_all_spans()

    # initialize root's span to contain all other spans
    root_dep_tup = (-1, (-1, len(sentence.token_sequence)))

    # memoization
    completed_spans = dict()

    # remove from set as you construct to avoid repeating work
    unconstructed_spans = set(dep_span_tups)
    unconstructed_spans.add(root_dep_tup)

    parse, completed_spans, unconstructed_spans = dep_span_to_constituent(sentence, root_dep_tup, dep_span_tups, completed_spans, unconstructed_spans)

    return parse

###################################################################################################################

def dep_span_to_constituent(sentence, dep_span_tup, dep_span_tups, completed_spans, unconstructed_spans):

    token_idx, (start, end) = dep_span_tup

    head_token = sentence.token_sequence[token_idx]
    head_upos = head_token.upos
    #head_xpos = head_token.xpos
    head_text = head_token.text

    head_projection = upos_projections[head_upos] if head_upos in upos_projections else head_upos

    # dynamic programming -- if already solved, return
    if dep_span_tup in completed_spans:
        syn_node = completed_spans[dep_span_tup]
        return syn_node, completed_spans, unconstructed_spans

    # single-word span, base case
    elif start == end:

        # get syn_node for single-word span based on POS
        syn_node = "({} {})".format(head_upos, head_text)

        # update memoization
        completed_spans[dep_span_tup] = syn_node
        unconstructed_spans.remove(dep_span_tup)

        return syn_node, completed_spans, unconstructed_spans

    # multi-token span
    else:

        # find syn_nodes of all subspans
        child_tups = find_all_children(dep_span_tup, dep_span_tups, sentence)
        left_child_syn_nodes = []
        right_child_syn_nodes = []

        # recursively construct syn_node for each child (left or right)
        for child_tup in child_tups:

            child_idx, span = child_tup
            child_syn_node, completed_spans, unconstructed_spans = dep_span_to_constituent(sentence, child_tup, dep_span_tups, completed_spans, unconstructed_spans)

            if child_idx < token_idx:
                left_child_syn_nodes.append(child_syn_node)
            else: # if subspan_idx > token_idx:
                right_child_syn_nodes.append(child_syn_node)

        # construct syn_node based on head POS and child syn_nodes
        if token_idx == -1:
            syn_node = "(S {})".format(" ".join(right_child_syn_nodes)) # root only has right children
        else:
            syn_node = "({} {} {} {})".format(head_projection, " ".join(left_child_syn_nodes), "({} {})".format(head_upos, head_text), " ".join(right_child_syn_nodes))

        # update memoization
        completed_spans[dep_span_tup] = syn_node
        unconstructed_spans.remove(dep_span_tup)

        return syn_node, completed_spans, unconstructed_spans

###################################################################################################################

def find_all_children(dep_span_tup, dep_span_tups, sentence):
    '''
    :param dep_span_tup: (token_idx, (start, end))
    :param dep_span_tups: list for all tokens in sentence
    :return: tups for all immediate children of node represented by dep_span_tup
    '''

    curr_idx, curr_span = dep_span_tup

    # first find all subspans
    all_subspan_tups = find_all_subspans(dep_span_tup, dep_span_tups)

    # then filter out those subspans which aren't immediate children
    all_children_tups = []
    for subspan_tup in all_subspan_tups:

        subspan_token_idx, subspan = subspan_tup

        # determine if this subspan's head is actually the current token
        subspan_token = sentence.token_sequence[subspan_token_idx]
        subspan_head_token = subspan_token.head

        if subspan_head_token:
            subspan_head_token_idx = subspan_head_token.index()
        else:
            subspan_head_token_idx = -1 # if child of root

        if subspan_head_token_idx == curr_idx:
            all_children_tups.append(subspan_tup)

    return all_children_tups

###################################################################################################################

def find_all_subspans(dep_span_tup, dep_span_tups):
    '''
    Since the tree is non-projective, dep-spans form a partially-ordered set?????
    :param dep_span_tup: (idx, (start, end))
    :param dep_span_tups: list of dep_span_tups
    :return: all subspans (excluding current)
    '''

    subspan_tups = []
    token_idx, span = dep_span_tup

    for other_idx, other_span in dep_span_tups:
        if contains(span, other_span) and span != other_span:
            subspan_tups.append((other_idx, other_span))

    return subspan_tups

###################################################################################################################

def contains(span_1, span_2):
    '''
    :param span_1: (start, end) tuple
    :param span_2: (start, end) tuple
    :return: span_1 contains_span_2 (bool)
    '''
    start_1, end_1 = span_1
    start_2, end_2 = span_2

    return start_1 <= start_2 and end_2 <= end_1

###################################################################################################################

if __name__ == '__main__':
    import serifxml3
    doc = serifxml3.Document("/nfs/raid66/u11/users/brozonoy/text-open/src/python/test/rundir/output/mini.xml")
    #dep_spans = dep_parse_to_dep_spans(s0)
    #print(dep_spans)
    #print(find_all_subspans((11, (10,14)), dep_spans))

    dt1 = DependencyTree(doc.sentences[0])
    print(len(dt1.sentence.token_sequence))
    #print(dt1.token_2_attributes(dt1.sentence.token_sequence[0], return_string=True))
    #print(dt1.to_conllu())
    print([n.token_idx for n in dt1.linearize()])
    print(dt1.to_conllu())
    print(dt1.get_all_spans())
#    for i in range(20):
#        parse = dep_parse_to_constituency_parse(doc.sentences[i])
#        print(parse)
#        print()
    #t0 = DependencyTree(s0)
    print(dep_parse_to_constituency_parse(doc.sentences[0]))