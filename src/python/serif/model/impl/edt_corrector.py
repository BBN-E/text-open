import bisect
import re

TAG_PATTERN = re.compile('<[^<>]+>', re.MULTILINE)

from serif.model.document_model import DocumentModel
from serif.theory.serif_theory import SerifTheory

from serif.theory.serif_offset_theory import SerifOffsetTheory
from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _ReferenceAttribute, _ReferenceListAttribute, _ChildTheoryElement, _ChildTheoryElementList

class EDTCorrector(DocumentModel):
    """
    This class is for correcting EDT offsets because literary every components cannot handle this correctly
    """

    def __init__(self, **kwargs):
        super(EDTCorrector, self).__init__(**kwargs)

    @staticmethod
    def get_edt_offset(char_to_addition_add, amp_key_list, char_off):
        return char_off - char_to_addition_add[amp_key_list[bisect.bisect(amp_key_list, char_off) - 1]]

    @staticmethod
    def dfs_visit_theory(serif_theory, char_to_addition_add, amp_key_list, visited):
        if serif_theory in visited:
            return
        visited.add(serif_theory)
        if isinstance(serif_theory, SerifOffsetTheory):
            serif_theory.start_edt = EDTCorrector.get_edt_offset(char_to_addition_add, amp_key_list,
                                                                   serif_theory.start_char)
            serif_theory.end_edt = EDTCorrector.get_edt_offset(char_to_addition_add, amp_key_list,
                                                                 serif_theory.end_char)
        for attr_name, potential_attr in vars(serif_theory).items():
            if isinstance(potential_attr, SerifSequenceTheory) or isinstance(potential_attr, list):
                for i in potential_attr:
                    EDTCorrector.dfs_visit_theory(i, char_to_addition_add, amp_key_list, visited)
            elif isinstance(potential_attr, SerifTheory):
                EDTCorrector.dfs_visit_theory(potential_attr, char_to_addition_add, amp_key_list, visited)



    def process_document(self, serif_doc):
        # Step 1 build EDT mapping
        original_text = serif_doc.original_text.contents
        char_to_addition_add = dict()
        char_to_addition_add[0] = 0
        acc = 0
        for i in TAG_PATTERN.finditer(original_text):
            start_char_off, end_char_off = i.span()
            char_to_addition_add[end_char_off] = acc + end_char_off - start_char_off
            acc += end_char_off - start_char_off
        amp_key_list = sorted(char_to_addition_add.keys())

        # Step 2: for every element in the document, change its EDT offset. We need DFS here.
        EDTCorrector.dfs_visit_theory(serif_doc, char_to_addition_add, amp_key_list, set())

