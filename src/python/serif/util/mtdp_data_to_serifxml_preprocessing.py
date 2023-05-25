import re, os

# from serif.model.document_model import DocumentModel
# from serif.theory.event_mention import EventMention
# from serif.theory.mention import Mention
# from serif.theory.value_mention import ValueMention
# from serif.util.better_serifxml_helper import find_valid_anchors_by_token_index


month_to_num = {"January": 1, "February": 2, "March": 3, "April": 4, "May": 5, "June": 6, "July": 7, "August": 8, "September": 9, "October": 10, "November": 11, "December": 12}
months = set(month_to_num.keys())
days_of_week = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}

class MTDPDataToSerifxmlPreprocessing:

    def __init__(self, **kwargs):
        pass

    def preprocess_docs_file(self,
                             input_docs,
                             dump_dir,
                             txt_paths_outfile,
                             docid_to_mtdp_metadata_path_outfile,
                             docid_to_edge_list_path_outfile,
                             mode="modal"):

        txt_paths = []
        mtdp_metadata_paths = []
        edge_list_paths = []

        docid_to_mtdp_metadata_path = dict()
        docid_to_edge_list_path = dict()

        for docs_file_path in input_docs:
            with open(docs_file_path, "r") as f:
                raw_mtdp_docs = [d.strip() for d in f.read().split("\n\n") if d.strip() != ""]

            for i,raw_mtdp_doc in enumerate(raw_mtdp_docs):

                docid, dct_start, dct_end, text_lines, edge_list = self.raw_mtdp_doc_to_serifxml(raw_mtdp_doc, mode=mode)

                print("====\n{}\n====".format(docid))

                txt_path = os.path.join(dump_dir, docid + ".txt")
                txt_paths.append(txt_path)
                with open(txt_path, "w") as f:
                    f.write("\n".join([" ".join(l).strip() for l in text_lines]).strip())

                mtdp_metadata_path = os.path.join(dump_dir, docid + ".metadata")
                mtdp_metadata_paths.append(mtdp_metadata_path)
                with open(mtdp_metadata_path, "w") as f:
                    f.write("\n".join([docid, dct_start, dct_end]))

                edge_list_path = os.path.join(dump_dir, docid + ".edge_list")
                edge_list_paths.append(edge_list_path)
                with open(edge_list_path, "w") as f:
                    f.write(edge_list)

                docid_to_mtdp_metadata_path[docid] = mtdp_metadata_path
                docid_to_edge_list_path[docid] = edge_list_path

        with open(txt_paths_outfile, "w") as f:
            f.write("\n".join(txt_paths))

        with open(docid_to_mtdp_metadata_path_outfile, "w") as f:
            for k, v in docid_to_mtdp_metadata_path.items():
                f.write(k + "\t" + v + "\n")

        with open(docid_to_edge_list_path_outfile, "w") as f:
            for k, v in docid_to_edge_list_path.items():
                f.write(k + "\t" + v + "\n")

    def raw_mtdp_doc_to_serifxml(self, raw_mtdp_doc, mode="modal"):
        mtdp_text = raw_mtdp_doc.split("EDGE_LIST")[0].strip()
        mtdp_edges = raw_mtdp_doc.split("EDGE_LIST")[-1].strip()
        # mtdp_edges = self.normalize_mtdp_edges(mtdp_edges, mode=mode)
        docid, dct_start, dct_end, text_lines = self.parse_mtdp_text(mtdp_text, mode=mode)
        return docid, dct_start, dct_end, text_lines, mtdp_edges

    def parse_mtdp_text(self, mtdp_text, mode="modal"):

        lines = [l.strip() for l in mtdp_text.split("\n")]

        header_line = lines[0]
        text_lines = [l.split(" ") for l in lines[1:]]

        if mode == "modal":
            docid = re.fullmatch(pattern=r"filename:<doc id=(.*)>:SNT_LIST", string=header_line).group(1)
        elif mode == "temporal":
            docid = re.fullmatch(pattern=r'filename:<doc id="(.*)" url=".*>:SNT_LIST', string=header_line).group(1)

        dct_start, dct_end = None, None
        for i,l in enumerate(lines[:6]):  # let's hope dct line is no later than this
            if re.fullmatch(pattern=r"([12]\d\d\d)-(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d) \+\d\d:\d\d .", string=l) is not None:
                dct_line = l
                dct_start = "T".join(dct_line.split()[:2])
                dct_end = "T".join(dct_line.split()[:2])
                break
            else:                
                dct_tokens = l.split()
                print(dct_tokens)
                if re.fullmatch(pattern=r"(January|February|March|April|May|June|July|August|September|October|November|December) \d\d? , [12]\d\d\d( .)?", string=l):  # May 14 , YYYY
                     month, day, year = month_to_num[dct_tokens[0]], dct_tokens[1], dct_tokens[3]
                     dct_start = "T".join(["{}-{}-{}".format(year, month, day), "00:00:00"])
                     dct_end = "T".join(["{}-{}-{}".format(year, month, day), "23:59:59"])
                     break
                elif re.fullmatch(pattern=r"(January|February|March|April|May|June|July|August|September|October|November|December) \d\d? [12]\d\d\d( .)?", string=l):  # May 14 YYYY
                    month, day, year = month_to_num[dct_tokens[0]], dct_tokens[1], dct_tokens[2]
                    dct_start = "T".join(["{}-{}-{}".format(year, month, day), "00:00:00"])
                    dct_end = "T".join(["{}-{}-{}".format(year, month, day), "23:59:59"])
                    break
                elif re.fullmatch(pattern=r"(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday) , (January|February|March|April|May|June|July|August|September|October|November|December) \d\d? , [12]\d\d\d( .)?", string=l):  # Monday , May 14 , YYYY
                    month, day, year = month_to_num[dct_tokens[2]], dct_tokens[3], dct_tokens[5]
                    dct_start = "T".join(["{}-{}-{}".format(year, month, day), "00:00:00"])
                    dct_end = "T".join(["{}-{}-{}".format(year, month, day), "23:59:59"])
                    break

        assert dct_start is not None
        assert dct_end is not None

        return docid, dct_start, dct_end, text_lines

    def normalize_mtdp_edges(self, mtdp_edges, mode="modal"):
        # deprecated: let's leave original indices and include dct line in text

        edge_list = [l.strip().split() for l in mtdp_edges.split("\n") if l.strip() != ""]
        edge_list = [list([list([int(m) for m in a.split('_')]), b, list([int(n) for n in c.split('_')]), d]) for
                    (a, b, c, d) in edge_list]
        normalized_edge_list = []
        if mode == "temporal": # make sents 0-indexed
            for a, b, c, d in edge_list:
                a_norm = [a[0] - 1 if a[0] > 0 else a[0], a[1], a[2]]
                # if not (a[0] < 0):
                #     a_norm[0] -= 1
                c_norm = [c[0] - 1 if c[0] > 0 else c[0], c[1], c[2]]
                # if not (c[0] < 0):
                #     c_norm[0] -= 1
                normalized_edge_list.append([a_norm, b, c_norm, d])
        elif mode == "modal":
            for a, b, c, d in edge_list:
                if (a[0] > dct_line_index):  # sentence at dct_line_index (which is almost always 1) is always dct which has been removed from text
                    a[0] -= 1
                if (c[0] > dct_line_index):
                    c[0] -= 1
                normalized_edge_list.append([a, b, c, d])
        mtdp_edges = "\n".join(["\t".join(["_".join([str(a[0]), str(a[1]), str(a[2])]), b, "_".join([str(c[0]), str(c[1]), str(c[2])]), d]) for a, b, c, d in normalized_edge_list])
        return mtdp_edges


if __name__ == '__main__':

    modal_docs_file_paths = ["/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal/train1225.txt",
                             "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal/dev1225.txt",
                             "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal/test1225.txt"]

    temporal_docs_file_paths = ["/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal/train_e_te_dct_meta.txt",
                                "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal/dev_e_te_dct_meta.txt",
                                "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal/test_e_te_dct_meta.txt"]

    model = MTDPDataToSerifxmlPreprocessing()
    model.preprocess_docs_file(modal_docs_file_paths,
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.pre_serifxml",
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.txts.list",
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.docid_to_mtdp_metadata_paths",
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.docid_to_edge_list_paths",
                          mode="modal")

    model.preprocess_docs_file(temporal_docs_file_paths,
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal.pre_serifxml",
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal.txts.list",
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal.docid_to_mtdp_metadata_paths",
                         "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/temporal.docid_to_edge_list_paths",
                         mode="temporal")
