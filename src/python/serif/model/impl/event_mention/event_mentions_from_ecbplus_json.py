from serif.model.event_mention_model import EventMentionModel
import json


ACTION_TYPES = ["ACTION_OCCURRENCE", "ACTION_PERCEPTION", "ACTION_REPORTING", "ACTION_ASPECTUAL", "ACTION_STATE",
                "ACTION_CAUSATIVE", "ACTION_GENERIC"]

NEG_ACTION_TYPES = ["NEG_ACTION_OCCURRENCE", "NEG_ACTION_PERCEPTION", "NEG_ACTION_REPORTING", "NEG_ACTION_ASPECTUAL",
                    "NEG_ACTION_STATE", "NEG_ACTION_CAUSATIVE", "NEG_ACTION_GENERIC"]


EVENT_MENTION_TYPES = set(ACTION_TYPES + NEG_ACTION_TYPES)


class EventMentionsFromECBPlusJson(EventMentionModel):
    ''' Model for parsing annotated corpus into Serifxml by first reading in entity and event mentions
    '''

    def __init__(self, doc_to_anno_mapping, **kwargs):

        ''':param annotations_file: json of annotated spans per docid'''

        super(EventMentionsFromECBPlusJson, self).__init__(**kwargs)

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

    def add_event_mentions_to_sentence(self, sentence):

        doc_anno = self.doc_to_anno[sentence.document.docid]

        source_markables_within_sentence = self.find_source_markables_within_sentence(sentence, doc_anno)

        new_event_mentions = []
        for markable in source_markables_within_sentence:
            assert len(list(markable.keys())) == 1
            m_id = list(markable.keys())[0]
            m_dict = markable[m_id]

            assert len(set(doc_anno["tokens"][str(t)]["sentence"] for t in m_dict["token_anchors"])) == 1  # all tokens coming from same sentence
            token_idx_range = [doc_anno["tokens"][str(t)]["number"] for t in m_dict["token_anchors"]]
            # assert token_idx_range == list(range(token_idx_range[0], token_idx_range[-1] + 1))  # mention consists of contiguous tokens in same sentence

            start_token_idx = token_idx_range[0]
            end_token_idx = token_idx_range[-1]

            if start_token_idx is not None and end_token_idx is not None:  # note: 0 is not None
                new_event_mentions.extend(EventMentionModel.add_new_event_mention(sentence.event_mention_set, m_dict["type"],
                                                                                  sentence.token_sequence[start_token_idx],
                                                                                  sentence.token_sequence[end_token_idx],
                                                                                  pattern_id=m_id,  # use pattern_id field to store id, avoid creating more fields
                                                                                  model="ECB+"))
        return new_event_mentions

    def find_source_markables_within_sentence(self, sentence, doc_anno):

        source_markables_within_sentence = []
        for m_id, m_dict in doc_anno["source_markables"].items():

            if m_dict["type"] in EVENT_MENTION_TYPES:

                m_token_anchors = m_dict["token_anchors"]
                m_tokens = [doc_anno["tokens"][str(t)] for t in m_token_anchors]

                m_token_sent_idxs = [int(m_token["sentence"]) for m_token in m_tokens]
                assert len(set(m_token_sent_idxs)) == 1  # assert all tokens for a source markable come from same sentence
                m_sent_idx = m_token_sent_idxs[0]

                if m_sent_idx == sentence.sent_no:
                    source_markables_within_sentence.append({m_id: m_dict})

        return source_markables_within_sentence