
def find_matching_syn_node_rec(old_syn_node, new_syn_node):
    if old_syn_node is None or new_syn_node is None:
        return None
    if (old_syn_node.tag == new_syn_node.tag and
            old_syn_node.start_token.index() == new_syn_node.start_token.index() and
            old_syn_node.end_token.index() == new_syn_node.end_token.index()):
        return new_syn_node
    for child in new_syn_node:
        result = find_matching_syn_node_rec(old_syn_node, child)
        if result is not None:
            return result
    return None


# Given a SynNode from one serifxml Document, find an equivalent
# SynNode in new_parse (from a different Document)
def find_matching_syn_node(old_syn_node, new_parse):
    if new_parse is None:
        return None
    else:
        return find_matching_syn_node_rec(old_syn_node, new_parse.root)
        

# Given a Token from one serifxml Document, find an equivalent
# Mention in new_token_sequence (from a different Document)
def find_matching_token(old_token, new_token_sequence):
    for new_token in new_token_sequence or ():
        if (old_token.text == new_token.text and
                old_token.start_char == new_token.start_char and
                old_token.end_char == new_token.end_char):
            return new_token
    return None


# Given a Mention from one serifxml Document, find an equivalent
# Mention in new_sentence (from a different Document)
def find_matching_mention(old_mention, new_sentence):
    new_mention_set = new_sentence.mention_set
    new_token_sequence = new_sentence.token_sequence 
    new_parse = new_sentence.parse
    for new_mention in new_mention_set or ():
        if new_mention.mention_type != old_mention.mention_type:
            continue
        if new_mention.entity_type != old_mention.entity_type:
            continue

        # See if new_mention points to a SynNode that is 
        # identical to old_mention's SynNode
        if old_mention.syn_node is not None:
            matching_syn_node = find_matching_syn_node(old_mention.syn_node, new_parse)
            if matching_syn_node == new_mention.syn_node:
                return new_mention
            elif matching_syn_node is not None:
                matching_start_token = find_matching_token(old_mention.syn_node.start_token, new_token_sequence)
                matching_end_token = find_matching_token(old_mention.syn_node.end_token, new_token_sequence)
                if (matching_start_token == new_mention.start_token and
                        matching_end_token == new_mention.end_token):
                    return new_mention
            continue

        # mentions without syn nodes should have start and end 
        # tokens. See if new_mention has identical start and end
        # tokens to old_mentions start and end tokens.
        matching_start_token = find_matching_token(old_mention.start_token, new_token_sequence)
        matching_end_token = find_matching_token(old_mention.end_token, new_token_sequence)

        if (matching_start_token == new_mention.start_token and
                matching_end_token == new_mention.end_token):
            return new_mention
            
    return None


# Given a ValueMention from one serifxml Document, find an
# equivalent ValueMention in new_sentence (from a different
# Document)
def find_matching_value_mention(old_value_mention, new_sentence):
    new_value_mention_set = new_sentence.value_mention_set
    new_token_sequence = new_sentence.token_sequence
    for new_value_mention in new_value_mention_set or ():
        if new_value_mention.value_type != old_value_mention.value_type:
            continue

        matching_start_token = find_matching_token(old_value_mention.start_token, new_token_sequence)
        matching_end_token = find_matching_token(old_value_mention.end_token, new_token_sequence)

        if (matching_start_token == new_value_mention.start_token and
                matching_end_token == new_value_mention.end_token):
            return new_value_mention

    return None

# Given a Value from one serifxml Document, find an equivalent
# Value in the new_document (a different Document)
def find_matching_value(old_value, new_document):
    for new_value in new_document.value_set or ():
        # iterate through all sentence since value_mention.sent_no isn't required
        for new_sentence in new_document.sentences:
            new_value_mention = find_matching_value_mention(old_value.value_mention, new_sentence)
            if new_value_mention is not None and new_value_mention == new_value.value_mention:
                return new_value
    return None

# Given an Entity from one serifxml Document, find an equivalent
# Entity in new_document (a different Document)
def find_matching_entity(old_entity, new_document):
    for new_entity in new_document.entity_set or ():
        # check to see if new_entity matches old_entity
        # by iterating over old mentions and see if we have 
        # equivalent mentions
        match = True
        for old_mention in old_entity.mentions:
            sent_no = old_mention.sent_no
            new_sentence = new_document.sentences[sent_no]
            new_mention = find_matching_mention(old_mention, new_sentence)
            if new_mention not in new_entity.mentions:
                match = False
        if match:
            return new_entity
    return None


# Given a RelMention from one serifxml Document, find an equivalent
# RelMention in new_sentence (from a different Document)
def find_matching_relation_mention(old_rel_mention, new_sentence):
    new_left_mention = find_matching_mention(
        old_rel_mention.left_mention, new_sentence)
    new_right_mention = find_matching_mention(
        old_rel_mention.right_mention, new_sentence)

    for new_rel_mention in new_sentence.rel_mention_set or ():
        if new_rel_mention.type != old_rel_mention.type:
            continue
        if (new_rel_mention.left_mention == new_left_mention and
                new_rel_mention.right_mention == new_right_mention):
            return new_rel_mention
        
    return None


# Given an EventMention from on serifxml Document, find an equivalent
# EventMention in new_sentence (from a different Document). 
def find_matching_event_mention(old_event_mention, new_sentence):
    matching_anchor_node = None
    if old_event_mention.anchor_node is not None:
        matching_anchor_node = find_matching_syn_node(
            old_event_mention.anchor_node, new_sentence.parse)
    for new_event_mention in new_sentence.event_mention_set or ():
        if old_event_mention.event_type != new_event_mention.event_type:
            continue

        # Assumes that these chacteristics make a unique EventMention
        if (matching_anchor_node is not None and
                matching_anchor_node == new_event_mention.anchor_node):
            return new_event_mention
        if (old_event_mention.semantic_phrase_start is not None and
                old_event_mention.semantic_phrase_end is not None and
                old_event_mention.semantic_phrase_start == new_event_mention.semantic_phrase_start and
                old_event_mention.semantic_phrase_end == new_event_mention.semantic_phrase_end):
            return new_event_mention
    return None


# Given an ActorMention from one serifxml Document, find an 
# equivalent ActorMention in new_sentence (from a different
# Document)
def find_matching_actor_mention(old_actor_mention, new_sentence):
    new_mention = find_matching_mention(old_actor_mention.mention, new_sentence)
    for new_actor_mention in new_sentence.actor_mention_set or ():
        if (new_actor_mention.mention == new_mention and
                new_actor_mention.actor_uid == old_actor_mention.actor_uid):
            return new_actor_mention
    return None


# Given a Proposition from one serifxml Document, find an
# equivalent Proposition in new_sentence (from a different
# Document)
def find_matching_proposition(old_proposition, new_sentence):
    new_proposition_set = new_sentence.proposition_set
    new_parse = new_sentence.parse
    for new_proposition in new_proposition_set or ():
        if new_proposition.pred_type != old_proposition.pred_type:
            continue
        if old_proposition.head is not None:
            if new_proposition.head is None:
                continue
            else:
                matching_head = find_matching_syn_node(old_proposition.head, new_parse)
                if new_proposition.head != matching_head:
                    continue

        match = True
        for old_argument in old_proposition.arguments:
            new_argument = find_matching_argument(old_argument, new_sentence, new_proposition)
            if new_argument not in new_proposition.arguments:
                match = False
                break
        if match:
            return new_proposition
    return None


# Given an Argument from one serifxml Document, find an
# equivalent Argument in new_sentence (from a different
# Document)
def find_matching_argument(old_argument, new_sentence, new_proposition):
    new_parse = new_sentence.parse
    for new_argument in new_proposition.arguments or ():
        if new_argument.role != old_argument.role:
            continue
        if new_argument.mention is not None and old_argument.mention is not None:
            matching_mention = find_matching_mention(old_argument.mention, new_sentence)
            if matching_mention == new_argument.mention:
                return new_argument
        if new_argument.syn_node is not None and old_argument.syn_node is not None:
            matching_syn_node = find_matching_syn_node(old_argument.syn_node, new_parse)
            if matching_syn_node == new_argument.syn_node:
                return new_argument
        if new_argument.proposition is not None and old_argument.proposition is not None:
            matching_proposition = find_matching_proposition(old_argument.proposition, new_sentence)
            if matching_proposition == new_argument.proposition:
                return new_argument
    return None
