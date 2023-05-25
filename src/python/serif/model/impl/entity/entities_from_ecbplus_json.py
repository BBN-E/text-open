import json
from serif.model.mention_coref_model import MentionCoreferenceModel

TIMES = ["TIME_DATE", "TIME_OF_THE_DAY", "TIME_DURATION", "TIME_REPETITION"]

LOCATIONS = ["LOC_GEO", "LOC_FAC", "LOC_OTHER"]

HUMAN_PARTICIPANTS = ["HUMAN_PART_PER", "HUMAN_PART_ORG", "HUMAN_PART_GPE", "HUMAN_PART_FAC", "HUMAN_PART_VEH",
                      "HUMAN_PART_MET", "HUMAN_PART_GENERIC"]

NON_HUMAN_PARTICIPANTS = ["NON_HUMAN_PART", "NON_HUMAN_PART_GENERIC"]

ENTITY_TYPES = set(TIMES + LOCATIONS + HUMAN_PARTICIPANTS + NON_HUMAN_PARTICIPANTS)

class EntitiesFromECBPlusJson(MentionCoreferenceModel):

    def __init__(self, doc_to_anno_mapping, **kwargs):
        super(EntitiesFromECBPlusJson, self).__init__(**kwargs)

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

    def add_entities_to_document(self, serif_doc):
        added_entities = list()

        for intra_document_coref in self.doc_to_anno[serif_doc.docid]["intra_document_corefs"]:

            assert len(intra_document_coref["target"]) == 1
            target_m_id = intra_document_coref["target"][0]
            type = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["type"]
            source_m_ids = set(intra_document_coref["sources"])

            if type not in ENTITY_TYPES:
                continue  # this must be an Event

            # aggregate serifxml mentions corresponding to source_m_ids
            source_mentions = list()
            for sentence in serif_doc.sentences:
                for mention in sentence.mention_set:
                    if mention.pattern in source_m_ids:
                        source_mentions.append(mention)

            print(type)
            print(source_mentions)
            print(source_m_ids)
            assert len(source_mentions) == len(source_m_ids)

            # fields from target markable
            r_id = intra_document_coref["r_id"]
            RELATED_TO = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["RELATED_TO"]
            TAG_DESCRIPTOR = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["TAG_DESCRIPTOR"]
            type = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["type"]

            added_entities.extend(
                MentionCoreferenceModel.add_new_entity(serif_doc.entity_set, source_mentions,
                                                       entity_type=type,
                                                       entity_subtype="UNDET",
                                                       is_generic=True))

        for cross_document_coref in self.doc_to_anno[serif_doc.docid]["cross_document_corefs"]:

            assert len(cross_document_coref["target"]) == 1
            target_m_id = cross_document_coref["target"][0]
            type = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["type"]
            source_m_ids = set(cross_document_coref["sources"])

            if type not in ENTITY_TYPES:
                continue  # this must be an Event

            # aggregate serifxml mentions corresponding to source_m_ids
            source_mentions = list()
            for sentence in serif_doc.sentences:
                for mention in sentence.mention_set:
                    if mention.pattern in source_m_ids:
                        source_mentions.append(mention)

            assert len(source_mentions) == len(source_m_ids)

            # fields from target markable
            note = cross_document_coref["note"]
            r_id = cross_document_coref["r_id"]
            RELATED_TO = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["RELATED_TO"]
            TAG_DESCRIPTOR = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["TAG_DESCRIPTOR"]
            instance_id = self.doc_to_anno[serif_doc.docid]["target_markables"][target_m_id]["instance_id"]  # for cross document corefs
            assert note == instance_id

            added_entities.extend(
                MentionCoreferenceModel.add_new_entity(serif_doc.entity_set, source_mentions,
                                                       entity_type=type,
                                                       entity_subtype="UNDET",
                                                       cross_document_instance_id=instance_id,
                                                       is_generic=True))

        return added_entities
