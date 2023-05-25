import logging
import os
import csv

from serif.model.document_model import DocumentModel
from serif.model.event_mention_model import EventMentionModel
from serif.model.mention_model import MentionModel
import serif.util.mtdp_claim_utils as claim_utils
from serif.theory.event_mention import EventMention
from serif.theory.value_mention import ValueMention
from serif.theory.mention import Mention

logger = logging.getLogger(__name__)

def get_event_mention_set (sentence):
    if sentence.event_mention_set is None:
        sentence.add_new_event_mention_set()
    return sentence.event_mention_set

def get_entity_mention_set (sentence):
    if sentence.mention_set is None:
        sentence.add_new_mention_set()
    return sentence.mention_set

class MTDPClaim (DocumentModel):

    def __init__ (self, **kwargs):
        super(MTDPClaim, self).__init__(**kwargs)
        self.do_write_claims = kwargs.get("do_write_claims","false").lower() == "true"
        self.name = kwargs.get("name","pipeline")
        self.use_subgraph_pattern_matching = kwargs.get("use_subgraph_pattern_matching", "false").lower() == "true"
        self.patterns_path = kwargs.get("patterns_path", None)

        self.output_directory = kwargs["argparse"].output_directory
        self.pipeline_org = os.path.join(self.output_directory,
                                         "claim.{}.org".format(self.name))
        logger.info("Opening {}\n".format(self.pipeline_org))
        self.PIPELINE_ORG = open(self.pipeline_org,'w')
        self.PIPELINE_ORG.write("* pipeline events\n")

        self.EMPTY = []
        self.NONEMPTY = []

    def __del__ (self):
        logger.info("MTDPClaim __del__\n")

    def close_org_files (self):
        self.pipeline_report()
        logger.info("Closing {}\n".format(self.pipeline_org))
        self.PIPELINE_ORG.close()

    def load_model (self):
        pass

    def unload_model (self):
        self.close_org_files()

    def process_document (self, serif_doc):
        '''
        1. Discover claims using MTDPFeature
        2. Write to the pipeline-level diagnostic file
        2. Modify serif_doc to record the claims
        3. Write a document-level diagnostic file
        '''

        self.PIPELINE_ORG.write("** Processing document {}\n".format(
            serif_doc.docid))

        if self.use_subgraph_pattern_matching:
            self.find_claims_from_subgraph_pattern_matching(serif_doc)
        else:
            self.find_claims_from_MTDPFeature(serif_doc)

    def find_claims_from_subgraph_pattern_matching(self, serif_doc):
        from subgraph_pattern_matching import graph_builder, decode, match_wrapper

        if self.patterns_path:
            patterns = decode.prepare_serialized_patterns(patterns_json_path=self.patterns_path)
        else:
            patterns = decode.prepare_patterns()

        GB = graph_builder.GraphBuilder(dp=True, amr=True, mdp=True, tdp=False)
        nx_graphs = decode.serif_doc_to_nx_graphs(serif_doc=serif_doc, graph_builder=GB)
        serif_doc_claim_matches = decode.extract_patterns_from_nx_graph(nx_graph=nx_graphs[0], serif_doc=serif_doc,
                                                                        serif_sentence=None, patterns=patterns)

        serif_doc_claim_match_corpus = match_wrapper.MatchCorpus(serif_doc_claim_matches)

        conceiver_event_mtras = serif_doc_claim_match_corpus.to_mtra_pairs(include_pattern_id=True)

        for (conceiver_mtra, event_mtra, pattern_id) in conceiver_event_mtras:
            claim_utils.add_claim_to_serif_doc(event_mtra=event_mtra, conceiver_mtra=conceiver_mtra,
                                               claim_pattern_id=pattern_id)

    def find_claims_from_MTDPFeature(self, serif_doc):

        ###################################
        # Discover the claims
        ###################################

        mtdpFeature = claim_utils.MTDPFeature(serif_doc, isReference=False)

        ###################################
        # Write to the pipeline-level diagnostic file
        ###################################
        mtdpFeature.write_claim_diagnostics (self.PIPELINE_ORG)

        # Gather info to write to pipeline-level diagnostic
        # at unload-model time.
        if len(mtdpFeature.claims) == 0:
            self.EMPTY.append(serif_doc)
        else:
            self.NONEMPTY.append(serif_doc)

        ###################################
        # Modify the serif document
        ###################################
        mtdpFeature.add_claims_to_serif_doc ()

        # for n, claim in enumerate(mtdpFeature.claims):
        #     # A claim in mtdpFeature.claims is a MTDPNode
        #     mtra = claim.data.modal_temporal_relation_argument
        #     if mtra is None:
        #         pass
        #     else:
        #         # One way to represent a claim in serif: Set an attribute
        #         # on the MTRA.  However, it is difficult to extract the
        #         # claimant/claim edges using only this attribute.
        #         mtra.is_aida_claim = True

        #         # Use existing EventMention from ModalTemporalRelationArgument
        #         eventMention = mtra.value

        #         if type(eventMention) != EventMention:
        #             eventMention.claim_role = "CLAIM ANOMALY"
        #         elif claim.parent.data.modal_temporal_relation_argument is None:
        #             eventMention.claim_role = "ORPHAN CLAIM"
        #         else:
        #             # Use existing Mention from parent
        #             mention = claim.parent.data.modal_temporal_relation_argument.value
        #             if isinstance(mention, Mention):
        #                 EventMentionModel.add_new_event_mention_argument(
        #                     eventMention,
        #                     role="has_claimant",
        #                     mention=mention,
        #                     arg_score=1.0)
        #                 mention.claim_role = "CLAIMANT"
        #                 eventMention.claim_role = "CLAIM"
        #             elif isinstance(mention,str):
        #                 # If the mtra.value is string "AUTHOR", there is
        #                 # no Mention in the document corresponding to this
        #                 # ModalTemporalRelationMention. Set claim_role
        #                 # of the EventMention to "AUTHOR CLAIM"
        #                 eventMention.claim_role = "AUTHOR CLAIM"
        #             eventMention.claim_label = mtra.relation_type

        #########################################
        # Write a document-level diagnostic file
        #########################################
        if self.do_write_claims:
            claim_output = os.path.join(self.output_directory, "{}.claim.out.org".format(serif_doc.docid))
            CLAIM_OUTPUT = open(claim_output,'w')
            logger.info("open {}".format(claim_output))
            mtdpFeature.write_org(CLAIM_OUTPUT)
            CLAIM_OUTPUT.close()

    def pipeline_report (self):
        self.PIPELINE_ORG.write("* empty {}\n".format(len(self.EMPTY)))
        for d in self.EMPTY:
            self.PIPELINE_ORG.write("{}\n".format(d.docid))
        self.PIPELINE_ORG.write("* non empty {}\n".format(len(self.NONEMPTY)))
        for d in self.NONEMPTY:
            self.PIPELINE_ORG.write("{}\n".format(d.docid))
