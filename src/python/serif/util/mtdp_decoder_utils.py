from serif.theory.event_mention import EventMention
from serif.theory.mention import Mention
from serif.theory.value_mention import ValueMention


def get_docid_to_edge_list_dict(docid_to_edge_list_path_file, as_str=True):
    '''
    :param docid_to_edge_list_path_file: e.g. "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.docid_to_edge_list_paths"
    :return:
    '''

    with open(docid_to_edge_list_path_file, "r")  as f:
        lines = [l.strip().split() for l in f.readlines()]
        docid_to_edge_list_path = {l[0]: l[1] for l in lines}

    docid_to_edge_list = dict()
    for docid, edge_list_path in docid_to_edge_list_path.items():
        with open(edge_list_path, "r") as f:
            edge_list = [l.strip().split() for l in f.readlines() if l.strip() != ""]
            if not as_str:  # convert to ints if populating serifxml, else keep as strings for eval
                edge_list = [tuple([tuple([int(m) for m in a.split('_')]), b, tuple([int(n) for n in c.split('_')]), d]) for
                             (a, b, c, d) in edge_list]
        docid_to_edge_list[docid] = edge_list

    return docid_to_edge_list

def create_edge_list_from_serif_doc(serif_doc, data_type="modal", source="mtdp"):

    assert data_type in {'modal', 'time'}
    assert source in {'gold', 'mtdp' }

    edge_list = []

    # n.mode.model will have values (gold|mtdp)_(modal|time)
    mtrms = [m for m in serif_doc.modal_temporal_relation_mention_set if m.node.model == "{}_{}".format(source,data_type)]

    # traverse graph as triples: for each node, look at its children
    for p in mtrms:

        # determine parent node code
        p_node_triple = mtrm_node_2_triple(p.node.value)

        for c in p.children:

            # determine child node code
            c_node_triple = mtrm_node_2_triple(c.node.value)

            edge_list.append((c_node_triple, c.node.modal_temporal_node_type, p_node_triple, c.node.relation_type))

    return edge_list

def mtrm_node_2_triple(mtrm_node):

    node2code = {"ROOT_NODE": "-1_-1_-1",
                 "AUTHOR_NODE": "-3_-3_-3",
                 "NULL_CONCEIVER_NODE": "-5_-5_-5",
                 "DCT_NODE": "-7_-7_-7"}

    if type(mtrm_node) == str:
        return node2code[mtrm_node]

    elif type(mtrm_node) == EventMention:

        if mtrm_node.anchor_node is not None:
            return "_".join([str(mtrm_node.sentence.sent_no), str(mtrm_node.anchor_node.start_token.index()), str(mtrm_node.anchor_node.end_token.index())])
        else:
            return "_".join([str(mtrm_node.sentence.sent_no), str(mtrm_node.semantic_phrase_start), str(mtrm_node.semantic_phrase_end)])

    elif type(mtrm_node) == Mention:

        if mtrm_node.syn_node is not None:
            return "_".join([str(mtrm_node.sentence.sent_no), str(mtrm_node.syn_node.start_token.index()), str(mtrm_node.syn_node.end_token.index())])
        else:
            return "_".join([str(mtrm_node.sentence.sent_no), str(mtrm_node.start_token.index()), str(mtrm_node.end_token.index())])

    elif type(mtrm_node) == ValueMention:

        return "_".join([str(mtrm_node.sentence.sent_no), str(mtrm_node.start_token.index()), str(mtrm_node.end_token.index())])

    else:
        raise TypeError
