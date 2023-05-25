import logging

logger = logging.getLogger(__name__)


def spacy_tokenizer_adder(serif_sentence):
    token_sequence = serif_sentence.add_new_token_sequence()
    token_sequence.set_score(0.7)
    pos_sequence = serif_sentence.add_new_part_of_speech_sequence()
    pos_sequence.set_score(0.7)
    if "spacy_sentence" not in serif_sentence.aux:
        logger.warning("Cannot find spacy_sentence for {}, skipping!!".format(serif_sentence.text))
        return
    spacy_sentence = serif_sentence.aux["spacy_sentence"]
    spacy_token_to_char_offsets = serif_sentence.aux["spacy_token_to_char_offsets"]
    for spacy_token in spacy_sentence:
        start_offset, end_offset = spacy_token_to_char_offsets[spacy_token]

        serif_token = token_sequence.add_new_token(
            start_offset,
            end_offset,
            spacy_token.text,
            spacy_token.lemma_)

        pos_sequence.add_new_pos(
            serif_token, spacy_token.tag_, spacy_token.pos_ if spacy_token.pos_ is not None else "",
            spacy_token.dep_ if spacy_token.dep_ is not None else "")

    for token, spacy_token in zip(serif_sentence.token_sequence, spacy_sentence):
        head_index_in_serif_sentence = spacy_token.head.i - spacy_sentence.start
        token.head = serif_sentence.token_sequence[head_index_in_serif_sentence]

