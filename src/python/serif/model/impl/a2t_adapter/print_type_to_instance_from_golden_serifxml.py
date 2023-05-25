import json
import serifxml3

def unary_span_assembler(serif_sentence, start_token, end_token):
    ret = []
    for idx, token in enumerate(serif_sentence.token_sequence):
        current = ""
        if token is start_token:
            current += "["
        current += token.text.replace("\r"," ").replace("\n"," ")
        if token is end_token:
            current += "]"
        ret.append(current)
    return " ".join(ret)


def single_document_handler(serif_path):
    serif_doc = serifxml3.Document(serif_path)
    type_to_marked_sentences = dict()
    for sentence in serif_doc.sentences:
        for event_mention in sentence.event_mention_set or ():
            start_token = sentence.token_sequence[event_mention.semantic_phrase_start]
            end_token = sentence.token_sequence[event_mention.semantic_phrase_end]
            event_type = event_mention.event_type
            type_to_marked_sentences.setdefault(event_type, list()).append(unary_span_assembler(sentence, start_token, end_token))
    return type_to_marked_sentences


def main():
    serif_list = "/nfs/raid88/u10/users/ychan-ad/BETTER/p2_granular_events/data/serifxml/devtest_p2/output.list"
    output_path = "/home/hqiu/tmp/better_p2_marked_event.json"
    type_to_marked_sentences_all = dict()
    with open(serif_list) as fp:
        for i in fp:
            i = i.strip()
            type_to_marked_sentences = single_document_handler(i)
            for ontology_type, sentences in type_to_marked_sentences.items():
                type_to_marked_sentences_all.setdefault(ontology_type,list()).extend(sentences)

    with open(output_path,'w') as wfp:
        json.dump(type_to_marked_sentences_all,wfp,indent=4,sort_keys=True,ensure_ascii=False)

if __name__ == "__main__":
    main()