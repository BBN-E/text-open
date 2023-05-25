from serif.model.mention_model import MentionModel
import json


LOCATIONS = ["LOC_GEO", "LOC_FAC", "LOC_OTHER"]

HUMAN_PARTICIPANTS = ["HUMAN_PART_PER", "HUMAN_PART_ORG", "HUMAN_PART_GPE", "HUMAN_PART_FAC", "HUMAN_PART_VEH",
                      "HUMAN_PART_MET", "HUMAN_PART_GENERIC"]

NON_HUMAN_PARTICIPANTS = ["NON_HUMAN_PART", "NON_HUMAN_PART_GENERIC"]

TIMES = ["TIME_DATE", "TIME_OF_THE_DAY", "TIME_DURATION", "TIME_REPETITION"]  # include times as mentions because I don't think that value mentions have entity coreference objects for them


MENTION_TYPES = set(LOCATIONS + HUMAN_PARTICIPANTS + NON_HUMAN_PARTICIPANTS + TIMES)


class MentionsFromECBPlusJson(MentionModel):
    ''' Model for parsing annotated corpus into Serifxml by first reading in entity and event mentions
    '''

    def __init__(self, doc_to_anno_mapping, **kwargs):

        ''':param annotations_file: json of annotated spans per docid'''

        super(MentionsFromECBPlusJson, self).__init__(**kwargs)

        self.doc_to_anno = self.load_annotations(doc_to_anno_mapping)

    def load_annotations(self, annotations_file="/nfs/raid66/u11/users/brozonoy-ad/ecb+2serifxml/docid_to_json.map"):

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

        doc_anno = self.doc_to_anno[sentence.document.docid]

        source_markables_within_sentence = self.find_source_markables_within_sentence(sentence, doc_anno)

        new_mentions = []
        for markable in source_markables_within_sentence:
            assert len(list(markable.keys())) == 1
            m_id = list(markable.keys())[0]
            m_dict = markable[m_id]

            assert len(set(doc_anno["tokens"][str(t)]["sentence"] for t in m_dict["token_anchors"])) == 1  # all tokens coming from same sentence
            token_idx_range = [doc_anno["tokens"][str(t)]["number"] for t in m_dict["token_anchors"]]
            # assert token_idx_range == list(range(token_idx_range[0], token_idx_range[-1] + 1))  # mention consists of contiguous token(s) in same sentence

            start_token_idx = token_idx_range[0]
            end_token_idx = token_idx_range[-1]

            if start_token_idx is not None and end_token_idx is not None:  # note: 0 is not None
                new_mentions.extend(self.add_or_update_mention(sentence.mention_set, m_dict["type"], "UNDET",
                                                               sentence.token_sequence[start_token_idx],
                                                               sentence.token_sequence[end_token_idx],
                                                               pattern=str(m_id),  # use pattern field to store id, avoid creating more fields
                                                               model="ECB+",
                                                               loose_synnode_constraint=True))
        return new_mentions

    def find_source_markables_within_sentence(self, sentence, doc_anno):

        source_markables_within_sentence = []
        for m_id, m_dict in doc_anno["source_markables"].items():

            if m_dict["type"] in MENTION_TYPES:

                m_token_anchors = m_dict["token_anchors"]
                m_tokens = [doc_anno["tokens"][str(t)] for t in m_token_anchors]

                m_token_sent_idxs = [int(m_token["sentence"]) for m_token in m_tokens]
                assert len(set(m_token_sent_idxs)) == 1  # assert all tokens for a source markable come from same sentence
                m_sent_idx = m_token_sent_idxs[0]

                if m_sent_idx == sentence.sent_no:
                    source_markables_within_sentence.append({m_id: m_dict})

        return source_markables_within_sentence