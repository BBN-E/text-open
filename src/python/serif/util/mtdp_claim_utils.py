import argparse
import os
import re
import logging
import textwrap
import csv
import json
import serifxml3

from serif.model.event_mention_model import EventMentionModel
from serif.theory.event_mention import EventMention
from serif.theory.mention import Mention
from serif.theory.value_mention import ValueMention

logging.basicConfig(format='%(asctime)s - %(levelname)s - %(name)s - %(message)s',
                    datefmt='%m/%d/%Y %H:%M:%S',
                    level=logging.INFO)
logger = logging.getLogger(__name__)

################################################################################
# Given an event mention, return the sentence that contains it and the
# its tokens
################################################################################
def get_serif_components_of_event_mention (eventMention):
    if eventMention.anchor_node is not None:
        sentence = eventMention.sentence
        start_token = eventMention.anchor_node.start_token
        end_token = eventMention.anchor_node.end_token
        tokens = sentence.token_sequence[start_token.index():end_token.index()+1]
        return sentence, tokens

    else:
        sentence = eventMention.sentence
        start_token_index = eventMention.semantic_phrase_start
        end_token_index = eventMention.semantic_phrase_end
        tokens = sentence.token_sequence[start_token_index:end_token_index+1]
        return sentence, tokens
    
def get_serif_components_of_mention (mention):
    if mention.syn_node is not None:
        sentence = mention.sentence
        start_token = mention.syn_node.start_token
        end_token = mention.syn_node.end_token
        tokens = sentence.token_sequence[start_token.index():end_token.index()+1]
        return sentence, tokens

    else:
        sentence = mention.sentence
        start_token = mention.start_token
        end_token = mention.end_token
        tokens = sentence.token_sequence[start_token.index():end_token.index()+1]
        return sentence, tokens

def get_serif_components_of_value_mention (mention):
    sentence = mention.sentence
    start_token = mention.start_token
    end_token = mention.end_token
    tokens = sentence.token_sequence[start_token.index():end_token.index()+1]
    return sentence, tokens

def wrap_document_text (doc):
    if doc is None: return ""
    docText = doc.original_text.text
    return wrap_text(docText)

def wrap_sentence_text (sentence):
    if sentence is None: return ""
    return wrap_text(sentence.text)

def wrap_sentence_range (document, sent1, sent2):
    if document is None or sent1 is None or sent2 is None: return ""
    indexes = sorted([sent1.sent_no,sent2.sent_no])
    sentences = document.sentences[indexes[0]:indexes[1]+1]
    rangeText = " ".join(["({}) {}".format(x.sent_no, x.text) for x in sentences])
    return wrap_text(rangeText)

def wrap_text (text):
    if text is None: return ""
    wrappedText = textwrap.wrap(text, initial_indent="", subsequent_indent="  ")
    wrappedText = [re.sub("^(\s*)\*","\1#*",x) for x in wrappedText]
    wrappedText = "\n".join(wrappedText)
    return wrappedText
    

def sort_node (n):
    c1, c2, c3 = [int(x) for x in n.data.nodeId.split("_")]
    return (c1, c2, c3)

def sort_edge (t):
    c1, c2, c3 = [int(x) for x in t[0].split("_")]
    p1, p2, p3 = [int(x) for x in t[2].split("_")]
    return (c1, c2, c3, p1, p2, p3)

# a simple tree structure
class MTDPNode:
    def __init__ (self, data):
        self.parent = None
        self.children = []
        self.data = data

    def depth_first (self):
        result = []
        self._depth_first(result)
        return result

    def _depth_first (self, result):
        result.append(self)
        for node in sorted(self.children, key=sort_node):
            node._depth_first(result)

    def get_mtra (self):
        return self.data.modal_temporal_relation_argument

    def get_parent_mtra (self):
        if self.parent is None:
            return None
        else:
            return self.parent.data.modal_temporal_relation_argument

    def write_org (self, ORG, depth=0):
        if self.data.nodeLabel is not None:
            ORG.write("{}{} {} {}\n".format(
                "  "*depth, self.data.nodeId, self.data.nodeLabel, self.data.relationLabel))
        else:
            ORG.write("{}{}\n".format(
                "  "*depth, self.data.nodeId))
        for child in self.children:
            child.write_org(ORG,depth+1)

    def __repr__ (self):
        return ("{}".format(self.data))

class MTDPNodeData:
    def __init__ (self, doc, special, label, sentence, tokens, node_id, relation_label=None, mtra=None):
        self.doc = doc
        self.nodeId = node_id
        self.nodeLabel = label
        self.relationLabel = relation_label
        self.sentence = sentence
        self.tokens = tokens
        self.sentence_idx, self.token_start_idx, self.token_end_idx = [int(x) for x in node_id.split("_")]
        self.modal_temporal_relation_argument = mtra

    def token_count (self):
        return len(self.tokens)

    def get_text (self):
        if self.sentence_idx == -1:
            return "ROOT"
        elif self.sentence_idx == -3:
            return "AUTHOR"
        elif self.sentence_idx == -5:
            return "NULL_CONCEIVER"
        elif self.sentence_idx == -7:
            return "DCT"
        else:
            return " ".join([x.text for x in self.tokens])

    def get_upos (self):
        if len(self.tokens) > 0:
            return self.tokens[0].upos
        else:
            return None

    def get_dep_rel (self):
        if len(self.tokens) > 0:
            return self.tokens[0].dep_rel
        else:
            return None

    def __repr__ (self):
        return ("{},{},{},{}".format(
            self.doc.docid,
            self.nodeId,
            self.nodeLabel,
            self.relationLabel))

class MTDPEdge:
    def __init__ (self, tup):
        self.childId, self.childLabel, self.parentId, self.relationLabel = tup

class MTDPSerifEdge:
    def __init__ (self,
                  child_special, 
                  child_label,
                  child_sentence, child_tokens,
                  parent_special,
                  parent_label,
                  parent_sentence, parent_tokens,
                  relation_label, 
                  parent_mtrm, parent_mtra,
                  child_mtrm, child_mtra):
        self.child_special = child_special
        self.child_label = child_label
        self.child_sentence = child_sentence
        self.child_tokens = child_tokens
        self.parent_special = parent_special 
        self.parent_label = parent_label
        self.parent_sentence = parent_sentence
        self.parent_tokens = parent_tokens
        self.relation_label = relation_label
        self.child_id = self._child_id()
        self.parent_id = self._parent_id()
        self.parent_mtrm = parent_mtrm # ModalTemporalRelationMention
        self.parent_mtra = parent_mtra # ModalTemporalRelationArgument
        self.child_mtrm = child_mtrm
        self.child_mtra = child_mtra

    def _child_id (self):
        return self.node_id (self.child_special, self.child_sentence, self.child_tokens)

    def _parent_id (self):
        return self.node_id (self.parent_special, self.parent_sentence, self.parent_tokens)

    def node_id (self, special, sentence, tokens):
        special2id = {"ROOT_NODE": "-1_-1_-1",
                      "AUTHOR_NODE": "-3_-3_-3",
                      "NULL_CONCEIVER_NODE": "-5_-5_-5",
                      "DCT_NODE": "-7_-7_-7"}
        if special is not None:
            return special2id[special]

        return "_".join([
            str(sentence.sent_no),
            str(tokens[0].index()),
            str(tokens[-1].index())])

    def __repr__ (self):
        return "{} {} {} {}".format(
            self.child_special if self.child_special is not None else self.child_id, 
            self.child_label, 
            self.parent_special if self.parent_special is not None else self.parent_id,
            self.relation_label,
        )
        
    def write_org (self, i, ORG):
        ORG.write("**** {} {} {} {} {}\n".format(
            i, 
            self.child_special if self.child_special is not None else self.child_label, 
            self.child_id, 
            self.parent_special if self.parent_special is not None else self.parent_label,
            self.parent_id))
                  
        ORG.write("Child {}\n{}\n".format(self.child_sentence, self.child_tokens))
        ORG.write("Parent {}\n{}\n".format(self.parent_sentence, self.parent_tokens))

class MTDPFeature:
    def __init__ (self, doc, isReference=True):
        self.doc = doc
        self.docid = doc.docid
        self.isReference = isReference
        self.nodesById = {} # nodeId -> MTDPNode
        self.nodes = []
        self.roots = []
        self.edges = []
        self.serif_edges = []
        self.process_doc()

    def process_doc (self):
        source = "gold" if self.isReference else "mtdp"
        self.find_serif_edges()

        # create a MTDP Node for each child
        for edge in self.serif_edges:
            if edge.child_id not in self.nodesById:
                node = MTDPNode(MTDPNodeData(
                    self.doc, 
                    edge.child_special, edge.child_label, edge.child_sentence, edge.child_tokens,
                    edge.child_id,
                    edge.relation_label, edge.child_mtra
                ))
                self.nodesById[edge.child_id] = node
                self.nodes.append(node)
        # create a MTDP Node for any parent we haven't seen yet
        for edge in self.serif_edges:
            if edge.parent_id not in self.nodesById:
                node = MTDPNode(MTDPNodeData(
                    self.doc, 
                    edge.parent_special, edge.parent_label, edge.parent_sentence, edge.parent_tokens,
                    edge.parent_id,
                ))
                self.nodesById[edge.parent_id] = node
                self.nodes.append(node)
        # build a tree of MTDP Node for this document
        for edge in self.serif_edges:
            childNode = self.nodesById[edge.child_id]
            parentNode = self.nodesById[edge.parent_id]
            childNode.parent = parentNode
            parentNode.children.append(childNode)

        # identify the roots
        for node in self.nodes:
            if node.parent is None:
                self.roots.append(node)

        # self.find_claims()
        self.find_claims_from_serif_edges()
        self.find_edge_distribution()
        
    def find_claims_from_serif_edges (self):
        self.claims = []
        self.claimGroups = {}
        self.claimEdges = []
        for edge in self.serif_edges:
            child = self.nodesById[edge.child_id]
            parent = child.parent
            if parent is None: continue
            childLabel = child.data.nodeLabel
            parentLabel = parent.data.nodeLabel
            parentText = parent.data.get_text()
            dep_rel = child.data.get_dep_rel()
            upos = child.data.get_upos()
            if (childLabel in ["Event","Event-SIP"] and
                parentLabel == "Conceiver" and
                # parentText != "AUTHOR" and
                ((dep_rel == "root" and upos == "VERB") or
                 (dep_rel == "root" and upos == "ADJ") or
                 (dep_rel == "ccomp" and upos == "VERB"))):
                self.claims.append(child)
                self.claimEdges.append(edge)
                groupKey = "{} {}".format(dep_rel,upos)
                if groupKey not in self.claimGroups:
                    self.claimGroups[groupKey] = []
                self.claimGroups[groupKey].append(child)


    def find_edge_distribution (self):
        self.edge_by_node_labels = {}
        self.edge_by_dep_rel_upos = {}
        for edge in self.serif_edges:
            childNode = self.nodesById[edge.child_id]
            parentNode = self.nodesById[edge.parent_id]
            
            labelKey = "{} {}".format(childNode.data.nodeLabel,parentNode.data.nodeLabel)
            if labelKey not in self.edge_by_node_labels:
                self.edge_by_node_labels[labelKey] = []
            self.edge_by_node_labels[labelKey].append(edge)

            relposKey = "{} {}".format(childNode.data.get_dep_rel(),
                                       childNode.data.get_upos())
            if relposKey not in self.edge_by_dep_rel_upos:
                self.edge_by_dep_rel_upos[relposKey] = []
            self.edge_by_dep_rel_upos[relposKey].append(edge)


    def find_serif_edges (self):
        self.serif_edges = []

        if self.doc.modal_temporal_relation_mention_set is None:
            return

        mtrm_list = [m for m in self.doc.modal_temporal_relation_mention_set if
                     re.match("(.*)_modal", m.node.model)]
        for mtrm in mtrm_list:
            # Loop over ModalTemporalRelationMention
            parent_mtrm = mtrm
            parent_mtra = mtrm.node # ModalTemporalRelationArgument
            (parent_special, parent_mention, parent_event_mention,
             parent_value_mention, parent_sentence, parent_tokens
            ) = find_serif_components (parent_mtra)
            parent_label = parent_mtra.modal_temporal_node_type
            for child_mtrm in mtrm.children:
                # Loop over children
                child_mtra = child_mtrm.node
                (child_special, child_mention, child_event_mention,
                 child_value_mention, child_sentence, child_tokens
                 ) = find_serif_components (child_mtra)
                child_label = child_mtra.modal_temporal_node_type
                relation_label = child_mtra.relation_type
                serifEdge = MTDPSerifEdge(
                    child_special, child_label, child_sentence, child_tokens,
                    parent_special, parent_label, parent_sentence, parent_tokens,
                    relation_label, 
                    parent_mtrm, parent_mtra,
                    child_mtrm, child_mtra)
                self.serif_edges.append(serifEdge)

    def get_node_label_keys (self):
        return self.edge_by_node_labels.keys()

    def get_dep_rel_upos_keys (self):
        return self.edge_by_dep_rel_upos.keys()

    def get_node_label_edges (self, key):
        return self.edge_by_node_labels.get(key,[])

    def get_dep_rel_upos_edges (self, key):
        return self.edge_by_dep_rel_upos.get(key,[])

    def get_edge_claim_counts (self):
        claim_count = 0
        non_claim_count = 0
        edge_count = 0
        for edge in self.serif_edges:
            childNode = self.nodesById[edge.child_id]
            if childNode in self.claims:
                claim_count += 1
            else:
                non_claim_count += 1
            edge_count += 1
        return (edge_count, claim_count, non_claim_count)
            
    # The dep_rel, upos pairs that we observe when searching for claims
    def get_claim_group_keys (self):
        return sorted(self.claimGroups.keys())

    # The claims that match the dep_rel,upos pair.
    def get_claim_group (self, key):
        return self.claimGroups.get(key,[])

    def add_claims_to_serif_doc (self):
        for n, claim in enumerate(self.claims):
            # A claim in mtdpFeature.claims is a MTDPNode
            mtra = claim.get_mtra()
            parent_mtra = claim.get_parent_mtra()
            add_claim_to_serif_doc (mtra, parent_mtra)
            



    def write_org (self, ORG):
        ORG.write("** {}\n".format(self.docid))
        ORG.write("*** Edges\n")
        for i, edge in enumerate(self.edges):
            ORG.write("{} {} {} {} {}\n".format(
                i, edge.childId, edge.childLabel, edge.parentId, edge.relationLabel))
        ORG.write("*** Serif Edges\n")
        for i, edge in enumerate(self.serif_edges):
            edge.write_org(i, ORG)
        ORG.write("*** Nodes\n")
        for i, node in enumerate(sorted(self.nodes, key=sort_node)):
            ORG.write("{} {} {} {} [{}] {} {}\n".format(
                i, node.data.nodeId, node.data.nodeLabel, node.data.relationLabel,
                node.data.get_text(),
                node.data.get_upos(), node.data.get_dep_rel()))
        ORG.write("*** Trees\n")
        for root in self.roots:
            root.write_org(ORG)
        ORG.write("*** Claims ({})\n".format(len(self.claims)))
        for node in self.claims:
            parent = node.parent
            ORG.write("**** {} [{}] {} [{}] {} {}\n".format(
                node.data.nodeLabel, node.data.get_text(),
                parent.data.nodeLabel, parent.data.get_text(),
                node.data.get_dep_rel(), node.data.get_upos()))
            ORG.write("{}\n".format(node.data.modal_temporal_relation_argument))
            ORG.write("{}\n".format(wrap_sentence_text(node.data.sentence)))
            if (node.data.sentence != parent.data.sentence and
                parent.data.sentence is not None):
                ORG.write("{}\n".format(parent.data.modal_temporal_relation_argument))
                ORG.write("{}\n".format(wrap_sentence_text(parent.data.sentence)))
                
        # self.write_serif_stuff(ORG)
            
    def write_serif_stuff (self, ORG):
        ORG.write("*** Serif Stuff\n")
        ORG.write("**** Modal Temporal Relation Mention\n")
        mtrm_list = [m for m in self.doc.modal_temporal_relation_mention_set if
                     re.match("(.*)_modal", m.node.model)]
        for mtrm in mtrm_list:
            ORG.write("{}\n{}\n{}\n{}\n\n".format(
                mtrm, mtrm.node, mtrm.node.value, mtrm.node.model))
    

    # For possible use by mtdp_claim, write a lot of diagnostic information
    # to an org file during PySerif claim processing
    def write_claim_diagnostics (self, ORG):
        ORG.write("*** Serif Edges {}\n".format(
            len(self.serif_edges)))
        for edge in self.serif_edges:
            (parent_special, parent_mention, parent_event_mention,
             parent_value_mention, parent_sentence, parent_tokens
            ) = find_serif_components (edge.parent_mtra)
            (child_special, child_mention, child_event_mention,
             child_value_mention, child_sentence, child_tokens
            ) = find_serif_components (edge.child_mtra)

            ORG.write("**** {}\n".format(edge))
            ORG.write("parent mtrm {}\n".format(
                edge.parent_mtrm.id))
            ORG.write("parent mtra {}\n".format(
                edge.parent_mtra.id))
            if parent_mention is not None:
                ORG.write("parent mention {} {}\n".format(
                    parent_mention.id,
                    parent_mention.entity_type
                ))
            if parent_event_mention is not None:
                ORG.write("parent event {} {}\n".format(
                    parent_event_mention.id,
                    parent_event_mention.event_type
                ))
            if parent_value_mention is not None:
                ORG.write("parent value {} {}\n".format(
                    parent_value_mention.id,
                    parent_value_mention.value_type))
            ORG.write("child mtrm {}\n".format(
                edge.child_mtrm.id))
            ORG.write("child mtra {}\n".format(
                edge.child_mtra.id))
            if child_mention is not None:
                ORG.write("child mention {} {}\n".format(
                    child_mention.id,
                    child_mention.entity_type
                ))
            if child_event_mention is not None:
                ORG.write("child event {} {}\n".format(
                    child_event_mention.id,
                    child_event_mention.event_type
                ))

        ORG.write("*** mtdpFeature Claims {}\n".format(
            len(self.claimEdges)))
        for claimEdge in self.claimEdges:
            ORG.write("**** {}\n".format(claimEdge))            
            (parent_special, parent_mention, parent_event_mention,
             parent_value_mention, parent_sentence, parent_tokens
            ) = find_serif_components (claimEdge.parent_mtra)
            (child_special, child_mention, child_event_mention,
             child_value_mention, child_sentence, child_tokens
            ) = find_serif_components (claimEdge.child_mtra)

            ORG.write("parent mtrm {}\n".format(
                claimEdge.parent_mtrm.id))
            ORG.write("parent mtra {}\n".format(
                claimEdge.parent_mtra.id))
            if parent_mention is not None:
                ORG.write("parent mention {} {}\n".format(
                    parent_mention.id,
                    parent_mention.entity_type
                ))
            if parent_event_mention is not None:
                ORG.write("parent event {} {}\n".format(
                    parent_event_mention.id,
                    parent_event_mention.event_type
                ))
            if parent_value_mention is not None:
                ORG.write("parent value {} {}\n".format(
                    parent_value_mention.id,
                    parent_value_mention.value_type))            
            ORG.write("child mtrm {}\n".format(
                claimEdge.child_mtrm.id))
            ORG.write("child mtra {}\n".format(
                claimEdge.child_mtra.id))
            if child_mention is not None:
                ORG.write("child mention {} {}\n".format(
                    child_mention.id,
                    child_mention.entity_type
                ))
            if child_event_mention is not None:
                ORG.write("child event {} {}\n".format(
                    child_event_mention.id,
                    child_event_mention.event_type
                ))



################################################################################
# ModalTemporalRelationMention and ModalTemporalRelationArgument elements 
# represent the child or parent nodes of a MTDP relation.
# 
# Given a ModalTemporalRelationArgument, return
#   the mtdp special name or None
#   the Mention or None
#   the EventMention or None
#   the ValueMention or None
#   the sentence that contains the argument's tokens
#   the argument's tokens
################################################################################
def find_serif_components (mtra):
    # default values
    specialName = None
    mention = None
    eventMention = None
    valueMention = None
    sentence = None
    tokens = []

    valueType = type(mtra.value)
    if valueType == str:
        specialName = mtra.value

    elif valueType == EventMention:
        eventMention = mtra.value
        if eventMention.anchor_node is not None:
            sentence = eventMention.sentence
            start_token = eventMention.anchor_node.start_token
            end_token = eventMention.anchor_node.end_token
            tokens = sentence.token_sequence[start_token.index():end_token.index()+1]

        else:
            sentence = eventMention.sentence
            start_token_index = eventMention.semantic_phrase_start
            end_token_index = eventMention.semantic_phrase_end
            tokens = sentence.token_sequence[start_token_index:end_token_index+1]

    elif valueType == Mention:
        mention = mtra.value
        if mention.syn_node is not None:
            sentence = mention.sentence
            start_token = mention.syn_node.start_token
            end_token = mention.syn_node.end_token
            tokens = sentence.token_sequence[start_token.index():end_token.index()+1]

        else:
            sentence = mention.sentence
            start_token = mention.start_token
            end_token = mention.end_token
            tokens = sentence.token_sequence[start_token.index():end_token.index()+1]

    elif valueType == ValueMention:
        valueMention = mtra.value

        sentence = valueMention.sentence
        start_token = valueMention.start_token
        end_token = valueMention.end_token
        tokens = sentence.token_sequence[start_token.index():end_token.index()+1]

    else:
        raise TypeError

    return (specialName, mention, eventMention, valueMention,
            sentence, tokens)

################################################################################
# Assume that MTDP Edges and Claim attributes have already been added to the 
# document. Recover the claim edges solely from the serifxml document
################################################################################
class MTDPClaimDocument:        
    def __init__ (self):
        pass


class MTDPCorpus:
    def __init__ (self):
        self.mtdpFeatures = {} # docid -> MTDPFeature

    def add_document (self, doc, isReference=True):
        self.mtdpFeatures[doc.docid] = MTDPFeature(doc, isReference=isReference)

    def write_org (self, ORG):

        # Serif stuff
        mtrmTypes = set()
        mtrmNodeTypes = set()
        mtrmModels = set()
        for docid in self.mtdpFeatures:
            doc = self.mtdpFeatures[docid].doc
            for mtrm in doc.modal_temporal_relation_mention_set:
                mtrmTypes.add(type(mtrm))
                mtrmNodeTypes.add(type(mtrm.node))
                mtrmModels.add(mtrm.node.model)
        ORG.write("** Serif stuff\n")
        ORG.write("*** Children of ModalTemporalRelationMentionSet\n")
        for x in sorted(mtrmTypes):
            ORG.write("{}\n".format(x))
        ORG.write("*** MTRM Node\n")
        for x in sorted(mtrmNodeTypes):
            ORG.write("{}\n".format(x))
        ORG.write("*** MTRM Model\n")
        for x in sorted(mtrmModels):
            ORG.write("{}\n".format(x))


        # Gather some corpus level counts
        claim_group_keys = set()
        for docid in self.mtdpFeatures:
            mtdpFeature = self.mtdpFeatures[docid]
            claim_group_keys.update(mtdpFeature.get_claim_group_keys())
        claim_group_counts = {}
        for key in claim_group_keys:
            claim_group_counts[key] = 0
            for docid in self.mtdpFeatures:
                claim_group_counts[key] += len(self.mtdpFeatures[docid].get_claim_group(key))
        ORG.write("** Claim group distribution\n")
        for key in sorted(claim_group_keys, key=lambda x: - claim_group_counts[x]):
            ORG.write("{} {}\n".format(key, claim_group_counts[key]))
        ORG.write("** Edge distributions\n")
        edge_group_counts = {}
        edge_group_counts["claim"] = 0
        edge_group_counts["non-claim"] = 0
        edge_group_counts["edge"] = 0
        for docid in self.mtdpFeatures:
            mtdpFeature = self.mtdpFeatures[docid]
            e, c, nc = mtdpFeature.get_edge_claim_counts()
            edge_group_counts["edge"] += e
            edge_group_counts["claim"] += c
            edge_group_counts["non-claim"] += nc
        ORG.write("edge {} claim {} non-claim {}\n".format(
            edge_group_counts["edge"],
            edge_group_counts["claim"],
            edge_group_counts["non-claim"]))
        node_label_keys = set()
        dep_rel_upos_keys = set()
        for docid in self.mtdpFeatures:
            mtdpFeature = self.mtdpFeatures[docid]
            node_label_keys.update(mtdpFeature.get_node_label_keys())
            dep_rel_upos_keys.update(mtdpFeature.get_dep_rel_upos_keys())
        # clear out edge_group_counts
        edge_group_counts = {}
        for key in node_label_keys:
            edge_group_counts[key] = 0
        for docid in self.mtdpFeatures:
            mtdpFeature = self.mtdpFeatures[docid]
            for key in node_label_keys:
                edge_group_counts[key] += len(mtdpFeature.get_node_label_edges(key))
        ORG.write("*** By Node Labels\n")
        for key in sorted(node_label_keys, key=lambda x: - edge_group_counts[x]):
            ORG.write("{} {}\n".format(key,edge_group_counts[key]))
        # clear out edge_group_counts
        edge_group_counts = {}
        for key in dep_rel_upos_keys:
            edge_group_counts[key] = 0
        for docid in self.mtdpFeatures:
            mtdpFeature = self.mtdpFeatures[docid]
            for key in dep_rel_upos_keys:
                edge_group_counts[key] += len(mtdpFeature.get_dep_rel_upos_edges(key))
        ORG.write("*** By Dependency Relation and Part of Speech\n")
        for key in sorted(dep_rel_upos_keys, key=lambda x: - edge_group_counts[x]):
            ORG.write("{} {}\n".format(key,edge_group_counts[key]))
        

        # Results by document
        for docid in sorted(self.mtdpFeatures.keys()):
            self.mtdpFeatures[docid].write_org(ORG)



class DependencyNode:
    def __init__ (self, data):
        self.parent = None
        self.children = []
        self.data = data

    def depth_first (self):
        result = []
        self._depth_first(result)
        return result

    def _depth_first (self, result):
        result.append(self)
        for node in self.children:
            node._depth_first(result)

    def write_org (self, ORG, depth=0):
        ORG.write("{}{} {} {} {}\n".format(
            "  "*depth, self.data.id, self.data.text, self.data.upos, self.data.dep_rel))
        for child in self.children:
            child.write_org(ORG, depth+1)

class DependencyEdge:
    def __init__ (self, child, parent):
        self.child = child
        self.parent = parent

    def write_org (self, ORG):
        child_id = self.child.id
        child_text = self.child.text
        parent_id = self.parent.id if self.parent is not None else None
        parent_text = self.parent.text if self.parent is not None else None
        relation = self.child.dep_rel
        if self.parent is None:
            ORG.write("{} {} {}\n".format(child_id, child_text, relation))
        else:
            ORG.write("{} {} {} {} {}\n".format(
                child_id, child_text, relation, parent_id, parent_text))

class DependencyFeature:
    def __init__ (self, doc):
        self.doc = doc
        self.docid = doc.docid
        self.edges = {} # sentence -> list of DependencyEdge
        self.nodes = {} # token -> DepdendencyNode
        self.roots = {} # sentence -> list of DependencyNode

        self.process_doc()

    def process_doc (self):
        for sentence in self.doc.sentences:
            self.edges[sentence] = []
            self.roots[sentence] = []
            for token in sentence.token_sequence:
                parent = token.head
                edge = DependencyEdge(token, parent)
                self.edges[sentence].append(edge)

            # create a Dependency Node for the child of each edge
            for edge in self.edges[sentence]:
                node = DependencyNode(edge.child)
                self.nodes[edge.child] = node

            # build a tree of DependencyNode for this sentence
            for edge in self.edges[sentence]:
                childNode = self.nodes[edge.child]
                if edge.parent is not None:
                    parentNode = self.nodes[edge.parent]
                    childNode.parent = parentNode
                    parentNode.children.append(childNode)
                else:
                    self.roots[sentence].append(childNode)

    def get_node_by_token (self, token):
        return self.nodes.get(token, None)

    def get_governed_text (self, token):
        node = self.nodes[token]
        sentence = token.sentence
        governed_tokens = [x.data for x in node.depth_first()]
        governed_texts = [] # fill in sentence order
        inGap = True
        for token in sentence.token_sequence:
            if token in governed_tokens:
                governed_texts.append(token.text)
                inGap = False
            else:
                if not inGap:
                    governed_texts.append("...")
                inGap = True
        # Avoid final "..."
        if governed_texts[-1] == "...":
            governed_texts.pop()
        return " ".join(governed_texts)

    def write_org (self, ORG):
        ORG.write("** {}\n".format(self.docid))
        for i, sentence in enumerate(self.doc.sentences):
            ORG.write("*** Sentence {}\n".format(i))
            ORG.write("**** Text\n")
            ORG.write("{}\n".format(wrap_sentence_text(sentence)))
            ORG.write("**** Edges\n")
            for edge in self.edges[sentence]:
                edge.write_org(ORG)
            ORG.write("**** Tree\n")
            for root in self.roots[sentence]:
                root.write_org(ORG)
            ORG.write("**** Governed Text By Token\n")
            for token in sentence.token_sequence:
                governed_text = self.get_governed_text(token)
                ORG.write("[{} {}] {}\n".format(token.id, token.text, governed_text))

class DependencyCorpus:
    def __init__ (self):
        self.dependencyFeatures = {} # docid -> DependencyFeature

    def add_document (self, doc):
        self.dependencyFeatures[doc.docid] = DependencyFeature(doc)


    def write_org (self, ORG):
        for docid in sorted(self.dependencyFeatures.keys()):
            self.dependencyFeatures[docid].write_org(ORG)

class MtdpOrg:
    def __init__ (self):
        self.orgPath = None
        self.orgFile = None

    def open (self, path):
        self.orgPath = path
        self.orgFile = open(path,'w')

    def close (self):
        self.orgFile.close()

    def process_filelist (self, path):
        if self.orgFile is None:
            return

        TEXTLIST = open(path,'r')
        for line in TEXTLIST:
            serifxml_path = line.rstrip()
            self.process_serifxml(serifxml_path)

    def process_json (self, path):
        if self.orgFile is None:
            return

        logger.info("processing {}".format(path))

        f = open(path,'r')
        db_dump = json.load(f)
        # db_dump is a list of dictionaries
        N = 0
        for d1 in db_dump:
            for text_analytics in d1.get('text_analytics',[]):
                serifxml = text_analytics.get('serifxml', None)
                if serifxml is not None:
                    N += 1
                    self.process_serifxml(serifxml)
            if N > 10:
                break
    
    def process_serifxml (self, path):
        if self.orgFile is None:
            return
        
        logger.info("processing {}".format(path))

        doc = serifxml3.Document(path)
        mtdpFeature = MTDPFeature(doc, isReference=False)
        depFeature = DependencyFeature(doc)

        self.orgFile.write("* {}\n".format(doc.docid))
        self.orgFile.write("** MTDP Edges\n")
        for edge in mtdpFeature.serif_edges:
            self.orgFile.write("*** {}\n".format(edge))
            self.orgFile.write("Child {}\n{}\n".format(
                edge.child_tokens,
                wrap_sentence_text(edge.child_sentence)))
            self.orgFile.write("Parent {}\n{}\n".format(
                edge.parent_tokens,
                wrap_sentence_text(edge.parent_sentence)))
        self.orgFile.write("** Dependency Parses\n")
        for n, sentence in enumerate(doc.sentences):
            self.orgFile.write("*** Sentence {}\n{}\n".format(
                n, wrap_sentence_text(sentence)))
            self.orgFile.write("**** Edges\n")
            for edge in depFeature.edges[sentence]:
                edge.write_org(self.orgFile)
            self.orgFile.write("**** Depedency Tree\n")
            for root in depFeature.roots[sentence]:
                root.write_org(self.orgFile)
        

class ClaimCSV:
    def __init__ (self, detect_claims=False):
        self.csvPath = None
        self.csvFile = None
        self.csvWriter = None
        self.parent_children_tab = None
        self.topic_list = None
        self.child_uid_to_topic_id = {}
        self.topic_id_to_topic_subtopic_template = {}
        # If pipeline did not include mdtp_claim, then perform
        # it here
        self.detect_claims = detect_claims

    def open (self, path):
        self.csvPath = path
        self.csvFile = open(path,'w')
        self.csvWriter = csv.writer(self.csvFile)
        self.header()

    def close (self):
        self.csvWriter = None
        self.csvFile.close()

    def header (self):
        self.csvWriter.writerow([
            "document-id",

            "claimant-id",
            "claimant-entity-type",
            "claimant-start-offset",
            "claimant-end-offset",
            "claimant-text",

            "event-id",
            "event-event-type",
            "event-start-offset",
            "event-end-offset",
            "event-trigger",
            "event-governed-text",
            "event-sentence-text",

            "event-trigger-dep-rel",
            "claiming-word",

            "modal-strength",
            "epistemic-strength-label",

            "claim-topic-id",
            "claim-template-X",

            "doc-topic-id",
            "doc-topic",
            "doc-subtopic",
            "doc-template"
        ])

    def load_topics (self, parent_children_tab, topic_list):
        # Map of document to topic,subtopic,template from LDC
        self.parent_children_tab = parent_children_tab
        self.topic_list = topic_list
        self.child_uid_to_topic_id = {}
        self.topic_id_to_topic_subtopic_template = {}

        if self.parent_children_tab == "" or self.topic_list == "":
            return

        if not os.path.exists(self.parent_children_tab):
            logger.info("parent_children_tab does not exist: {}".format(
                self.parent_children_tab))
            return

        if not os.path.exists(self.topic_list):
            logger.info("topic_list does not exist: {}".format(
                self.topic_list))
            return

        with open(self.parent_children_tab) as doc_list:
            doc_list_reader = csv.DictReader(doc_list, delimiter='\t')
            for document in doc_list_reader:
                self.child_uid_to_topic_id[document['child_uid']] = document['topic']
        with open(self.topic_list, newline='') as doc_list:
            doc_list_reader = csv.DictReader(doc_list, delimiter='\t')
            for document in doc_list_reader:
                self.topic_id_to_topic_subtopic_template[document['ID']] = (document['topic'], document['subtopic'], document['Template'])
        

    def process_filelist (self, path):
        if self.csvWriter is None:
            return

        TEXTLIST = open(path,'r')
        for line in TEXTLIST:
            serifxml_path = line.rstrip()
            self.process_serifxml(serifxml_path)

    def process_json (self, path):
        if self.csvWriter is None:
            return

        logger.info("processing {}".format(path))

        f = open(path,'r')
        db_dump = json.load(f)
        # db_dump is a list of dictionaries
        N = 0
        for d1 in db_dump:
            for text_analytics in d1.get('text_analytics',[]):
                serifxml = text_analytics.get('serifxml', None)
                if serifxml is not None:
                    N += 1
                    self.process_serifxml(serifxml)
            if N > 10:
                break
        
    def process_serifxml (self, path):
        if self.csvWriter is None:
            return

        logger.info("processing {}".format(path))

        doc = serifxml3.Document(path)

        # If pipeline did not include mtdp_claim, then perform it here
        if self.detect_claims:
            logger.info("detecting claims on {}".format(doc.docid))
            mtdpFeature = MTDPFeature(doc, isReference=False)
            mtdpFeature.add_claims_to_serif_doc()

        claims = extract_claims(doc)
        for claim in claims:
            # document-level topics from LDC
            # L0C04958D.rsd --> L0C04958D
            child_uid = os.path.splitext(claim.doc.docid)[0]
            topic_id = self.child_uid_to_topic_id.get(child_uid,"")
            topic, subtopic, template = self.topic_id_to_topic_subtopic_template.get(topic_id,("","",""))
            # empty columns for manual annotation
            claim_topic_id = ""
            claim_template_X = ""

            self.csvWriter.writerow([
                claim.doc.docid,

                claim.claimant_id,
                claim.claimant_type,
                claim.claimant_start_char,
                claim.claimant_end_char,
                claim.claimant_text,

                claim.event.id,
                claim.event.event_type,
                claim.event_tokens[0].start_char,
                claim.event_tokens[-1].end_char,
                claim.event_text,
                claim.event_governed_text,
                claim.event_sentence_text,

                claim.event_dep_rel,
                claim.event_dp_parent_text,

                claim.modal_strength,
                claim.epistemic_strength,
                
                claim_topic_id,
                claim_template_X,

                topic_id,
                topic,
                subtopic,
                template
            ])
            

class Claim:
    def __init__ (self, doc, df, mf, event, claimant):
        self.doc = doc
        self.event = event
        self.claimant = claimant

        self.event_sentence, self.event_tokens = get_serif_components_of_event_mention(event)
        if claimant is None:
            self.claimant_sentence = None
            self.claimant_tokens = []
            self.claimant_type = "AUTHOR"
            self.claimant_id = ""
            self.claimant_start_char = ""
            self.claimant_end_char = ""
        elif isinstance(claimant,Mention):
            self.claimant_sentence, self.claimant_tokens = get_serif_components_of_mention(claimant)
            self.claimant_type = claimant.entity_type
            self.claimant_id = claimant.id
            self.claimant_start_char = claimant.start_char
            self.claimant_end_char = claimant.end_char
        elif isinstance(claimant,ValueMention):
            self.claimant_sentence, self.claimant_tokens = get_serif_components_of_value_mention(claimant)
            self.claimant_type = claimant.value_type
            self.claimant_id = claimant.id
            self.claimant_start_char = claimant.start_char
            self.claimant_end_char = claimant.end_char
        else:
            raise TypeError ("Claimant must be None, Mention, or ValueMention")
        self.event_text = " ".join([x.text for x in self.event_tokens])
        self.event_governed_text = df.get_governed_text(self.event_tokens[0])
        self.event_sentence_text = self.event_sentence.text.replace("\n"," ")
        self.claimant_text = " ".join([x.text for x in self.claimant_tokens])

        self.event_dp_node = df.get_node_by_token(self.event_tokens[0])
        self.event_dp_parent = self.event_dp_node.parent
        self.event_dep_rel = self.event_dp_node.data.dep_rel
        if self.event_dp_parent is not None:
            self.event_dp_parent_text = self.event_dp_parent.data.text
        else:
            self.event_dp_parent_text = "NONE"
        self.modal_strength = self.event.claim_label # from MTDP
        if self.modal_strength == "pos":
            self.epistemic_strength = "true certain"
        elif self.modal_strength in ["pn","pp"]:
            self.epistemic_strength = "true uncertain"
        elif self.modal_strength == "neg":
            self.epistemic_strength = "false certain"

def extract_claims (serif_doc):
    claims = []
    try:
        df = DependencyFeature(serif_doc)
        mf = MTDPFeature(serif_doc, isReference=False)
        for sentence in serif_doc.sentences:
            if sentence.event_mention_set is None: continue
            for event_mention in sentence.event_mention_set:
                if event_mention.claim_role == "CLAIM":
                    for event_mention_arg in event_mention.arguments:
                        if event_mention_arg.role == "has_claimant":
                            mention = event_mention_arg.value
                            claim = Claim(serif_doc,df,mf,event_mention,mention)
                            claims.append(claim)
                elif event_mention.claim_role == "AUTHOR CLAIM":
                    claim = Claim(serif_doc,df,mf,event_mention,None)
                    claims.append(claim)
    except Exception as e:
        logger.info("extract_claims failed on {}\n{}".format(
            serif_doc.docid, e))
    return(claims)

################################################################################
# Add a single claim to a serif xml document. A claim is represeted as two
# ModalTemporalRelationArgument instances, one for the inner-claim event and one 
# for the conceiver.
# A claim is represented in a serifxml document in two ways
# 1. Set attribute is_aida_claim is to true on the on the ModalTemporalRelationArgument
#    of the ModalTemporalRelationMention for the inner claim event
# 2. Add an EventMentionArg to the inner-claim EventMention pointing to the 
#    conceiver Mention
#    Set attribute claim_role to CLAIM or AUTHOR CLAIM on the EventMention for
#    the inner-claim
#    Set attribute claim_role to CLAIMANT on the Mention for the conceiver
################################################################################
def add_claim_to_serif_doc (event_mtra, conceiver_mtra, claim_pattern_id=None):
    """Adds a claim to a serifxml3 document
    
    Parameters
    ----------
    event_mtra : ModalTemporalRelationArgument
        The mtra for the inner claim event

    conceiver_mtra : ModalTemporalRelationArgument
        The mtra for the conceiver.

    claim_pattern_id: str
        The id for the pattern in subgraph-pattern-matching that extracted this claim

    """
    if event_mtra is None:
        pass
    else:
        # One way to represent a claim in serif: Set an attribute 
        # on the MTRA.  However, it is difficult to extract the 
        # claimant/claim edges using only this attribute.
        event_mtra.is_aida_claim = True

        # Use existing EventMention from ModalTemporalRelationArgument
        eventMention = event_mtra.value

        if type(eventMention) != EventMention:
            eventMention.claim_role = "CLAIM ANOMALY"
        elif conceiver_mtra is None:
            eventMention.claim_role = "ORPHAN CLAIM"
        else:
            # Use existing Mention from parent
            mention = conceiver_mtra.value
            if isinstance(mention, Mention):
                EventMentionModel.add_new_event_mention_argument(
                    eventMention,
                    role="has_claimant",
                    mention=mention,
                    arg_score=1.0)
                mention.claim_role = "CLAIMANT"
                eventMention.claim_role = "CLAIM"
            elif isinstance(mention,str):
                # If the event_mtra.value is string "AUTHOR", there is
                # no Mention in the document corresponding to this
                # ModalTemporalRelationMention. Set claim_role
                # of the EventMention to "AUTHOR CLAIM"
                eventMention.claim_role = "AUTHOR CLAIM"
            eventMention.claim_label = event_mtra.relation_type
            if claim_pattern_id:
                if eventMention.claim_pattern_ids is None:
                    eventMention.claim_pattern_ids = json.dumps([claim_pattern_id])
                else:
                    eventMention.claim_pattern_ids = json.dumps(json.loads(eventMention.claim_pattern_ids).append(claim_pattern_id))


def test_csv ():
    claimCSV = ClaimCSV ()
    claimCSV.load_topics(
        "/nfs/raid66/u15/aida/data/LDC2021E11_AIDA_Phase_3_Practice_Topic_Source_Data_V2.0/docs/parent_children.tab",
        "/nfs/raid66/u15/aida/data/LDC2021E11_AIDA_Phase_3_Practice_Topic_Source_Data_V2.0/docs/topic_list.txt")
    # claimCSV.open("/nfs/mercury-13/u124/dzajic/projects/aida/experiments/doc_processing/expts/LDC2021E11.20211004A/text_analytics/serifxml/db_dump.csv")
    # claimCSV.process_serifxml("/nfs/mercury-13/u124/dzajic/expts/doc_processing/LDC2021E11.20211004A/text_analytics/serifxml/L0C04D4NL.rsd.xml")
    # claimCSV.process_filelist("/nfs/mercury-13/u124/dzajic/expts/doc_processing/LDC2021E11.20211004A/text_analytics/serifxml/claim.filelist")
    claimCSV.open("/nfs/mercury-13/u124/dzajic/aida/mtdp/output/LDC2021E11-09.24.2021-v3/claim.LDC2021E11-09.24.2021-v3.csv")
    claimCSV.process_filelist("/nfs/mercury-13/u124/dzajic/aida/mtdp/text_lists/LDC2021E11-09.24.2021-v3.filelist")
    # claimCSV.process_json("/nfs/raid83/u13/caml/users/mselvagg_ad/experiments/expts/doc_processing/LDC2021E11-09.24.2021-v3/merger/db_dump.json")
    claimCSV.close()

def test_claim ():
    parser = argparse.ArgumentParser()
    parser.add_argument("reference_dir")
    parser.add_argument("candidate_dir")
    parser.add_argument("docids")
    parser.add_argument("output")
    args = parser.parse_args()

    claim_main (args.reference_dir, args.candidate_dir, args.docids, args.output)

def claim_main (reference_dir, candidate_dir, docids, output):
    refCorpus = MTDPCorpus()
    canCorpus = MTDPCorpus()
    with open(docids) as DOCIDS:
        for line in DOCIDS:
            docid = line.rstrip()
            logger.info(docid)
            refFile = os.path.join(reference_dir, "{}.xml".format(docid))
            canFile = os.path.join(candidate_dir, "{}.xml".format(docid))
            if os.path.exists(refFile):
                refDoc = serifxml3.Document(refFile)
                refCorpus.add_document(refDoc, isReference=True)
            if os.path.exists(canFile):
                canDoc = serifxml3.Document(canFile)
                canCorpus.add_document(canDoc, isReference=False)
    ORG = open(output,'w')
    ORG.write("* reference corpus\n")
    refCorpus.write_org(ORG)
    ORG.write("* candidate corpus\n")
    canCorpus.write_org(ORG)

def dp_main (reference_dir, candidate_dir, docids, output):
    refCorpus = DependencyCorpus()
    canCorpus = DependencyCorpus()
    with open(docids) as DOCIDS:
        for line in DOCIDS:
            docid = line.rstrip()
            logger.info(docid)
            refFile = os.path.join(reference_dir, "{}.xml".format(docid))
            refDoc = serifxml3.Document(refFile)
            refCorpus.add_document(refDoc)
            canFile = os.path.join(candidate_dir, "{}.xml".format(docid))
            if os.path.exists(canFile):
                canDoc = serifxml3.Document(canFile)
                canCorpus.add_document(canDoc)
    ORG = open(output,'w')
    ORG.write("* reference corpus\n")
    refCorpus.write_org(ORG)
    ORG.write("* candidate corpus\n")
    canCorpus.write_org(ORG)
    ORG.close()



if __name__ == "__main__":
    # test_claim ()
    test_csv ()
