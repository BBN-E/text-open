import argparse
import os
import logging
import statistics
import serifxml3
from serif.util.mtdp_decoder_utils import get_docid_to_edge_list_dict, create_edge_list_from_serif_doc

logger = logging.getLogger(__name__)

class MTDPScorer:
    def __init__ (self):
        self.DocumentIds = [] # list of docids
        self.ReferenceEdges = {} # docid -> list of edges
        self.CandidateEdges = {} # docid -> list of edges
        self.Score = {} # docid -> maps of stats

    def get_reference (self, docid, method):
        return method (docid, self.ReferenceEdges)

    def get_candidate (self, docid, method):
        return method (docid, self.CandidateEdges)

    def get_labeled (self, docid, edgeDict):
        # score equivalence on childId, relationLabel, parentId
        return set([(edge[0],edge[1],edge[2]) for edge in edgeDict[docid]])

    def get_unlabeled (self, docid, edgeDict):
        # score equivalence on childId, relationLabel
        return set([(edge[0], edge[1]) for edge in edgeDict[docid]])

    def get_rel_only (self, docid, edgeDict):
        # score equivalence on childId, parentId
        return set([(edge[0],edge[2]) for edge in edgeDict[docid]])


    ###########################################################################
    # Score a candidate SerifXML document against a reference SerifXML document
    ###########################################################################
    def score_serifxml_pair (self, docid, refDoc, canDoc):
        refEdges = create_edge_list_from_serif_doc(refDoc, source="gold")
        canEdges = create_edge_list_from_serif_doc(canDoc, source="mtdp")
        self.score_edges_pair (docid, refEdges, canEdges)

    ###########################################################################
    # Score a list of candidate tuples against a list of reference tuples
    # In the tuples on the lists must have the same form in order to match
    # ('-3_-3_-3', 'Conceiver', '-1_-1_-1', 'Depend-on')
    ###########################################################################
    def score_edges_pair (self, docid, refEdges, canEdges):
        logger.info("Document {}".format(docid))
        # logger.info("Size of refEdges {}".format(len(refEdges)))
        # logger.info("Size of canEdges {}".format(len(canEdges)))
        # logger.info("Type of refEdge elements {}".format(type(refEdges[0])))
        # logger.info("Type of refEdge element members {}".format(
        #     " ".join([repr(type(x)) for x in refEdges[0]])))
        # logger.info("Sample refEdge element {}".format(repr(refEdges[0])))
        # logger.info("Type of canEdge elements {}".format(type(canEdges[0])))
        # logger.info("Type of canEdge element members {}".format(
        #     " ".join([repr(type(x)) for x in canEdges[0]])))
        # logger.info("Sample canEdge element {}".format(repr(canEdges[0])))

        self.ReferenceEdges[docid] = set(refEdges)
        self.CandidateEdges[docid] = set(canEdges)
        self.DocumentIds.append(docid)

        for docid in self.DocumentIds:
            self.Score[docid] = {}
            for metric in ["labeled","unlabeled","rel_only"]:
                self.Score[docid][metric] = {}


        for docid in sorted(self.DocumentIds):
            # Score equivalence on childId, relationLabel, parentId
            metric = "labeled"
            reference = self.get_reference(docid, self.get_labeled)
            candidate = self.get_candidate(docid, self.get_labeled)
            truePos = reference.intersection(candidate)
            falsePos = candidate.difference(reference)
            falseNeg = reference.difference(candidate)
            for name, edges in [("TP", truePos), 
                                ("FP", falsePos),
                                ("FN", falseNeg)]:
                self.Score[docid][metric][name] = edges

            # Score equivalence on childId, relationLabel
            metric = "unlabeled"
            reference = self.get_reference(docid, self.get_unlabeled)
            candidate = self.get_candidate(docid, self.get_unlabeled)
            truePos = reference.intersection(candidate)
            falsePos = candidate.difference(reference)
            falseNeg = reference.difference(candidate)
            for name, edges in [("TP", truePos), 
                                ("FP", falsePos),
                                ("FN", falseNeg)]:
                self.Score[docid][metric][name] = edges
        
            # Score equivalence on childId, parentId
            metric = "rel_only"
            reference = self.get_reference(docid, self.get_rel_only)
            candidate = self.get_candidate(docid, self.get_rel_only)
            truePos = reference.intersection(candidate)
            falsePos = candidate.difference(reference)
            falseNeg = reference.difference(candidate)
            for name, edges in [("TP", truePos), 
                                ("FP", falsePos),
                                ("FN", falseNeg)]:
                self.Score[docid][metric][name] = edges

    ###########################################################################
    # Alternate way of scoring. Load all the references at once, then
    # score a single candidate SerifXML document against the edges
    # for the document. This is a closer match to what happens as 
    # mtdp_decoder is used in a pyserif pipeline
    ###########################################################################
    def load_references (self, docid_to_edge_list_path_file):
        self.docid_to_edge_list = get_docid_to_edge_list_dict(
            docid_to_edge_list_path_file,
            as_str=True)
        # The values in the dict are lists of lists.  Convert them to
        # lists of tuples, to be hashable
        for docid in self.docid_to_edge_list:
            self.docid_to_edge_list[docid] = [tuple(x) for x in self.docid_to_edge_list[docid]]

    # Score a candidate serifxml document against previously loaded reference
    def score_serifxml_candidate (self, docid, canDoc):
        refEdges = self.docid_to_edge_list[docid]
        canEdges = create_edge_list_from_serif_doc(canDoc, source="mtdp")
        self.score_edges_pair (docid, refEdges, canEdges)

    # Score a candidate edge list against previously loaded reference
    def score_edgelist_candidate (self, docid, canEdges):
        refEdges = self.docid_to_edge_list[docid]
        self.score_edges_pair (docid, refEdges, canEdges)
        
    def true_positive (self, docid, metric):
        return len(self.Score[docid][metric]["TP"])

    def false_positive (self, docid, metric):
        return len(self.Score[docid][metric]["FP"])

    def false_negative (self, docid, metric):
        return len(self.Score[docid][metric]["FN"])

    def precision (self, docid, metric):
        if "precision" in self.Score[docid][metric]:
            return self.Score[docid][metric]["precision"]
        TP = len(self.Score[docid][metric]["TP"])
        FP = len(self.Score[docid][metric]["FP"])
        if (TP + FP) == 0:
            self.Score[docid][metric]["precision"] = 0
            return 0
        else:
            p = TP/(TP+FP)
            self.Score[docid][metric]["precision"] = p
            return p

    def recall (self, docid, metric):
        if "recall" in self.Score[docid][metric]:
            return self.Score[docid][metric]["recall"]
        TP = len(self.Score[docid][metric]["TP"])
        FN = len(self.Score[docid][metric]["FN"])
        if (TP + FN) == 0:
            self.Score[docid][metric]["recall"] = 0
            return 0
        else:
            r = TP/(TP+FN)
            self.Score[docid][metric]["recall"] = r
            return r

    def f_score (self, docid, metric):
        if "f_score" in self.Score[docid][metric]:
            return self.Score[docid][metric]["f_score"]
        p = self.precision(docid,metric)
        r = self.recall(docid,metric)
        if (p + r) == 0:
            self.Score[docid][metric]["f_score"] = 0
            return 0
        else:
            f = 2 * p * r / (p + r)
            self.Score[docid][metric]["f_score"] = f
            return f

    def micro_average (self, metric):
        tpSum = sum([self.true_positive(x,metric) for x in self.DocumentIds])
        fpSum = sum([self.false_positive(x,metric) for x in self.DocumentIds])
        fnSum = sum([self.false_negative(x,metric) for x in self.DocumentIds])
        if (tpSum + fpSum) == 0:
            micro_p = 0
        else:
            micro_p = tpSum/(tpSum+fpSum)
        if (tpSum + fnSum) == 0:
            micro_r = 0
        else:
            micro_r = tpSum/(tpSum+fnSum)
        if (micro_p + micro_r) == 0:
            micro_f = 0
        else:
            micro_f = 2 * micro_p * micro_r / (micro_p + micro_r)
        return micro_p, micro_r, micro_f
          
    def macro_average (self, metric):
        # macro_p = statistics.mean([self.precision(x,metric) for x in self.DocumentIds])
        # macro_r = statistics.mean([self.recall(x,metric) for  x in self.DocumentIds])
        # macro_f = statistics.mean([self.recall(x,metric) for  x in self.DocumentIds])
        macro_p = sum([self.precision(x,metric) for x in self.DocumentIds])/len(self.DocumentIds)
        macro_r = sum([self.recall(x,metric) for  x in self.DocumentIds])/len(self.DocumentIds)
        macro_f = sum([self.f_score(x,metric) for  x in self.DocumentIds])/len(self.DocumentIds)

        return macro_p, macro_r, macro_f

    def write_org (self, output):
        ORG = open(output,'w')
        for docid in self.DocumentIds:
            ORG.write("* {}\n".format(docid))
            ORG.write("reference edges {}\n".format(len(self.ReferenceEdges[docid])))
            ORG.write("candidate edges {}\n".format(len(self.CandidateEdges[docid])))
            for metric in ["unlabeled","rel_only","labeled"]:
                ORG.write("** {}\n".format(metric))
                ORG.write("true_p = {}, false_p = {}, false_n = {}\n".format(
                    self.true_positive(docid,metric),
                    self.false_positive(docid,metric),
                    self.false_negative(docid,metric)))
                ORG.write("p = {:.3f} r = {:.3f} f = {:.10f}\n".format(
                    self.precision(docid,metric),
                    self.recall(docid,metric),
                    self.f_score(docid,metric)))
        ORG.write("* macro average\n")
        for metric in ["unlabeled","rel_only","labeled"]:
            macro_p, macro_r, macro_f = self.macro_average(metric)
            ORG.write("** {}\n".format(metric))
            ORG.write("p = {:.3f} r = {:.3f} f = {:.10f}\n".format(
                macro_p, macro_r, macro_f))
            ORG.write("macro average f {:.10f} / {} = {:.10f}\n".format(
                sum([self.f_score(x,metric) for  x in self.DocumentIds]),
                len(self.DocumentIds),
                macro_f))
        ORG.write("* micro average\n")
        for metric in ["unlabeled","rel_only","labeled"]:
            micro_p, micro_r, micro_f = self.micro_average(metric)
            ORG.write("** {}\n".format(metric))
            ORG.write("p = {:.3f} r = {:.3f} f = {:.10f}\n".format(
                micro_p, micro_r, micro_f))
        ORG.close()
        
                


def main (reference_dir, candidate_dir, docids, output, edgelists):
    mtdp_scorer = MTDPScorer()

    if edgelists is None:
        with open(docids) as DOCIDS:
            for line in DOCIDS:
                docid = line.rstrip()
                refFile = os.path.join(reference_dir, "{}.xml".format(docid))
                canFile = os.path.join(candidate_dir, "{}.xml".format(docid))
                refDoc = serifxml3.Document(refFile)
                canDoc = serifxml3.Document(canFile)
                mtdp_scorer.score_serifxml_pair(docid, refDoc, canDoc)
        mtdp_scorer.write_org(output)
    else:
        mtdp_scorer.load_references(edgelists)
        with open(docids) as DOCIDS:
            for line in DOCIDS:
                docid = line.rstrip()
                canFile = os.path.join(candidate_dir, "{}.xml".format(docid))
                canDoc = serifxml3.Document(canFile)
                mtdp_scorer.score_serifxml_candidate(docid, canDoc)
        mtdp_scorer.write_org(output)
        

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("reference_dir")
    parser.add_argument("candidate_dir")
    parser.add_argument("docids")
    parser.add_argument("output")
    parser.add_argument("--edgelists")
    args = parser.parse_args()

    main (args.reference_dir, args.candidate_dir, args.docids, args.output, args.edgelists)
