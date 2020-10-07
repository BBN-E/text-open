import os,sys,enum, multiprocessing

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

import serifxml3

def get_event_anchor(serif_em:serifxml3.EventMention,serif_sentence_tokens:serifxml3.TokenSequence):

    if serif_em.semantic_phrase_start is not None:
        serif_em_semantic_phrase_text = " ".join(i.text for i in serif_sentence_tokens[int(serif_em.semantic_phrase_start):int(serif_em.semantic_phrase_end)+1])
        return serif_em_semantic_phrase_text
    elif len(serif_em.anchors) > 0:
        return " ".join(i.anchor_node.text for i in serif_em.anchors)
    else:
        return serif_em.anchor_node.text
class SerifEventMentionTypingField(enum.Enum):
    event_type = "event_type"
    event_types = "event_types"
    factor_types = "factor_types"

def get_event_type(serif_em:serifxml3.EventMention,typing_field:SerifEventMentionTypingField):
    if typing_field == SerifEventMentionTypingField.event_type:
        return [[serif_em.event_type,serif_em.score]]
    ret = list()
    if typing_field == SerifEventMentionTypingField.event_types:
        for event_type in serif_em.event_types:
            ret.append([event_type.event_type,event_type.score])
    elif typing_field == SerifEventMentionTypingField.factor_types:
        for event_type in serif_em.factor_types:
            ret.append([event_type.event_type,event_type.score])
    else:
        raise NotImplementedError
    return ret

def get_event_arg(serif_em:serifxml3.EventMention):
    ret = list()
    for argument in serif_em.arguments:
        if isinstance(argument.mention,serifxml3.Mention):
            ret.append("event_arg: {}: {}".format(argument.role,argument.mention.text))
        elif isinstance(argument.value_mention,serifxml3.ValueMention):
            ret.append("event_arg: {}: {}".format(argument.role,argument.value_mention.text))
        else:
            raise NotImplementedError
    return ret

def assembley_event_frame(serif_em:serifxml3.EventMention):
    sentence = serif_em.owner_with_type(serifxml3.Sentence)
    sentence_theory = sentence.sentence_theory
    # sentence_text = " ".join(i.text for i in sentence_theory.token_sequence).replace("\n", " ").replace("\t", " ")
    event_anchor = get_event_anchor(serif_em, sentence_theory.token_sequence).replace("\n", " ").replace("\t", " ")
    event_type_groundings = []
    event_types_groundings = []
    factor_types_groundings = []

    event_type_groundings.append("event_type: {} , score: {}".format(serif_em.event_type, serif_em.score))
    for event_type in serif_em.event_types:
        event_types_groundings.append("event_types: {} , score: {}".format(event_type.event_type, event_type.score))
    for event_type in serif_em.factor_types:
        factor_types_groundings.append("factor_types: {} , score: {}".format(event_type.event_type, event_type.score))
    event_args = get_event_arg(serif_em)
    pattern_id = "pattern_id: {}".format(serif_em.pattern_id)
    # buf = "[EM]\nAnchor: {}\n{}\n{}\n{}\n{}\n{}\n[EM END]".format(event_anchor,pattern_id,"\n".join(event_type_groundings),"\n".join(event_types_groundings),"\n".join(factor_types_groundings),"\n".join(event_args))
    buf = "[EM START] Anchor: {}[EM END]".format(event_anchor)
    return buf

def single_document_hanlder(serif_path):
    eer_list = list()
    serif_doc = serifxml3.Document(serif_path)
    event_mention_in_eer = set()
    eer_frame = list()
    standalone_event_frame = list()

    for serif_eerm in serif_doc.event_event_relation_mention_set or []:

        serif_em_arg1 = None
        serif_em_arg2 = None
        relation_type = serif_eerm.relation_type
        confidence = serif_eerm.confidence
        for arg in serif_eerm.event_mention_relation_arguments:
            if arg.role == "arg1":
                serif_em_arg1 = arg.event_mention
            if arg.role == "arg2":
                serif_em_arg2 = arg.event_mention
        if serif_em_arg1 is not None and serif_em_arg2 is not None:
            event_mention_in_eer.add(serif_em_arg1)
            event_mention_in_eer.add(serif_em_arg2)
            sentence = serif_em_arg1.owner_with_type(serifxml3.Sentence)
            sentence_theory = sentence.sentence_theory
            sentence_text = " ".join(i.text for i in sentence_theory.token_sequence).replace("\n", " ").replace("\t", " ")
            left_event_buf = assembley_event_frame(serif_em_arg1)
            right_event_buf = assembley_event_frame(serif_em_arg2)
            buf = "[EER] docid:{} sentid:{} sent:{} {} eer_type:{}\tscore:{} {} [EER END]".format(serif_doc.docid,sentence.sent_no,sentence_text,left_event_buf,relation_type,confidence,right_event_buf)
            eer_frame.append((confidence, buf))

    for sent_idx,sentence in enumerate(serif_doc.sentences):
        sentence_theory = sentence.sentence_theory
        sentence_text = " ".join(i.text for i in sentence_theory.token_sequence).replace("\n", " ").replace("\t", " ")
        for event_mention in sentence_theory.event_mention_set:
            if event_mention not in event_mention_in_eer:
                event_buf = assembley_event_frame(event_mention)
                buf = "[SEVENT]\ndocid:{} sentid:{}\nsent:{}\n{}\n[SEVENT END]".format(serif_doc.docid,sentence.sent_no,sentence_text,event_buf)
                standalone_event_frame.append(buf)

    eer_list.extend(eer_frame)
    return eer_list

def main(serif_list):
    manager = multiprocessing.Manager()
    lock = manager.Lock()
    global_list = list() 
    with manager.Pool(multiprocessing.cpu_count()) as pool:
        workers = list()
        with open(serif_list) as fp:
            for i in fp:
                i = i.strip()
                workers.append(pool.apply_async(single_document_hanlder,args=(i,)))
        for idx,i in enumerate(workers):
            i.wait()
            buf = i.get()
            global_list.extend(buf)
            # print("{}".format(buf))
    global_list.sort(reverse=True)
    for i, eer in enumerate(global_list):
        print(i, eer)
    # print('\n'.join(global_list))


if __name__ == "__main__":
    import argparse
    parser= argparse.ArgumentParser()
    parser.add_argument("--serif_list",required=True)
    args = parser.parse_args()
    main(args.serif_list)
