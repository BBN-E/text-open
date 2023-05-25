from serif.model.document_model import DocumentModel
from serif.theory.document import Document

class MentionCleanerForJSerif(DocumentModel):
    def __init__(self,**kwargs):
        super().__init__(**kwargs)

    def process_document(self, serif_doc):
        # This is incomplete and only clean mention not consider entity coreference and mention mention relation
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            if sentence.mention_set is not None:
                good_mentions = list()
                for mention in sentence.mention_set:
                    if mention.syn_node is not None:
                        good_mentions.append(mention)
                sentence.mention_set._children.clear()
                sentence.mention_set._children.extend(good_mentions)
        return serif_doc