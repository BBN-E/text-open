from serif.model.document_model import DocumentModel
from serif.theory.document import Document

class AttachEventMentionAnchorNodeFromSemanticPhraseModel(DocumentModel):
    def __init__(self, **kwargs):
        super(AttachEventMentionAnchorNodeFromSemanticPhraseModel, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        # This is incomplete and only clean event_mention not consider event coreference and event event relation
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            if sentence.event_mention_set is not None and sentence.parse is not None and sentence.parse.root is not None:
                for event_mention in sentence.event_mention_set:
                    if event_mention.anchor_node is not None:
                        continue
                    elif event_mention.semantic_phrase_start is None or event_mention.semantic_phrase_end is None:
                        continue

                    start_token = sentence.token_sequence[event_mention.semantic_phrase_start]
                    end_token = sentence.token_sequence[event_mention.semantic_phrase_end]
                    potential_anchor_node = sentence.parse.get_covering_syn_node(start_token, end_token, ())
                    if potential_anchor_node is not None and potential_anchor_node.start_token == start_token and potential_anchor_node.end_token == end_token:
                        event_mention.anchor_node = potential_anchor_node
        return serif_doc
