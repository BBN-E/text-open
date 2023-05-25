import json


def bfs_over_amr_parse(amr_parse, condition):
    '''

    :param amr_node: serif.theory.amr_node.AMRNode
    :param condition: function from AMRNode to boolean, saying whether to return the AMRNode
    :return:
    '''

    # perform BFS starting from given amr node

    visited = []  # List to keep track of visited nodes.
    queue = []  # Initialize a queue
    ret = []  # List of AMRNodes to return

    visited.append(amr_parse.root.id)
    queue.append(amr_parse.root)

    while queue:

        curr_amr_node = queue.pop(0)

        # TODO DO SOMETHING TO CURRENT AMR NODE
        #  e.g. check that current amr node has desired attribute/value
        if condition(curr_amr_node):
            ret.append(curr_amr_node)

        # iterate over child nodes
        if curr_amr_node._children is not None and curr_amr_node._outgoing_amr_rels is not None:
            for i, (child_amr_node, outgoing_amr_rel) in enumerate(zip(curr_amr_node._children,
                                                                       json.loads(curr_amr_node._outgoing_amr_rels))):

                if child_amr_node.id not in visited:
                    visited.append(child_amr_node.id)
                    queue.append(child_amr_node)

    return ret


def amr_node_has_attribute_value(amr_node, attribute=":polarity", value="-"):
    '''

    :param amr_node: serif.theory.amr_node.AMRNode
    :return: bool
    '''

    # iterate over child nodes
    if amr_node._children is not None and amr_node._outgoing_amr_rels is not None:
        for i, (child_amr_node, outgoing_amr_rel) in enumerate(zip(amr_node._children,
                                                                   json.loads(amr_node._outgoing_amr_rels))):

            # check that current amr node has desired attribute/value
            if child_amr_node.is_attribute_node:
                if outgoing_amr_rel == attribute:
                    if child_amr_node.content == value:
                        return True

    return False


def amr_node_is_event_node(amr_node):
    '''

    :param amr_node: serif.theory.amr_node.AMRNode
    :return: bool
    '''

    argument_relations = {":arg0", ":arg1", ":arg2", ":arg3"}

    if amr_node._outgoing_amr_rels:
        return any(i in argument_relations for i in json.loads(amr_node._outgoing_amr_rels))
    return False


def find_amr_nodes_with_attribute_value(serif_doc, attribute=":polarity", value="-"):

    ret = []
    for s in serif_doc.sentences:
        if s.amr_parse:
            ret.extend(bfs_over_amr_parse(amr_parse=s.amr_parse,
                                          condition=lambda n: amr_node_has_attribute_value(n, attribute, value)))
    return ret


def find_amr_event_nodes_in_sentence(sentence):

    ret = []
    if sentence.amr_parse:
        ret.extend(bfs_over_amr_parse(amr_parse=sentence.amr_parse,
                                      condition=lambda n: amr_node_is_event_node(n)))
    return ret


def find_mentions_aligned_to_amr_node(amr_node):
    '''
    returns all mentions in sentence whose tokens intersect with amr node's tokens

    :param amr_node: serif.theory.amr_node.AMRNode
    :return: list[serif.theory.mention.Mention]
    '''

    amr_aligned_tokens = set(amr_node.tokens) if amr_node.tokens else set()
    mention_set = amr_node.sentence.mention_set if amr_node.sentence.mention_set else []

    return [m for m in mention_set if set(m.tokens).intersection(amr_aligned_tokens)]


def find_value_mentions_aligned_to_amr_node(amr_node):
    '''
    returns all value mentions in sentence whose tokens intersect with amr node's tokens

    :param amr_node: serif.theory.amr_node.AMRNode
    :return: list[serif.theory.value_mention.ValueMention]
    '''

    amr_aligned_tokens = set(amr_node.tokens) if amr_node.tokens else set()
    value_mention_set = amr_node.sentence.value_mention_set if amr_node.sentence.value_mention_set else []

    return [vm for vm in value_mention_set if set(vm.tokens).intersection(amr_aligned_tokens)]


def find_event_mentions_aligned_to_amr_node(amr_node):
    '''
    returns all event mentions in sentence whose tokens intersect with amr node's tokens

    :param amr_node: serif.theory.amr_node.AMRNode
    :return: list[serif.theory.event_mention.EventMention]
    '''

    amr_aligned_tokens = set(amr_node.tokens) if amr_node.tokens else set()
    event_mention_set = amr_node.sentence.event_mention_set if amr_node.sentence.event_mention_set else []

    return [em for em in event_mention_set if set(em.tokens).intersection(amr_aligned_tokens)]


def find_event_mention_args_aligned_to_amr_node(amr_node):
    '''
    returns all event mentions in sentence whose tokens intersect with amr node's tokens

    :param amr_node: serif.theory.amr_node.AMRNode
    :return: list[serif.theory.event_mention_arg.EventMentionArg]
    '''

    amr_aligned_tokens = set(amr_node.tokens) if amr_node.tokens else set()

    event_mention_arg_set = []
    if amr_node.sentence.event_mention_set:
        for em in amr_node.sentence.event_mention_set:
            event_mention_arg_set.extend(em.arguments)

    return [a for a in event_mention_arg_set if set(a.value.tokens).intersection(amr_aligned_tokens)]
