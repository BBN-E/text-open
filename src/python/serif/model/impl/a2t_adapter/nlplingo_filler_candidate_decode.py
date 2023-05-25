import os.path

import serifxml3
from serif.model.impl.nlplingo_adapter.driver import NLPLingoDecoder

def main():
    input_list = "/d4m/ears/expts/48512.111822.granular_model_selection.v1/expts/48512.111822.granular_model_selection.v1.decode/expts/sub-expts/48512.111822.granular_model_selection.v1.decode.p3_granular_zho_sample/expts/pyserif_nlp/serif_files_all.list"
    nlplingo_config_path = "/home/hqiu/tmp/decode_ner.json"
    output_dir = "/home/hqiu/tmp/serif_ner_test"
    nlplingo_decoder = NLPLingoDecoder(nlplingo_config_path)
    nlplingo_decoder.load_model()
    serif_docs = list()
    with open(input_list) as fp:
        for i in fp:
            i = i.strip()
            serif_doc = serifxml3.Document(i)
            serif_docs.append(serif_doc)
    nlplingo_decoder.process_documents(serif_docs)

    for serif_doc in serif_docs:
        serif_doc.save(os.path.join(output_dir, "{}.xml".format(serif_doc.docid)))

if __name__ == "__main__":
    main()