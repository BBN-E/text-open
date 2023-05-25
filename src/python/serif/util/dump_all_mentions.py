import os
import serifxml3

def lookup_conceiver_as_kb_entity(conceiver):
    '''ensures that conceiver exists as entity in kb, returns None if conceiver not mapped'''
    if conceiver == "AUTHOR_NODE":
        return conceiver
    elif type(conceiver) is serifxml3.Mention:
        return conceiver
    else:
        return None


def modal_dep_trasverse(mtrm, mentioned_mentions):
    node = mtrm.node  # contains serifxml object
    node_type = node.modal_temporal_node_type  # 'ROOT_NODE', 'AUTHOR_NODE', Conceiver, Event
    rel = node.relation_type  # 'Depends-on', 'pos', 'pp', 'neg', ...
    if (
            node_type == "Event" or node_type == "Event_SIP") and node.event_mention is not None and node.value.event_type != "MTDP_EVENT":
        pass

    if len(mtrm.children) > 0:  # non-terminal node
        for child_mtrm in mtrm.children:
            # we're looking at the triples (node, child_rel, child_node)
            child_node = child_mtrm.node  # contains serifxml object
            child_node_type = child_node.modal_temporal_node_type  # 'ROOT_NODE', 'AUTHOR_NODE', Conceiver, Event
            child_rel = child_node.relation_type  # 'Depends-on', 'pos', 'pp', 'neg', ...
            if node_type == "Conceiver":  # keep only positive conceivers?
                kb_conceiver = lookup_conceiver_as_kb_entity(node.value)
                if kb_conceiver is not None and type(kb_conceiver) is serifxml3.Mention:
                    mentioned_mentions.add(kb_conceiver)
            modal_dep_trasverse(child_mtrm, mentioned_mentions)

def mention_to_entity_id(serif_entities):
    mention_to_cluster_ids = dict()
    for entity_id,serif_entity in enumerate(serif_entities):
        if serif_entity.model == "AllenNLPCoreferenceModel" or serif_entity.model == "UWCoreferenceModel":
            for mention in serif_entity.mentions:
                mention_to_cluster_ids.setdefault(mention,set()).add("eid_{}".format(entity_id))
    return mention_to_cluster_ids

def single_document_handler(serif_path):
    serif_doc = serifxml3.Document(serif_path)
    output_buf = list()

    mentions_in_conceivers = set()
    modal_dep_trasverse(serif_doc.modal_temporal_relation_mention_set.modal_root, mentions_in_conceivers)
    mention_cluster_ids = mention_to_entity_id(serif_doc.entity_set)
    for sentence in serif_doc.sentences:
        sent_no = sentence.sent_no
        token_marking_starts = dict()
        token_marking_ends = dict()
        mention_origin_strs = list()

        mention_in_event_args = set()
        for event_mention in sentence.event_mention_set or ():
            for event_arg in event_mention.arguments:
                if event_arg.role in {"has_location", "has_origin_location", "has_destination_location", "Place", "Origin", "Destination"}:
                    if event_arg.mention is not None:
                        mention_in_event_args.add(event_arg.mention)


        for mention in sentence.mention_set or ():
            is_mention_location = mention in mention_in_event_args
            is_mention_conceivers = mention in mentions_in_conceivers
            if mention.start_token is not None and mention.end_token is not None:
                start_token_idx = mention.start_token.index()
                end_token_idx = mention.end_token.index()
            else:
                start_token_idx = mention.syn_node.start_token.index()
                end_token_idx = mention.syn_node.end_token.index()
            if mention.mention_type.value.lower() == "name":
                token_marking_starts.setdefault(start_token_idx,list()).append("{")
                token_marking_ends.setdefault(end_token_idx,list()).append("}")
            else:
                token_marking_starts.setdefault(start_token_idx,list()).append("[")
                token_marking_ends.setdefault(end_token_idx,list()).append("]")
            mention_origin_strs.append("{}\t{}\t{}\t{}\t{}\t{}\t{}".format("!!" if is_mention_conceivers or is_mention_location else "",",".join(str(i) for i in mention_cluster_ids.get(mention,())),"is_loc" if is_mention_location else "","is_conc" if is_mention_conceivers else "" ,mention.get_original_text_substring(sentence.token_sequence[start_token_idx].start_char,sentence.token_sequence[end_token_idx].end_char).strip().replace("\t"," ").replace("\n"," ").replace("\r"," "),""+mention.mention_type.value, ""+mention.entity_type))
        marked_tokens = list()
        for token_idx,token in enumerate(sentence.token_sequence):
            escaped_token = ""
            for c in token_marking_starts.get(token_idx,()):
                escaped_token = escaped_token + c
            escaped_token += token.text.strip().replace("\t"," ").replace("\n"," ").replace("\r"," ")
            for c in token_marking_ends.get(token_idx,()):
                escaped_token = escaped_token + c
            marked_tokens.append(escaped_token)
        output_buf.append("### {}#{}\n{}\n{}".format(serif_doc.docid, sent_no, " ".join(marked_tokens), "\n".join(mention_origin_strs)))
    return output_buf


def main(serif_list_path):
    with open(serif_list_path) as fp:
        for idx, i in enumerate(fp):
            i = i.strip()
            print("\n".join(single_document_handler(i)))
            if idx >= 10:
                break


if __name__ == "__main__":
    # serif_list_path = "/d4m/ears/expts/48311.iarpa_final_demo.entity_linking.v1/expts/iarpa-covid-demo/entity_linking/en/serif.list"
    # serif_list_path = "/d4m/ears/expts/48311.iarpa_final_demo.entity_linking.v1/expts/iarpa-covid-demo/entity_linking/cn/serif.list"
    serif_list_path = "/d4m/ears/expts/48158.110221.aggr_entity_coref.v1/expts/iarpa-covid-demo/special_pyserif_stage/en/serif_sample.list"
    main(serif_list_path)