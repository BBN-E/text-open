import json
import serifxml3

allowed_event_types = {
    "Apply-NPI",
    "Communicate-Event",
    "Death-from-Crisis-Event",
    "Disease-Infects",
    "Disease-Outbreak",
    "Natural-Phenomenon-Event-or-SoA",
    "Other-Government-Action",
    "Provide-Aid",
    "Refugee-Movement",
    "Weather-or-Environmental-Damage"
}

def main():
    input_serif_list = "/d4m/ears/expts/48430.700_chs.051722.v1/expts/nlplingo/default/serif_files.list"
    output_ljson_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_chinese_sentences_all.ljson"
    ret = []
    quota = 500
    type_to_cnt = dict()
    with open(input_serif_list) as fp:
        for i in fp:
            i = i.strip()
            serif_doc = serifxml3.Document(i)
            for sentence in serif_doc.sentences or ():
                sent_id = "{}#{}".format(serif_doc.docid, sentence.sent_no)
                if len(sentence.event_mention_set or ()) > 0:
                    event_types = []
                    for event_mention in sentence.event_mention_set:
                        if event_mention.event_type in allowed_event_types:
                            event_types.append(event_mention.event_type)
                    pass_threshold = False
                    for event_type in event_types:
                        if type_to_cnt.get(event_type, 0) < quota:
                            pass_threshold = True
                    if pass_threshold is True:
                        en = {
                            "tokens": [token.text for token in sentence.token_sequence],
                            "event_spans": [[event_mention.semantic_phrase_start, event_mention.semantic_phrase_end, event_mention.event_type] for event_mention in sentence.event_mention_set],
                            "parse": sentence.parse.root._treebank_str(),
                            "sent_id": sent_id
                        }
                        for event_type in event_types:
                            type_to_cnt[event_type] = type_to_cnt.get(event_type, 0) + 1
                        ret.append(en)
    with open(output_ljson_path,'w') as wfp:
        for i in ret:
            wfp.write("{}\n".format(json.dumps(i, ensure_ascii=False)))

if __name__ == "__main__":
    main()