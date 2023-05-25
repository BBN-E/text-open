import functools
import logging
import time
import os

# from mtdp.decode import Decoder
from mtdp.decode_xlm import Decoder as DecoderXLM
from mtdp.eval import eval_stage1, eval_all, get_unlabeled_tups, get_rel_only_tups, get_labeled_tups

from serif.model.document_model import DocumentModel
from serif.model.corpus_model import CorpusModel
from serif.theory.event_mention import EventMention
from serif.theory.mention import Mention
from serif.theory.value_mention import ValueMention
from serif.util.better_serifxml_helper import find_valid_anchors_by_token_index
from serif.util.mtdp_decoder_utils import get_docid_to_edge_list_dict, create_edge_list_from_serif_doc
import serif.model.impl.mtdp_scorer as mtdp_scorer

globalLogger = logging.getLogger(__name__)
# logger that writes only to the log file, not to the
# console. Use a name that doesn't share prefix with
# __name__ to ensure that log messages from other modules
# don't get into the mtdp-only log file.
mtdpLogger = logging.getLogger("not_" + __name__)
mtdpLogger.propagate = False

DUMMY_EVENT_ID = "MTDP_EVENT"
DUMMY_EVENT_SIP_ID = "MTDP_EVENT_SIP"
DUMMY_CONCEIVER_ID = "MTDP_CONCEIVER"


def timer(func):
    '''Print the runtime of the decorated function'''

    @functools.wraps(func)
    def wrapper_timer(*args, **kwargs):
        start_time = time.perf_counter()
        value = func(*args, **kwargs)
        end_time = time.perf_counter()
        run_time = end_time - start_time
        mtdpLogger.info(f"Finished {func.__name__!r} in {run_time:.4f} secs")
        return value

    return wrapper_timer


class MTDPDecoder(DocumentModel):
    node_type_to_obj = {"Event": EventMention,
                        "Conceiver": Mention,
                        "Timex": ValueMention}

    def __init__(self, data_type, pretrained_model_dir=None, mode="use_decoder", docid_to_edge_list_path_file=None, **kwargs):
        '''
        :param data_type: "modal" or "time"
        :param pretrained_model_dir: e.g. "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/xlm_modal/"
        :param mode: "use_decoder" when using actual model; "use_gold_data" for converting mtdp data to serifxml format
        :param docid_to_edge_list_path_file: e.g. "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.docid_to_edge_list_paths"
                                             only for converting gold data to serifxml, i.e. when mode == "use_gold_data"
        :param kwargs:
        '''
        super(MTDPDecoder, self).__init__(**kwargs)
        self.data_type = data_type
        self.pretrained_model_dir = pretrained_model_dir
        self.mode = mode
        # parameters from pyserif config file
        self.do_eval = kwargs.get("do_eval", "false").lower() == "true"
        self.do_integration_overlap = kwargs.get("do_integration_overlap","false").lower() == "true"
        self.do_write_mtdp_output = kwargs.get("do_write_mtdp_output","false").lower() == "true"
        self.do_write_mtdp_eval = kwargs.get("do_write_mtdp_eval","false").lower() == "true"
        # parameters from argparse of pipeline.py
        self.output_directory = kwargs["argparse"].output_directory

        if self.mode == "use_decoder":

            assert self.pretrained_model_dir is not None and os.path.exists(self.pretrained_model_dir)

        self.docid_to_edge_list_path_file = docid_to_edge_list_path_file

        if (self.mode == "use_gold_data" or self.do_eval) and self.docid_to_edge_list_path_file is not None:
            self.docid_to_edge_list = get_docid_to_edge_list_dict(self.docid_to_edge_list_path_file, as_str=self.do_eval)

        # Logging. Don't pass logging events to parent loggers, i.e. write
        # to console. Instead create a log file in output_directory and write
        # to this file. Log file will be in emacs org mode format.

        if not os.path.isdir(self.output_directory):
            os.makedirs(self.output_directory)
        self.logFile = os.path.join(self.output_directory, "mtdp.log.org")
        globalLogger.info("MTDP logfile: {}".format(self.logFile))
        fh = logging.FileHandler(self.logFile, mode='w')
        formatter = logging.Formatter(
            fmt='%(message)s [%(asctime)s] {P%(process)d:%(module)s:%(lineno)d} ' +
            '%(levelname)s',
            datefmt='%m/%d/%Y %H:%M:%S')
        fh.setFormatter(formatter)
        mtdpLogger.addHandler(fh)

        # Keep a list of which SerifXML documents fail in MTDP
        self.doc_failure = []
        # Keep a list of which MTDP edges succeed to become ModalTemporalRelationMentions
        # and which do not
        self.mtrm_success = []
        self.mtrm_failure = []
        self.event = []
        self.event_mention = []
        self.mention = []
        self.value_mention = []
        self.decode_mtdp_edge_count = 0
        self.integrate_mtdp_edge_count = 0
        # Keep a list of times when overlap fails and when overlap succeeds
        # tuples (serif_doc, mtdp text span)
        self.overlap_failure = []
        self.overlap_success = []
        self.cache_success = []
        self.create_node = []
        

    def load_model(self):
        if self.mode == "use_decoder":
            self.decoder = DecoderXLM(self.pretrained_model_dir, data_type=self.data_type)

    def unload_model(self):
        if self.mode == "use_decoder":
            del self.decoder
            self.decoder = None

        # Report the SerifXML documents that fail in MTDP
        mtdpLogger.info("* MTDP Failures ({})".format(len(self.doc_failure)))
        for x in self.doc_failure:
            mtdpLogger.info("MTDP failed on document {}".format(x))
        mtdpLogger.info("* Decode MTDP Edge Count ({})".format(self.decode_mtdp_edge_count))
        mtdpLogger.info("* Integrate MTDP Edge Count ({})".format(self.integrate_mtdp_edge_count))
        mtdpLogger.info("* MTRM Success ({})".format(len(self.mtrm_success)))
        for x in self.mtrm_success:
            mtdpLogger.info("MTRM created for edge {}".format(x))
        mtdpLogger.info("* MTRM Failure ({})".format(len(self.mtrm_failure)))
        for x in self.mtrm_failure:
            mtdpLogger.info("MTRM failed for edge {}".format(x))
        mtdpLogger.info("* Event ({})".format(len(self.event)))
        mtdpLogger.info("* EventMention ({})".format(len(self.event_mention)))
        mtdpLogger.info("* Mention ({})".format(len(self.mention)))
        mtdpLogger.info("* ValueMention ({})".format(len(self.value_mention)))
        
        mtdpLogger.info("* Cache Success ({})".format(len(self.cache_success)))
        for (serif_doc, mtdp_tup) in self.cache_success:
            mtdpLogger.info("{} {}".format(serif_doc.docid, mtdp_tup))
        mtdpLogger.info("* Overlap Success ({})".format(len(self.overlap_success)))
        for (serif_doc, mtdp_tup) in self.overlap_success:
            mtdpLogger.info("{} {}".format(serif_doc.docid, mtdp_tup))
        mtdpLogger.info("* Overlap Failure ({})".format(len(self.overlap_failure)))
        for (serif_doc, mtdp_tup) in self.overlap_failure:
            mtdpLogger.info("{} {}".format(serif_doc.docid, mtdp_tup))
        mtdpLogger.info("* Create Node ({})".format(len(self.create_node)))
        for (serif_doc, mtdp_tup) in self.create_node:
            mtdpLogger.info("{} {}".format(serif_doc.docid, mtdp_tup))

        globalLogger.info("MTDP logfile: {}".format(self.logFile))
        

    @timer
    def get_snt_start_end_to_obj_cache(self, serif_doc):
        '''returns dictionary of {(snt_idx, start_tok, end_tok): serifxml_object},
        where serifxml_object is of type EventMention, Mention or ValueMention'''

        snt_start_end_to_obj = dict()
        for i, s in enumerate(serif_doc.sentences):

            # populate event mentions
            for event_mention in s.event_mention_set or ():
                if event_mention.anchor_node is not None:
                    start_token_index, end_token_index = event_mention.anchor_node.start_token.index(), event_mention.anchor_node.end_token.index()
                else:
                    start_token_index, end_token_index = event_mention.semantic_phrase_start, event_mention.semantic_phrase_end
                snt_start_end_to_obj[(i, start_token_index, end_token_index)] = event_mention

            # populate entity mentions
            for mention in s.mention_set or ():
                if mention.syn_node is not None:
                    start_token_index, end_token_index = mention.syn_node.start_token.index(), mention.syn_node.end_token.index()
                else:
                    start_token_index, end_token_index = mention.start_token.index(), mention.end_token.index()
                snt_start_end_to_obj[(i, start_token_index, end_token_index)] = mention

            # populate time value mentions
            for value_mention in s.value_mention_set or ():
                if value_mention.value_type == "TIMEX2.TIME":
                    # TODO why does value mention not have a syn_node?
                    snt_start_end_to_obj[
                        (i, value_mention.start_token.index(), value_mention.end_token.index())] = value_mention

        # populate entity mentions
        if serif_doc.entity_set:
            for entity in serif_doc.entity_set:
                for mention in entity.mentions:
                    if mention.syn_node is not None:
                        start_token_index, end_token_index = mention.syn_node.start_token.index(), mention.syn_node.end_token.index()
                    else:
                        start_token_index, end_token_index = mention.start_token.index(), mention.end_token.index()
                    snt_start_end_to_obj[(mention.sent_no, start_token_index, end_token_index)] = mention

        # populate time value mentions
        if serif_doc.value_set:
            for value in serif_doc.value_set:
                if value.value_mention.value_type == "TIMEX2.TIME":
                    snt_start_end_to_obj[(value.value_mention.sent_no, value.value_mention.start_token.index(),
                                          value.value_mention.end_token.index())] = value.value_mention

        return snt_start_end_to_obj

    @timer
    def decode_mtdp(self, serif_doc):
        '''runs parser over text and returns parse structure as list of tuples'''

        decoder_input = [[[t.text for t in s.token_sequence] for s in
                          serif_doc.sentences]]  # wrapped as list of docs, each doc is list of sents, each sent is list of toks
        edge_list = self.decoder.decode(decoder_input, data_type=self.data_type)[0]  # extract doc parse from list of length 1

        if self.do_write_mtdp_output:
            mtdp_output = os.path.join(self.output_directory, "{}.mtdp.out.txt".format(serif_doc.docid))
            MTDP_OUTPUT = open(mtdp_output,'w')
            mtdpLogger.info("open {}".format(mtdp_output))
            for i, d in enumerate(decoder_input[0]):
                # use tuples (i, token), to make it easier to associate mtdp node names with tokens.
                d_enum = [(ti, t) for (ti, t) in enumerate(d)]
                MTDP_OUTPUT.write("{} {}\n".format(i, d_enum))
            for e in edge_list:
                MTDP_OUTPUT.write("{}\n".format(e))
            MTDP_OUTPUT.close()
            mtdpLogger.info("close {}".format(mtdp_output))
        else:
            mtdpLogger.info("skipping mtdp write to file")

        edge_list = [tuple([tuple([int(m) for m in a.split('_')]), b, tuple([int(n) for n in c.split('_')]), d]) for
                     (a, b, c, d) in edge_list]
        self.decode_mtdp_edge_count += len(edge_list)
        return edge_list

    @timer
    def integrate_mtdp(self, serif_doc, edge_list, cache, mode="use_decoder"):
        '''integrates parse into serif doc, creating new nodes where necessary

        The parse gets stored in serif.theory.ModalTemporalRelationMention objects, where each of them has a node that
        contains the corresponding serifxml object (Mention, EventMention, TimeValueMention, "ROOT_NODE", "AUTHOR_NODE",
        "NULL_CONCEIVER_NODE"), as well as a list of children, each one a serif.theory.ModalTemporalRelationMention

        :param mode: "use_decoder" when using actual model; "use_gold_data" for converting mtdp data to serifxml format
        '''

        node_to_modal_temporal_node_type = {child_tup: child_modal_temporal_node_type for
                                            (child_tup, child_modal_temporal_node_type, _, _) in edge_list}
        node_to_modal_temporal_node_type.update({(-1, -1, -1): "ROOT"})

        # avoid replicating Mention/ValueMention/EventMention for the same node tuple
        node_tup_to_mention_value_mention_event_mention = dict()    # {(snt, start_tok, end_tok): serif.theory.{Mention/ValueMention/EventMention}}
        # avoid replicating MTRM objects for the same node tuple
        node_tup_to_modal_temporal_relation_mention = dict()  # {(snt, start_tok, end_tok): serif.theory.ModalTemporalRelationMention}

        for (child_tup, child_node_type, parent_tup, relation) in edge_list:
            mtdpLogger.info("** edge {} {} {} {}".format(child_tup, child_node_type,
                                                     parent_tup, relation))

            # set child node (can't be root)
            if child_tup == (-3, -3, -3):
                child_node = "AUTHOR_NODE"
            elif child_tup == (-5, -5, -5):
                child_node = "NULL_CONCEIVER_NODE"
            elif child_tup == (-7, -7, -7):
                child_node = "DCT_NODE"
            else:
                mtdpLogger.info("*** child")

                if child_tup not in node_tup_to_mention_value_mention_event_mention:
                    if self.do_integration_overlap:
                        child_node = self.get_overlapping_node_or_create_new_node(
                            child_tup, child_node_type, cache, serif_doc)
                    else:
                        child_node = self.create_new_node (
                            child_tup, child_node_type, serif_doc)
                    node_tup_to_mention_value_mention_event_mention[child_tup] = child_node
                else:
                    child_node = node_tup_to_mention_value_mention_event_mention[child_tup]

                sentence = serif_doc.sentences[child_tup[0]]
                tokens = sentence.token_sequence[child_tup[1]:child_tup[2]+1]
                text = " ".join([x.text for x in tokens])
                if child_node is not None:
                    mtdpLogger.info("node {} {} {}".format(type(child_node), child_node.id, text))
                    node_tup_to_mention_value_mention_event_mention[child_tup] = child_node
                else:
                    mtdpLogger.info("None {}".format(text))

            # set parent node
            if parent_tup == (-1, -1, -1):
                parent_node = "ROOT_NODE"
            elif parent_tup == (-3, -3, -3):
                parent_node = "AUTHOR_NODE"
            elif parent_tup == (-5, -5, -5):
                parent_node = "NULL_CONCEIVER_NODE"
            elif parent_tup == (-7, -7, -7):
                parent_node = "DCT_NODE"
            else:
                assert parent_tup in node_to_modal_temporal_node_type
                parent_node_type = node_to_modal_temporal_node_type[parent_tup]
                mtdpLogger.info("*** parent")

                if parent_tup not in node_tup_to_mention_value_mention_event_mention:
                    if self.do_integration_overlap:
                        parent_node = self.get_overlapping_node_or_create_new_node (
                            parent_tup, parent_node_type, cache, serif_doc)
                    else:
                        parent_node = self.create_new_node (
                            parent_tup, parent_node_type, serif_doc)
                    node_tup_to_mention_value_mention_event_mention[parent_tup] = parent_node
                else:
                    parent_node = node_tup_to_mention_value_mention_event_mention[parent_tup]

            # skip this edge if one of the nodes doesn't exist
            if parent_node is None or child_node is None:
                mtdpLogger.info("*** No MTRM created")
                self.mtrm_failure.append((serif_doc.docid, child_tup, child_node_type, parent_tup, relation))
                continue

            # create new modal/temporal relation set for doc if there isn't one
            if serif_doc.modal_temporal_relation_mention_set is None:
                serif_doc.add_new_modal_temporal_relation_mention_set()
                mtdpLogger.info("*** Created modal_temporal_relation_mention_set {}".format(serif_doc.modal_temporal_relation_mention_set.id))

            # get the serifxml mtdp object for parent if it exists
            if parent_tup in node_tup_to_modal_temporal_relation_mention:
                mod_tmp_rel_mention_for_parent = node_tup_to_modal_temporal_relation_mention[parent_tup]
            else:  # else create the mtdp object for parent and cache it
                parent_node_type = node_to_modal_temporal_node_type.get(parent_tup, None)
                mod_tmp_rel_mention_for_parent = serif_doc.modal_temporal_relation_mention_set.add_new_modal_temporal_relation_mention(
                    relation_type=None, node=parent_node, modal_temporal_node_type=parent_node_type, confidence=0.9,
                    model=f"mtdp_{self.data_type}" if mode=="use_decoder" else f"gold_{self.data_type}")
                node_tup_to_modal_temporal_relation_mention[parent_tup] = mod_tmp_rel_mention_for_parent
                mtdpLogger.info("*** Created parent modal_temporal_relation_mention {}".format(mod_tmp_rel_mention_for_parent.id))
                self.mtrm_success.append((serif_doc.docid, parent_tup, 
                                          type(mod_tmp_rel_mention_for_parent),
                                          mod_tmp_rel_mention_for_parent.id))

                # set roots at document-level when first creating them
                if parent_node == "ROOT_NODE":
                    if self.data_type == "modal":
                        mtdpLogger.info("Setting modal root")
                        serif_doc.modal_temporal_relation_mention_set.set_modal_root(mod_tmp_rel_mention_for_parent)
                    elif self.data_type == "time":
                        mtdpLogger.info("Setting temporal root")
                        serif_doc.modal_temporal_relation_mention_set.set_temporal_root(mod_tmp_rel_mention_for_parent)

            # add a serifxml mtdp object for the child to the mtdp parent object's list of children
            if child_tup in node_tup_to_modal_temporal_relation_mention:
                mod_tmp_rel_mention_for_child = node_tup_to_modal_temporal_relation_mention[child_tup]
                mod_tmp_rel_mention_for_parent.children.append(mod_tmp_rel_mention_for_child)
            else:
                mod_tmp_rel_mention_for_child = mod_tmp_rel_mention_for_parent.add_child(relation_type=relation,
                                                                                         node=child_node,
                                                                                         modal_temporal_node_type=child_node_type,
                                                                                         confidence=0.9,
                                                                                         model=f"mtdp_{self.data_type}" if mode=="use_decoder" else f"gold_{self.data_type}")
                mtdpLogger.info("*** Created child modal_temporal_relation_mention {}".format(mod_tmp_rel_mention_for_child.id))
                self.mtrm_success.append((serif_doc.docid, child_tup, child_node_type,
                                          parent_tup, relation,
                                          type(mod_tmp_rel_mention_for_child),
                                          mod_tmp_rel_mention_for_child.id))

            # cache the mtdp child object
            node_tup_to_modal_temporal_relation_mention[child_tup] = mod_tmp_rel_mention_for_child
            self.integrate_mtdp_edge_count += 1


    def get_overlapping_node_or_create_new_node(self, node_tup, node_type, cache, serif_doc):
        # TODO do we really need to ensure that "Events" are of type <EventMention>?
        #  For instance, we might get "employment" which is parsed as an "Event" but is of type <Mention> rather than <EventMention>

        # try to find matching node in serif doc
        if node_tup in cache:
            node = cache[node_tup]
            # if self.node_type_to_obj[node_type] == type(cache[node_tup]):
            mtdpLogger.info("On node_tup {} found cached node {} {}".format(
                node_tup, type(node), node.id))
            self.cache_success.append((serif_doc, node_tup))
            return node

        # if Event, find overlapping node (token-wise) in serif doc
        if node_type == "Event" or node_type == "Event-SIP":
            overlapping_node_tup = self.node_overlap(node_tup, list(cache.keys()))
            if overlapping_node_tup is not None:
                # if self.node_type_to_obj[node_type] == type(cache[overlapping_node_tup]):
                node = cache.get(overlapping_node_tup)
                if node is not None and type(node) == EventMention:
                    mtdpLogger.info("On node_tup {} found overlapping node {} {} {}".format(
                        node_tup, overlapping_node_tup, type(node), node.id))
                    self.overlap_success.append((serif_doc, node_tup))
                else:
                    mtdpLogger.info("On node_tup {} found overlapping node with unknown key {}".format(
                        node_tup, overlapping_node_tup))
                    self.overlap_failure.append((serif_doc, node_tup))
                return node

        # create new node if none of the above worked
        node = self.create_new_node(node_tup, node_type, serif_doc)
        return node
        

    def node_overlap(self, node_tup, candidate_node_tups):
        # get nodes in same sentence
        same_snt_node_tups = [(snt_idx, i, j) for (snt_idx, i, j) in candidate_node_tups if snt_idx == node_tup[0]]
        # get overlapping nodes in same sentence
        overlapping_node_tups = [tup for tup in same_snt_node_tups if self.overlaps(node_tup[1], node_tup[2], tup[1], tup[2])]
        # sort overlapping nodes by IoU
        overlapping_node_tups = sorted(overlapping_node_tups,
                                       reverse=True,
                                       key=lambda tup: self.IoU(node_tup[1], node_tup[2], tup[1], tup[2]))
        # return node with greatest IoU overlap
        if len(overlapping_node_tups) > 0:
            return overlapping_node_tups[0]
        return None

    def overlaps(self, s1, e1, s2, e2):
        return min(e1, e2) - max(s1, s2) >= 0

    def IoU(self, s1, e1, s2, e2):
        '''assumes spans are overlapping'''
        I = min(e1, e2) - max(s1, s2)
        U = min(s1, s2) - max(e1, e2)
        if U != 0:
            return I/U
        return 0

    def create_new_node(self, node_tup, node_type, serif_doc):
        mtdpLogger.info("On node_tup {} creating new node".format(node_tup))
        self.create_node.append((serif_doc, node_tup))

        (sent_no, start_token_index, end_token_index) = node_tup
        sent = serif_doc.sentences[sent_no]
        start_token = sent.token_sequence[start_token_index]
        end_token = sent.token_sequence[end_token_index]

        overlap, anchors = find_valid_anchors_by_token_index(
            # find any synnode in sentence which partially covers the token indices
            sent.sentence_theories[0],
            start_token_index,
            end_token_index,
            0.01 if self.mode == "use_decoder" else 1.0
        )
        anchor = None
        if len(anchors) > 0:
            anchor = anchors[0]

        new_node = None
        if node_type == "Event" or node_type == "Event-SIP":
            if sent.event_mention_set is None:
                sent.add_new_event_mention_set()

            new_node = sent.event_mention_set.add_new_event_mention(
                event_type=DUMMY_EVENT_ID if node_type=="Event" else DUMMY_EVENT_SIP_ID,
                anchor_node=anchor,
                score=0.5 if self.mode=="use_decoder" else 1.0)

            new_node.model = "MTDP"
            self.event_mention.append((serif_doc, new_node))

            # Add semantic_phrase_start and semantic_phrase_end as required
            # to add NLPLingo event arguments
            new_node.semantic_phrase_start = start_token_index
            new_node.semantic_phrase_end = end_token_index

            # if "use_gold_data", anchor_node start end tokens will 
            # match semantic phrase start end
            # dmz: these are now added for all dummy events regardless of 
            # anchor or mode.
            if not anchor or self.mode == "use_gold_data":  
                new_node.semantic_phrase_start = start_token_index
                new_node.semantic_phrase_end = end_token_index

            if serif_doc.event_set is None:
                serif_doc.add_new_event_set()
            new_event = serif_doc.event_set.add_new_event(event_mentions=[new_node], event_type="UNDET")
            self.event.append((serif_doc, new_event))
        elif node_type == "Conceiver":
            if sent.mention_set is None:
                sent.add_new_mention_set()
            if anchor:
                new_node = sent.mention_set.add_new_mention(syn_node=anchor, mention_type="desc",
                                                            entity_type=DUMMY_CONCEIVER_ID)
            else:
                new_node = sent.mention_set.add_new_mention_from_tokens(mention_type="desc",
                                                                        entity_type=DUMMY_CONCEIVER_ID,
                                                                        start_token=start_token, end_token=end_token)
            self.mention.append((serif_doc, new_node))
        elif node_type == "Timex":
            if sent.value_mention_set is None:
                sent.add_new_value_mention_set()
            new_node = sent.value_mention_set.add_new_value_mention(start_token, end_token, "TIMEX2.TIME")
            self.value_mention.append((serif_doc, new_node))

        assert new_node is not None
        mtdpLogger.info("Created node {} {}".format(
            type(new_node), new_node.id))
        return new_node

    def process_document(self, serif_doc):

        if self.mode == "use_decoder":

            mtdpLogger.info("* process_document {}".format(serif_doc.docid))
            try:
                # run decoding
                edge_list = self.decode_mtdp(serif_doc)

                # integrate parse
                cache = self.get_snt_start_end_to_obj_cache(serif_doc)
                self.integrate_mtdp(serif_doc, edge_list, cache, mode="use_decoder")

                if self.do_eval:
                    EVAL = {}
                    eval_types = ["unlabeled", "rel only", "labeled"]
                    for x in eval_types:
                        EVAL[x] = {}

                    assert serif_doc.docid in self.docid_to_edge_list.keys()
                    gold_edge_list = self.docid_to_edge_list[serif_doc.docid]

                    pred_edge_list = create_edge_list_from_serif_doc(serif_doc, data_type=self.data_type)

                    print("docid {}".format(serif_doc.docid))
                    mtdpLogger.info("**** unlabeled eval ****")
                    print("**** unlabeled eval ****")
                    macro_f, micro_f = eval_all([gold_edge_list], [pred_edge_list], get_unlabeled_tups, labeled=False)
                    EVAL["unlabeled"]["macro_f"] = macro_f
                    EVAL["unlabeled"]["micro_f"] = micro_f

                    mtdpLogger.info("**** rel only eval ****")
                    print("**** rel only eval ****")
                    macro_f, micro_f = eval_all([gold_edge_list], [pred_edge_list], get_rel_only_tups, labeled=False)
                    EVAL["rel only"]["macro_f"] = macro_f
                    EVAL["rel only"]["micro_f"] = micro_f

                    mtdpLogger.info("**** labeled eval ****")
                    print("**** labeled eval ****")
                    macro_f, micro_f = eval_all([gold_edge_list], [pred_edge_list], get_labeled_tups, labeled=True)
                    EVAL["labeled"]["macro_f"] = macro_f
                    EVAL["labeled"]["micro_f"] = micro_f

                    if self.do_write_mtdp_eval:
                        eval_output = os.path.join(self.output_directory, "{}.mtdp.eval.txt".format(serif_doc.docid))
                        EVAL_OUTPUT = open(eval_output,'w')
                        for x in eval_types:
                            EVAL_OUTPUT.write("**** {} eval ****\n".format(x))
                            EVAL_OUTPUT.write("macro average: f = {:.3f}\n".format(EVAL[x]["macro_f"]))
                            EVAL_OUTPUT.write("micro average: f = {:.3f}\n".format(EVAL[x]["micro_f"]))
                        EVAL_OUTPUT.close()

            except RuntimeError as e:
                mtdpLogger.warning("RuntimeError (most likely CUDA out of memory error)")
                globalLogger.warning("mtdp_decoder.process_document failed on {}".format(
                    serif_doc.docid))
                mtdpLogger.warning("mtdp_decoder.process_document failed on {}\n{}".format(
                    serif_doc.docid, e))
                self.doc_failure.append(serif_doc.docid)
            except IndexError as e:
                mtdpLogger.warning("IndexError (most likely because the parse came out empty)")
                globalLogger.warning("mtdp_decoder.process_document failed on {}".format(
                    serif_doc.docid))
                mtdpLogger.warning("mtdp_decoder.process_document failed on {}\n{}".format(
                    serif_doc.docid, e))
                self.doc_failure.append(serif_doc.docid)
            except KeyError as e:
                mtdpLogger.warning("KeyError (most likely coming from modal_and_temporal_parsing/mtdp/model.py)")
                globalLogger.warning("mtdp_decoder.process_document failed on {}".format(
                    serif_doc.docid))
                mtdpLogger.warning("mtdp_decoder.process_document failed on {}\n{}".format(
                    serif_doc.docid, e))
                self.doc_failure.append(serif_doc.docid)
            except ValueError as e:
                mtdpLogger.warning("ValueError (likely because document is too short)")  # Wrong shape for input_ids (shape torch.Size([0])) or attention_mask (shape torch.Size([0]))
                globalLogger.warning("mtdp_decoder.process_document failed on {}".format(
                    serif_doc.docid))
                mtdpLogger.warning("mtdp_decoder.process_document failed on {}\n{}".format(
                    serif_doc.docid, e))
                self.doc_failure.append(serif_doc.docid)


        elif self.mode == "use_gold_data":

            if serif_doc.docid in self.docid_to_edge_list.keys():
                edge_list = self.docid_to_edge_list[serif_doc.docid]

                self.integrate_mtdp(serif_doc, edge_list, cache=dict(), mode="use_gold_data")


class MTDPEvaluator(CorpusModel):

    def __init__(self, data_type, docid_to_edge_list_path_file, **kwargs):
        '''
        :param data_type: "modal" or "time"
        :param docid_to_edge_list_path_file: e.g. "/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/mtdp_data/modal.docid_to_edge_list_paths"
                                             only for converting gold data to serifxml, i.e. when mode == "use_gold_data"
        :param kwargs:
        '''
        super(MTDPEvaluator, self).__init__(**kwargs)
        self.data_type = data_type
        self.docid_to_edge_list_path_file = docid_to_edge_list_path_file

        self.docid_to_edge_list_path_file = docid_to_edge_list_path_file
        assert self.docid_to_edge_list_path_file is not None and os.path.exists(self.docid_to_edge_list_path_file)

        self.do_write_mtdp_eval = kwargs.get("do_write_mtdp_eval","false").lower() == "true"
        self.output_directory = kwargs["argparse"].output_directory
        self.docid_to_edge_list = get_docid_to_edge_list_dict(self.docid_to_edge_list_path_file, as_str=True)
        self.scorer = mtdp_scorer.MTDPScorer()
        self.scorer.load_references(self.docid_to_edge_list_path_file)

    def process_documents(self, serif_docs):

        gold_tups, pred_tups = [], []

        for serif_doc in serif_docs:

            assert serif_doc.docid in self.docid_to_edge_list.keys()
            gold_edge_list = self.docid_to_edge_list[serif_doc.docid]

            pred_edge_list = create_edge_list_from_serif_doc(serif_doc, data_type=self.data_type)

            gold_tups.append(gold_edge_list)
            pred_tups.append(pred_edge_list)

            self.scorer.score_edgelist_candidate (
                serif_doc.docid,
                pred_edge_list)
            

        EVAL = {}
        eval_types = ["unlabeled", "rel only", "labeled"]
        for x in eval_types:
            EVAL[x] = {}
        
        mtdpLogger.info("**** unlabeled eval ****")
        print("**** unlabeled eval ****")
        macro_f, micro_f = eval_all(gold_tups, pred_tups, get_unlabeled_tups, labeled=False)
        EVAL["unlabeled"]["macro_f"] = macro_f
        EVAL["unlabeled"]["micro_f"] = micro_f

        mtdpLogger.info("**** rel only eval ****")
        print("**** rel only eval ****")
        macro_f, micro_f = eval_all(gold_tups, pred_tups, get_rel_only_tups, labeled=False)
        EVAL["rel only"]["macro_f"] = macro_f
        EVAL["rel only"]["micro_f"] = micro_f

        mtdpLogger.info("**** labeled eval ****")
        print("**** labeled eval ****")
        macro_f, micro_f = eval_all(gold_tups, pred_tups, get_labeled_tups, labeled=True)
        EVAL["labeled"]["macro_f"] = macro_f
        EVAL["labeled"]["micro_f"] = micro_f

        if self.do_write_mtdp_eval:
            eval_output = os.path.join(self.output_directory, "corpus.mtdp.eval.txt")
            EVAL_OUTPUT = open(eval_output,'w')
            for x in eval_types:
                EVAL_OUTPUT.write("**** {} eval ****\n".format(x))
                EVAL_OUTPUT.write("macro average: f = {:.3f}\n".format(EVAL[x]["macro_f"]))
                EVAL_OUTPUT.write("micro average: f = {:.3f}\n".format(EVAL[x]["micro_f"]))
            EVAL_OUTPUT.close()

            eval_output_org = os.path.join(self.output_directory, "corpus.mtdp.eval.org")
            self.scorer.write_org(eval_output_org)


if __name__ == '__main__':
    import serifxml3

    test_file = "/nfs/raid66/u11/users/brozonoy-ad/text-open/src/python/test/test_mtdp_adapter/test_file.xml"
    # d = serifxml3.Document("/nfs/raid66/u11/users/brozonoy-ad/Hume/expts/mtdp/mtdp/split/158/ENG_NW_WM_aylien_3005407353_0.xml")
    d = serifxml3.Document("/nfs/raid66/u11/users/brozonoy-ad/text-open/src/python/test/sample_doc.xml")
    # d = serifxml3.Document("./TMP_OUT.xml")
    decoder = MTDPDecoder(data_type="modal",
                          pretrained_model_dir="/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing/modal_weight.mix")
    decoder.process_document(d)
