import json

def main():
    input_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_train_sentences_all_ui_nlplingo.json"
    output_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_train_sentences_all_ui_nlplingo_8.json"
    with open(input_path) as fp:
        en = json.load(fp)
    en = en[:8]
    with open(output_path,'w') as wfp:
        json.dump(en,wfp)

if __name__ == "__main__":
    main()