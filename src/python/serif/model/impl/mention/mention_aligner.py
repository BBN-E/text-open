from serif.model.document_model import DocumentModel
from serif.theory.enumerated_type import MentionType

import logging

logger = logging.getLogger(__name__)

class MentionAligner(DocumentModel):
    """Merges existing mentions with mentions created by MTDP"""

    def __init__(self,**kwargs):
        super(MentionAligner,self).__init__(**kwargs)

    def process_document(self, document):

        if document.modal_temporal_relation_mention_set is None:
            return document

        mention_id_to_new_id = {}

        for sentence in document.sentences:
            tokens_to_mention = {}

            for mention in sentence.mention_set:

                if (mention.start_token, mention.end_token) in tokens_to_mention:
                    existing_mention = tokens_to_mention[(mention.start_token, mention.end_token)]

                    if mention.entity_type == "MTDP_CONCEIVER":
                        ner_mention = existing_mention
                        mtdp_mention = mention
                    else:
                        ner_mention = mention
                        mtdp_mention = existing_mention

                    canon_mention = mtdp_mention
                    canon_mention.entity_type = ner_mention.entity_type
                    canon_mention.id = ner_mention.id
                    mention_id_to_new_id[mtdp_mention.id] = canon_mention.id

                    tokens_to_mention[(mention.start_token, mention.end_token)] = canon_mention
                else:
                    tokens_to_mention[(mention.start_token, mention.end_token)] = mention

            sentence.mention_set._children = tokens_to_mention.values()

        for mtrm in document.modal_temporal_relation_mention_set:
            if mtrm.node.id in mention_id_to_new_id:
                mtrm.node.id = mention_id_to_new_id[mtrm.node.id]

        return document
