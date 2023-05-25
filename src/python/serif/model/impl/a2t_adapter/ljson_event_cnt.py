import json


def main():
    input_jsonl_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_chinese_sentences_all.ljson"
    type_to_cnt = dict()
    with open(input_jsonl_path) as fp:
        for i in fp:
            i = i.strip()
            en = json.loads(i)
            for start_idx, end_idx, event_type in en["event_spans"]:
                type_to_cnt[event_type] = type_to_cnt.get(event_type, 0) + 1
    
    print(type_to_cnt)
    
if __name__ == "__main__":
    main()