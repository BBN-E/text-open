import argparse
import json
from collections import defaultdict

import serifxml3

def parse_args():
    parser = argparse.ArgumentParser(description="convert serifxml to jsonlines format")
    parser.add_argument("-i", "--input_serifxml_list", type=str, required=True, help="input serifxml filepaths list")
    parser.add_argument("-o", "--output_jsonlines", type=str, required=True, help="output jsonlines filepath")
    args = parser.parse_args()
    return args

# def sent_level_token_offsets_to_doc_level_token_offsets(serif_doc):
#     offset_map = dict()
#     c = 0
#     for i, s in enumerate(serif_doc.sentences):
#         for j, t in enumerate(s.token_sequence):
#              offset_map[(i,j)] = c
#              c += 1
#     return offset_map

def sent_level_token_offsets_to_corpus_level_token_offsets(serif_docs):
    offset_map = dict()
    count = 0
    for i, serif_doc in enumerate(serif_docs):
        for j, sentence in enumerate(serif_doc.sentences):
            for k, token in enumerate(sentence.token_sequence):
                 offset_map[(serif_doc.docid, j, k)] = (count, token.text)
                 count += 1
    assert len(offset_map.keys()) == count
    return offset_map

def main():

    args = parse_args()

    with open(args.input_serifxml_list, "r") as f:
        serifxml_filepaths = [l.strip() for l in f.readlines()]

    serif_docs = sorted([serifxml3.Document(fp) for fp in serifxml_filepaths], key=lambda d: d.docid)  # make sure docs are sorted!
    assert len(set([d.docid for d in serif_docs])) == len(serif_docs)

    offset_map = sent_level_token_offsets_to_corpus_level_token_offsets(serif_docs)
    cross_document_id_to_event_mentions = defaultdict(list)

    sentences = []

    seen_event_mentions = set()
    seen_event_mentions_full_refs = set()

    INTRA_DOC_EVENT_CORPUS_LEVEL_ID_COUNTER = 0
    for d in serif_docs:

        sentences.extend([[t.text for t in s.token_sequence] for s in d.sentences])

        for e in d.event_set:

            if e.cross_document_instance_id is None:  # intra-doc events in ECB+ serifxmls don't have cross_document_instance_id, so create one for them
                e.cross_document_instance_id = "INTRA_DOC_EVENT_CORPUS_LEVEL_ID_{}_{}".format(d.docid, INTRA_DOC_EVENT_CORPUS_LEVEL_ID_COUNTER)
                INTRA_DOC_EVENT_CORPUS_LEVEL_ID_COUNTER += 1

            assert e.cross_document_instance_id is not None

            for em in e.event_mentions:
                start_token_index = em.owner_with_type("Sentence").token_sequence[int(em.semantic_phrase_start)].index()
                end_token_index = em.owner_with_type("Sentence").token_sequence[int(em.semantic_phrase_end)].index()

                start = offset_map[(d.docid, em.owner_with_type("Sentence").sent_no, start_token_index)][0]
                end = offset_map[(d.docid, em.owner_with_type("Sentence").sent_no, end_token_index)][0]

                assert(((d.docid, em.owner_with_type("Sentence").sent_no, start_token_index),
                        (d.docid, em.owner_with_type("Sentence").sent_no, end_token_index))) not in seen_event_mentions_full_refs
                assert (start, end) not in seen_event_mentions

                seen_event_mentions_full_refs.add(((d.docid, em.owner_with_type("Sentence").sent_no, start_token_index),
                                                   (d.docid, em.owner_with_type("Sentence").sent_no, end_token_index)))
                seen_event_mentions.add((start, end))
                cross_document_id_to_event_mentions[e.cross_document_instance_id].append([start, end])

    # Second pass: create singleton clusters for each event mention that didn't participate in clustering.
    # Note that nlplingo eventcoref_cross_document/decoder.py only returns coreferent spanpair predictions, which means
    #   that a number of event mentions in the original serifxmls might not have been present in the decoder's output;
    #   since they weren't deemed coreferent with any other event mentions by the decoder, we create singleton clusters
    #   for them.

    cross_document_id_for_singleton = 0

    for d in serif_docs:
        for s in d.sentences:
            for em in s.event_mention_set:

                start_token_index = em.owner_with_type("Sentence").token_sequence[int(em.semantic_phrase_start)].index()
                end_token_index = em.owner_with_type("Sentence").token_sequence[int(em.semantic_phrase_end)].index()

                start = offset_map[(d.docid, em.owner_with_type("Sentence").sent_no, start_token_index)][0]
                end = offset_map[(d.docid, em.owner_with_type("Sentence").sent_no, end_token_index)][0]

                if (start, end) not in seen_event_mentions:

                    seen_event_mentions.add((start, end))
                    seen_event_mentions_full_refs.add(((d.docid, em.owner_with_type("Sentence").sent_no, start_token_index),
                                                       (d.docid, em.owner_with_type("Sentence").sent_no, end_token_index)))

                    cross_document_id_to_event_mentions["SINGLETON_{}".format(cross_document_id_for_singleton)].append([start, end])
                    cross_document_id_for_singleton += 1

    print("# event mentions collected: {}".format(str(len(seen_event_mentions))))
    # print(sorted(list(seen_event_mentions)))
    # print(sorted(list(seen_event_mentions_full_refs)))
    # print(sorted(list(set(offset_map.values()))))

    clusters = [v for v in cross_document_id_to_event_mentions.values()]

    assert len([em for c in clusters for em in c]) == len(seen_event_mentions_full_refs) == len(seen_event_mentions)

    ret = {"doc_key": "CORPUS",
           "predicted_clusters": clusters,
           "sentences": sentences}

    ljsons = [json.dumps(ret)]

    with open(args.output_jsonlines, "w") as f:
        f.write("\n".join(ljsons))

if __name__ == '__main__':
    main()
