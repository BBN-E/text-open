import logging
from collections import defaultdict

from serif.model.mention_coref_model import MentionCoreferenceModel

logger = logging.getLogger(__name__)


class EntityFromCorefChainModel(MentionCoreferenceModel):

    def __init__(self, **kwargs):
        super(EntityFromCorefChainModel, self).__init__(**kwargs)

    @staticmethod
    def build_coref_chain_to_mentions_map(serif_doc):
        '''group mentions by their coref id'''

        coref_chain_to_mentions = defaultdict(list)
        for sentence in serif_doc.sentences:
            for mention in sentence.mention_set:
                if mention.coref_chain is not None:
                    coref_chain_to_mentions[mention.coref_chain].append(mention)
        return coref_chain_to_mentions

    def add_entities_to_document(self, serif_doc):

        results = []
        coref_chain_to_mentions = self.build_coref_chain_to_mentions_map(serif_doc)

        for coref_chain, mentions in coref_chain_to_mentions.items():
            results.extend(MentionCoreferenceModel.add_new_entity(serif_doc.entity_set, mentions))

        return results