import argparse
import json

import serifxml3

def parse_args():
    parser = argparse.ArgumentParser(description="convert serifxml to jsonlines format")
    parser.add_argument("-i", "--input_serifxml_list", type=str, required=True, help="input serifxml filepaths list")
    parser.add_argument("-o", "--output_jsonlines", type=str, required=True, help="output jsonlines filepath")
    args = parser.parse_args()
    return args

def sent_level_token_offsets_to_doc_level_token_offsets(serif_doc):
    offset_map = dict()
    c = 0
    for i, s in enumerate(serif_doc.sentences):
        for j, t in enumerate(s.token_sequence):
             offset_map[(i,j)] = c
             c += 1
    return offset_map

def main():

    args = parse_args()

    with open(args.input_serifxml_list, "r") as f:
        serifxml_filepaths = [l.strip() for l in f.readlines()]

    ljsons = []
    for serifxml_filepath in serifxml_filepaths:

        d = serifxml3.Document(serifxml_filepath)
        doc_key = d.docid

        sentences = [[t.text for t in s.token_sequence] for s in d.sentences]

        offset_map = sent_level_token_offsets_to_doc_level_token_offsets(d)
        clusters = []
        seen_entity_mentions_ids = set()

        # document-level entities
        for e in d.entity_set:
            entity_cluster = []
            for m in e.mentions:
                start = offset_map[(m.sentence.sent_no, m.start_token.index())]
                end = offset_map[(m.sentence.sent_no, m.end_token.index())]
                entity_cluster.append([start, end])
                seen_entity_mentions_ids.add(m.id)
            clusters.append(entity_cluster)

        # collect remaining singletons
        for s in d.sentences:
            for m in s.mention_set:
                if m.id not in seen_entity_mentions_ids:
                    start = offset_map[(m.sentence.sent_no, m.start_token.index())]
                    end = offset_map[(m.sentence.sent_no, m.end_token.index())]
                    entity_cluster = [[start, end]]
                    seen_entity_mentions_ids.add(m.id)
                    clusters.append(entity_cluster)

        ret = {"doc_key": doc_key,
               "predicted_clusters": clusters,
               "sentences": sentences}

        ljsons.append(json.dumps(ret))

    with open(args.output_jsonlines, "w") as f:
        f.write("\n".join(ljsons))

if __name__ == '__main__':
    main()
