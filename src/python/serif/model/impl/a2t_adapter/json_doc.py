import io
import time
import json

def serifxml_to_string(serif_doc):
    with io.BytesIO() as byte_io:
        serif_doc.save(byte_io)
        byte_io.seek(0)
        return byte_io.read().decode("utf-8")

def serif_json_seralizer(serif_doc):
    ret = dict()
    ret["doc_id"] = serif_doc.docid
    ret["sentences"] = list()
    ret["entity_relations"] = list()
    ret["mention_id_to_mention_loc"] = dict()
    ret["event_mention_id_to_event_mention_loc"] = dict()
    timestamp = int(time.time())
    for sent_no, sentence in enumerate(serif_doc.sentences or ()):
        sent_dict = dict()
        ret["sentences"].append(sent_dict)
        sent_dict["unary_markings"] = {
            "mentions": [],
            "event_mentions": []
        }
        sent_dict["sent_no"] = sent_no
        sent_dict["start_char"] = sentence.start_char
        sent_dict["end_char"] = sentence.end_char
        sent_dict["original_text"] = serif_doc.get_original_text_substring(sentence.start_char, sentence.end_char).replace("\n"," ").replace("\r"," ")
        sent_dict["tokens"] = list()
        for token_idx, token in enumerate(sentence.token_sequence):
            sent_dict["tokens"].append(
                {
                    "sent_no": sent_no,
                    "token_idx": token_idx,
                    "text": token.text,
                    "start_char":token.start_char,
                    "end_char":token.end_char
                }
            )
        sent_dict["mentions"] = list()
        for mention in sentence.mention_set or ():
            start_token_idx = mention.start_token.index()
            end_token_idx = mention.end_token.index()
            mention_id = "{}#{}#{}#{}".format(serif_doc.docid, type(mention).__name__, mention.id, timestamp)
            sent_dict["mentions"].append({
                "mention_id": mention_id,
                "start_token_idx": start_token_idx,
                "end_token_idx": end_token_idx,
                "mention_type": mention.mention_type.value,
                "entity_type": mention.entity_type,
                "model": mention.model,
                "pattern": mention.pattern,
                "confidence": mention.confidence,
                "is_frozen": False,
                "is_good": True,
                "corrected_span": [start_token_idx, end_token_idx]
            })
            last_mention_en = sent_dict["mentions"][-1]
            if mention.model == "Ask2Transformers":
                for ontology_type, template, score in json.loads(mention.pattern):
                    if template == "Human annotated":
                        last_mention_en["is_frozen"] = True
                        last_mention_en["is_good"] = True

            ret["mention_id_to_mention_loc"][mention_id] = (sent_no, len(sent_dict["mentions"]) - 1)
        sent_dict["event_mentions"] = list()
        for event_mention in sentence.event_mention_set or ():
            start_token_idx = event_mention.semantic_phrase_start
            end_token_idx = event_mention.semantic_phrase_end
            event_args = list()
            for event_arg in event_mention.arguments:
                role = event_arg.role
                ref = event_arg.value
                event_args.append({
                    "role": role,
                    "ref": "{}#{}#{}#{}".format(serif_doc.docid, type(ref).__name__, ref.id, timestamp),
                    "model": event_arg.model,
                    "pattern": event_arg.pattern,
                    "is_frozen": False,
                    "is_good": True,
                    "serif_type": type(ref).__name__
                })
            event_mention_id = "{}#{}#{}#{}".format(serif_doc.docid, type(event_mention).__name__, event_mention.id,
                                                    timestamp)
            sent_dict["event_mentions"].append({
                "event_mention_id": event_mention_id,
                "start_token_idx": start_token_idx,
                "end_token_idx": end_token_idx,
                "event_type": event_mention.event_type,
                "event_args": event_args,
                "model": event_mention.model,
                "score": event_mention.score,
                "pattern_id": event_mention.pattern_id,
                "is_frozen": False,
                "is_good": True,
                "corrected_span": [start_token_idx, end_token_idx]
            })
            last_event_mention = sent_dict["event_mentions"][-1]
            if event_mention.model == "Ask2Transformers":
                for ontology_type, template, score in json.loads(event_mention.pattern_id):
                    if template == "Human annotated":
                        last_event_mention["is_frozen"] = True
                        last_event_mention["is_good"] = True

            ret["event_mention_id_to_event_mention_loc"][event_mention_id] = (
                sent_no, len(sent_dict["event_mentions"]) - 1)
    for rel_mention in serif_doc.rel_mention_set or ():
        rel_id = "{}#{}#{}#{}".format(serif_doc.docid, type(rel_mention).__name__, rel_mention.id, timestamp)
        left_mention = rel_mention.left_mention
        right_mention = rel_mention.right_mention
        rel_type = rel_mention.type
        pattern = rel_mention.pattern
        score = rel_mention.score
        ret["entity_relations"].append({
            "left_mention_id": "{}#{}#{}#{}".format(serif_doc.docid, type(left_mention).__name__, left_mention.id,
                                                    timestamp),
            "right_mention_id": "{}#{}#{}#{}".format(serif_doc.docid, type(right_mention).__name__, right_mention.id,
                                                     timestamp),
            "rel_mention_id": rel_id,
            "rel_type": rel_type,
            "pattern": pattern,
            "score": score,
            "model": rel_mention.model,
            "is_frozen": False,
            "is_good": True,
        })
    ret["annotation"] = dict()
    return ret
