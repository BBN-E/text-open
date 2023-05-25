# for converting ontonotes to serifxml with NER and COREF annotations

from serif.model.mention_model import MentionModel
import json


class MentionsAndCorefFromOntonotesJson(MentionModel):
    ''' Model for parsing annotated corpus into Serifxml by first reading in entity mention and coref annotations
    '''

    def __init__(self, doc_to_anno_mapping, **kwargs):

        ''':param annotations_file: json of annotated spans per docid'''

        super(MentionsAndCorefFromOntonotesJson, self).__init__(**kwargs)

        self.doc_to_anno = self.load_annotations(doc_to_anno_mapping)

    def load_annotations(self, annotations_file):
        '''
        :param mapping_file: json per-document span annotations file
        see example in /nfs/raid90/u10/users/brozonoy-ad/data/ontonotes.v2/docid_to_anno.en
        :return: {"docid": [{sent_0}, {sent_1}, ...]}
        '''
        docid_to_anno = dict()
        with open(annotations_file, "r") as f:
            lines = [l.strip().split() for l in f.readlines()]
            docid_to_anno_fp = {l[0]: l[1] for l in lines}
        for docid, anno in docid_to_anno_fp.items():
            with open(anno, "r") as f:
                anno_dict = json.load(f)
            docid_to_anno[docid] = anno_dict
        return docid_to_anno

    def add_mentions_to_sentence(self, sentence):

        new_mentions = []
        sent_anno = self.doc_to_anno[sentence.document.docid][sentence.sent_no]
        assert int(sent_anno['sent_no']) == sentence.sent_no
        sent_offsets = self.sent_tokens_to_start_end_offsets(sent_anno['text'])

        for span in sent_anno['spans'].keys():
            span_anno = sent_anno['spans'][span]

            if 'coref_id' in span_anno:
                coref_id = span_anno['coref_id']
            else:
                coref_id = None

            if 'enamex_type' in span_anno:
                entity_type = span_anno['enamex_type']
            else:
                entity_type = "UNDET"

            start_char, end_char = int(span.split("_")[0]), int(span.split("_")[1])
            start_token_idx, end_token_idx = self.offsets_to_start_end_tokens_indices(start_char, end_char,
                                                                                      sent_offsets)
            if start_token_idx and end_token_idx:
                new_mentions.extend(self.add_or_update_mention(sentence.mention_set, entity_type, "UNDET",
                                                               sentence.token_sequence[start_token_idx],
                                                               sentence.token_sequence[end_token_idx], coref_chain=coref_id,
                                                               loose_synnode_constraint=True))
        return new_mentions

    def sent_tokens_to_start_end_offsets(self, sent_text):
        tokens = sent_text.split(" ")
        offsets = []
        start_search = 0
        for t in tokens:
            start_char = sent_text.find(t, start_search)
            end_char = start_char + len(t) - 1
            offsets.append((start_char, end_char))
            start_search = end_char
        return offsets

    def offsets_to_start_end_tokens_indices(self, start_char, end_char, offsets):
        # print("=================")
        # print(offsets)
        # print(start_char, end_char)
        start_token_index = None
        end_token_index = None
        for i, offset in enumerate(offsets):
            if start_char == offset[0]:
                start_token_index = i
            if end_char == offset[1]:
                end_token_index = i
        if start_token_index is None or end_token_index is None:
            return None, None
        assert 0 <= start_token_index <= end_token_index
        return start_token_index, end_token_index
