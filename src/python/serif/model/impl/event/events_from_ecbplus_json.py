import json
from serif.model.event_mention_coref_model import EventMentionCoreferenceModel

ACTION_TYPES = ["ACTION_OCCURRENCE", "ACTION_PERCEPTION", "ACTION_REPORTING", "ACTION_ASPECTUAL", "ACTION_STATE",
                "ACTION_CAUSATIVE", "ACTION_GENERIC"]

NEG_ACTION_TYPES = ["NEG_ACTION_OCCURRENCE", "NEG_ACTION_PERCEPTION", "NEG_ACTION_REPORTING", "NEG_ACTION_ASPECTUAL",
                    "NEG_ACTION_STATE", "NEG_ACTION_CAUSATIVE", "NEG_ACTION_GENERIC"]

EVENT_TYPES = set(ACTION_TYPES + NEG_ACTION_TYPES)

class EventsFromECBPlusJson(EventMentionCoreferenceModel):

    def __init__(self, doc_to_anno_mapping, **kwargs):
        super(EventsFromECBPlusJson, self).__init__(**kwargs)

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

    def add_new_events_to_document(self, serif_doc):
        added_events = list()

        for intra_document_coref in self.doc_to_anno[serif_doc.docid]["intra_document_corefs"]:
            assert len(intra_document_coref["target"]) == 1
            target_m_id = intra_document_coref["target"][0]
            source_m_ids = set(intra_document_coref["sources"])
            type = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["type"]

            if type not in EVENT_TYPES:
                continue  # this must be an Entity

            # aggregate serifxml event mentions corresponding to source_m_ids
            source_mentions = list()
            for sentence in serif_doc.sentences:
                for event_mention in sentence.event_mention_set:
                    if event_mention.pattern_id in source_m_ids:
                        source_mentions.append(event_mention)

            assert len(source_mentions) == len(source_m_ids)

            # fields from target markable
            r_id = intra_document_coref["r_id"]
            RELATED_TO = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["RELATED_TO"]
            TAG_DESCRIPTOR = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["TAG_DESCRIPTOR"]
            type = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["type"]

            added_events.extend(
                EventMentionCoreferenceModel.add_new_event(serif_doc.event_set, source_mentions,
                                                           event_type=type))

        for cross_document_coref in self.doc_to_anno[serif_doc.docid]["cross_document_corefs"]:
            assert len(cross_document_coref["target"]) == 1
            target_m_id = cross_document_coref["target"][0]
            source_m_ids = set(cross_document_coref["sources"])
            type = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["type"]

            if type not in EVENT_TYPES:
                continue  # this must be an Entity

            # aggregate serifxml event mentions corresponding to source_m_ids
            source_mentions = list()
            for sentence in serif_doc.sentences:
                for event_mention in sentence.event_mention_set:
                    if event_mention.pattern_id in source_m_ids:
                        source_mentions.append(event_mention)

            assert len(source_mentions) == len(source_m_ids)

            # fields from target markable
            note = cross_document_coref["note"]
            r_id = cross_document_coref["r_id"]
            RELATED_TO = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["RELATED_TO"]
            TAG_DESCRIPTOR = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["TAG_DESCRIPTOR"]
            instance_id = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["instance_id"]  # for cross document corefs
            assert note == instance_id

            added_events.extend(
                EventMentionCoreferenceModel.add_new_event(serif_doc.event_set, source_mentions,
                                                           event_type=type,
                                                           cross_document_instance_id=instance_id))

        return added_events
