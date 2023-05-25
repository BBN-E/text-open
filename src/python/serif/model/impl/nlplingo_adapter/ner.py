from serif.model.impl.nlplingo_adapter.utils import get_serif_sentence_that_covers_offset


def write_ner_back_to_serifxml(serif_doc, document_p):
    for sent_p in document_p.sentences.values():
        for entity_mention in sent_p.entities.values():
            start_char = entity_mention.start
            end_char = entity_mention.end - 1   # -1 to be consistent with Serif

            max_label = None
            max_score = 0
            for label, score in entity_mention.entity_type_labels.items():
                if score > max_score:
                    max_label = label

            serif_sent = get_serif_sentence_that_covers_offset(start_char, end_char+1, serif_doc)
            """:type: serifxml3.theory.sentence.Sentence"""
            if serif_sent is None:
                print('WARNING: cannot find Serif sentence that covers char offsets {}-{}'.format(str(start_char), str(end_char)))
                continue

            start_token = None
            end_token = None
            for token in serif_sent.sentence_theories[0].token_sequence:
                if start_token is None and token.start_char == start_char:
                    start_token = token
                if token.end_char == end_char:
                    end_token = token

            if start_token is None:
                print('WARNING: cannot find Serif start_token for char {}'.format(start_char))
            if end_token is None:
                print('WARNING: cannot find Serif end_token for char {}'.format(end_char))

            if start_token is not None and end_token is not None:
                new_mention = serif_sent.mention_set.add_new_mention_from_tokens('NAME', max_label, start_token, end_token)
                new_mention.model = "NLPLingo"
                new_mention.confidence = max_score