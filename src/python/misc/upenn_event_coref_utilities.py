# Used in creating input for UPenn coref model by the better 
# script output_upenn_event_coref_input and the pyserif 
# model upenn_event_mention_coref_model


# Determines if we should skip over the event mention pair.
# We only want cases where the first event mention is earlier
# than the second.
def is_not_coref_pair(em1, em2):
    
    if em1 == em2:
        return True
    if em1.anchor_node.sent_no > em2.anchor_node.sent_no:
        return True
    if (em1.anchor_node.sent_no == em2.anchor_node.sent_no and
        em1.semantic_phrase_start > em2.semantic_phrase_start):
        return True
    if (em1.anchor_node.sent_no == em2.anchor_node.sent_no and
        em1.semantic_phrase_start == em2.semantic_phrase_start and
        em1.semantic_phrase_end > em2.semantic_phrase_end):
        return True
    if (em1.anchor_node.sent_no == em2.anchor_node.sent_no and
        em1.semantic_phrase_start == em2.semantic_phrase_start and
        em1.semantic_phrase_end == em2.semantic_phrase_end and
        em1.id > em2.id):  # Tie breaker
        return True

    return False


def get_sentence_tokens(serif_doc, event_mention):
    results = ""
    sentence = serif_doc.sentences[event_mention.anchor_node.sent_no]
    for token in sentence.token_sequence:
        if len(results) > 0:
            results += " "
        results += token.text
    return results
    
