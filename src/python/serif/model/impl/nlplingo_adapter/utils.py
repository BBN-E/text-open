class DummySentenceTheory(object):
    def __init__(self):
        self.token_sequence = list()


class DummySentence(object):
    def __init__(self, sent_no, start_edt, end_edt):
        dummy_sentence_theory = DummySentenceTheory()
        self.sentence_theories = [dummy_sentence_theory]
        self.sentence_theory = dummy_sentence_theory
        self.sent_no = sent_no
        self.start_edt = start_edt
        self.end_edt = end_edt

def get_serif_sentence_that_covers_offset(start, end, serif_doc):
    """ Given a (start, end) char offset, get the serif_sentence that covers it
    """
    for st_index, sentence in enumerate(serif_doc.sentences):
        if len(sentence.sentence_theories[0].token_sequence) == 0:
            sentence_start = sentence.start_edt
            sentence_end = sentence.end_edt
        else:
            sentence_start = sentence.sentence_theories[0].token_sequence[0].start_edt
            sentence_end = sentence.sentence_theories[0].token_sequence[-1].end_edt

        if sentence_start <= start and end - 1 <= sentence_end:
            return sentence
    return None