import re
from abc import abstractmethod
from serif.model.document_model import DocumentModel
from serif.model.validate import *


penn_treebank_bracket_reprs = {'-LRB-': '(',    # () left/right round bracket
                               '-RRB-': ')',
                               '-LCB-': '{',    # {} left/right curly bracket
                               '-RCB-': '}',
                               '-LSB-': '[',    # [] left/right square bracket
                               '-RSB-': ']'}


class AMRParserModel(DocumentModel):

    def __init__(self, **kwargs):
        super(AMRParserModel, self).__init__(**kwargs)

    @abstractmethod
    def add_amr_parse_to_sentence(self, serif_sentence):
        pass

    def add_new_amr_parse(self, sentence, amr_string, score=0.9):
        ret = list()
        if amr_string is not None:
            parse = sentence.add_new_amr_parse(score, amr_string)
            ret.append(parse)
        return ret

    def process_document(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            self.add_amr_parse_to_sentence(sentence)

    def preprocess_penn_treebank_bracket_notation(self, sentence_tokens):
        '''
        :param sentence_tokens: ['John', '-LRB-', 'my', 'brother', '-RRB-', 'left', '.']
        :return:                ['John', '(', 'my', 'brother', ')', 'left', '.']
        '''

        return [penn_treebank_bracket_reprs.get(t, t) for t in sentence_tokens]