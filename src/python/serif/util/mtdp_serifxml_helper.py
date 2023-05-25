import serifxml3


def serif_doc_to_mtdp_input(serif_doc, mtdp_input_file):

    lines = []

    header_line = "filename:<doc id={}>:SNT_LIST".format(serif_doc.docid)
    lines.append(header_line)

    for s in serif_doc.sentences:
        lines.append(" ".join([t.text for t in s.token_sequence]))

    mtdp_input_string = "\n".join(lines)

    with open(mtdp_input_file, "w") as f:
        f.write(mtdp_input_string)


def serif_doc_to_list_of_lists(serif_doc):
    return [[t.text for t in s.token_sequence] for s in serif_doc.sentences]


def extract_edge_list_from_mtdp_output(mtdp_output_file):

    with open(mtdp_output_file, "r") as f:
        mtdp_lines = [l.strip() for l in f.readlines()]

    edge_lines = []
    EDGE_LIST_line_seen = False
    for l in mtdp_lines:
        if EDGE_LIST_line_seen:
            edge_lines.append(edge_line_to_tup(l))
        if l == "EDGE_LIST":
            assert not EDGE_LIST_line_seen
            EDGE_LIST_line_seen = True

    return edge_lines


def edge_line_to_tup(edge_line):
    '''
    "0_0_3   Timex   -1_-1_-1        Depend-on"
    ((0,0,3), "Timex", (-1,-1,-1), "Depend-on")
    '''
    child, node_type, parent, relation = edge_line.split()
    child_tup = tuple(int(n) for n in child.split("_"))
    parent_tup = tuple(int(n) for n in parent.split("_"))
    return (child_tup, node_type, parent_tup, relation)


# if __name__ == '__main__':
#     test_file = "/d4m/ears/expts/48076.aylien.10p.1000.012721.v1/expts/test_pl/doctheory_resolver/split/29/ENG_NW_WM_aylien_69968855_0.xml"
#     test_mtdp_input_file = "./test_mdtp_input_file.txt"
#     # d = serifxml3.Document(test_file)
#     # serifxml_doc_to_mtdp_input(d, test_mtdp_input_file)
#     print(extract_edge_list_from_mtdp_output("/nfs/raid66/u11/users/brozonoy-ad/mtdp/output_temporal/output_dir/bert-base-cased_test_pipeline_stage2_auto_nodes.txt"))
