from serif.model.document_model import DocumentModel

class ConvertXPOSToUPOSParser(DocumentModel):

    def __init__(self,**kwargs):
        super(ConvertXPOSToUPOSParser, self).__init__(**kwargs)

    def get_token_to_preterminal_mapping(self,root,token_to_preterminal_map):
        if root.is_preterminal:
            tokens = root.tokens
            assert len(tokens) == 1
            token_to_preterminal_map[tokens[0]] = root
        for child in root:
            self.get_token_to_preterminal_mapping(child, token_to_preterminal_map)

    def process_document(self, serif_doc):
        """
        This model is basically used for changing tag of a pre-existing parse tree
        It's assuming
        1. You have POS populated, and you want to use upos as tag in parse tree
        :param serif_doc:
        :return:
        """
        for sentence in serif_doc.sentences:
            if sentence.parse is None or sentence.parse.root is None:
                continue
            token_to_preterminal_map = dict()
            self.get_token_to_preterminal_mapping(sentence.parse.root,token_to_preterminal_map)
            if len(sentence.token_sequence or ()) != len(sentence.pos_sequence or ()):
                continue
            for token,pos in zip(sentence.token_sequence or (), sentence.pos_sequence or ()):
                syn_node = token_to_preterminal_map[token]
                syn_node.tag = pos.upos
