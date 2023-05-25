from collections import defaultdict

from serif.model.mention_coref_model import MentionCoreferenceModel


class OrphanMentionToEntityModel(MentionCoreferenceModel):
    def __init__(self, **kwargs):
        super(OrphanMentionToEntityModel, self).__init__(**kwargs)

    def add_entities_to_document(self, serif_doc):
        e2m, m2e = self.build_mention_entity_maps(serif_doc)
        added_entities = list()
        for sentence in serif_doc.sentences:
            for mention in sentence.mention_set:
                # if (mention.entity_type == 'OTH' or
                #         mention.entity_type == 'UNDET'):
                #     continue
                if mention not in m2e:  # "orphan" mention currently without an entity over it
                    added_entities.extend(
                        MentionCoreferenceModel.add_new_entity(serif_doc.entity_set, [mention],
                                                               entity_type=mention.entity_type,
                                                               entity_subtype=mention.entity_subtype, is_generic=True))
        return added_entities

    def build_mention_entity_maps(self, serif_doc):
        m2e = defaultdict(list)  # the same mention might be governed by more than one entity
        e2m = defaultdict(list)
        for e in serif_doc.entity_set:
            for m in e.mentions:
                e2m[e].append(m)
                m2e[m].append(e)
        return e2m, m2e
