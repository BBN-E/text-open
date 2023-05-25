"""
There's a mention_coreference model under entity directory and it doesn't strictly follow get or create
pattern. Ideally we'd want to merge these two models together.
"""

from serif.model.mention_coref_model import MentionCoreferenceModel


class SimpleEntityModel(MentionCoreferenceModel):
    def __init__(self, **kwargs):
        super(SimpleEntityModel, self).__init__(**kwargs)

    def add_entities_to_document(self, serif_doc):
        all_reference_mentions = set()
        for serif_entity in serif_doc.entity_set or ():
            for mention in serif_entity.mentions:
                all_reference_mentions.add(mention)
        added_entities = list()
        for sentence in serif_doc.sentences:
            for mention in sentence.mention_set:
                if mention not in all_reference_mentions:
                    added_entities.extend(
                        MentionCoreferenceModel.add_new_entity(serif_doc.entity_set, [mention],
                                                               entity_type=mention.entity_type,
                                                               entity_subtype=mention.entity_subtype, is_generic=True,
                                                               model="SimpleEntityModel"))
        return added_entities