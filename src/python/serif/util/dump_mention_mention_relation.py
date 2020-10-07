import os,sys,enum, multiprocessing

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

import serifxml3


def single_document_handler(serif_path):
    serif_doc = serifxml3.Document(serif_path)
    ret = list()
    for sentence in serif_doc.sentences:
        for mmr in sentence.rel_mention_set:
            left_mention = mmr.left_mention
            right_mention = mmr.right_mention
            relation_type = mmr.type
            marked_tokens = list()
            for token in sentence.token_sequence:
                s = ""
                if token == left_mention.tokens[0]:
                    s += "["
                if token == right_mention.tokens[0]:
                    s += "{"
                s += token.text
                if token == left_mention.tokens[-1]:
                    s += "]"
                if token == right_mention.tokens[-1]:
                    s += "}"
                marked_tokens.append(s)
            s = "type: {} left:{} right:{} sent:{}".format(relation_type,left_mention.text,right_mention.text," ".join(marked_tokens))
            ret.append(s)
    return ret

def main(serif_list):
    manager = multiprocessing.Manager()
    with manager.Pool(multiprocessing.cpu_count()) as pool:
        workers = list()
        with open(serif_list) as fp:
            for i in fp:
                i = i.strip()
                workers.append(pool.apply_async(single_document_handler,args=(i,)))
        for idx,i in enumerate(workers):
            i.wait()
            buf = i.get()
            for en in buf:
                print("{}".format(en))

if __name__ == "__main__":
    test_serif_list = "/nfs/raid88/u10/users/hqiu/learnit_data/wm_thanksgiving_alignment.030920/source_lists/x0"
    main(test_serif_list)