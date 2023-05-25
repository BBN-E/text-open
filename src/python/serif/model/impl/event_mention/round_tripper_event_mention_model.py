import serifxml3

from serif.model.event_mention_model import EventMentionModel

from serif.model.impl.round_tripper_util import find_matching_mention, find_matching_value_mention, \
    find_matching_syn_node, find_matching_proposition


class RoundTripperEventMentionModel(EventMentionModel):

    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperEventMentionModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_event_mentions_to_sentence(self, sentence):
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.event_mention_set is None:
            return []

        sentence.event_mention_set.score = serif_doc_sentence.event_mention_set.score

        event_mentions = []
        for event_mention in serif_doc_sentence.event_mention_set:

            # Anchor node/tokens
            new_anchor_syn_node = None
            if event_mention.anchor_node is not None:
                new_anchor_syn_node = find_matching_syn_node(event_mention.anchor_node, sentence.parse)
            if new_anchor_syn_node is not None:
                start_token_in_new = new_anchor_syn_node.start_token
                end_token_in_new = new_anchor_syn_node.end_token
            else:
                start_token_in_new = sentence.token_sequence[event_mention.semantic_phrase_start]
                end_token_in_new = sentence.token_sequence[event_mention.semantic_phrase_end]

            # Anchor proposition
            new_anchor_prop = None
            if event_mention.anchor_prop is not None:
                new_anchor_prop = find_matching_proposition(event_mention.anchor_prop, sentence)

            new_event_mentions = EventMentionModel.add_new_event_mention(
                sentence.event_mention_set,
                event_mention.event_type,
                start_token_in_new,
                end_token_in_new,
                score=event_mention.score,
                pattern_id=event_mention.pattern_id,
                genericity=event_mention.genericity,
                polarity=event_mention.polarity,
                direction_of_change=event_mention.direction_of_change,
                tense=event_mention.tense,
                modality=event_mention.modality,
                state_of_affairs=event_mention.state_of_affairs,
                anchor_prop=new_anchor_prop,
                model=event_mention.model,
                genericityScore=event_mention.genericityScore,
                modalityScore=event_mention.modalityScore,
                cluster_id=event_mention.cluster_id,
                completion=event_mention.completion,
                coordinated=event_mention.coordinated,
                over_time=event_mention.over_time,
                granular_template_type_attribute=event_mention.granular_template_type_attribute
            )

            event_mentions.extend(new_event_mentions)

            for em in new_event_mentions:

                # Create EventMention argument specification
                for old_arg in event_mention.arguments:
                    if old_arg.mention is not None:
                        new_mention = find_matching_mention(old_arg.mention, sentence)
                        EventMentionModel.add_new_event_mention_argument(em, old_arg.role, new_mention, old_arg.score)
                    elif old_arg.value_mention is not None:
                        new_value_mention = find_matching_value_mention(old_arg.value_mention, sentence)
                        EventMentionModel.add_new_event_mention_argument(em, old_arg.role, new_value_mention,
                                                                         old_arg.score)
                    else:
                        raise NotImplementedError("Cannot support arg_type {}".format(type(old_arg.value)))

                # Create EventAnchors
                for anchor in event_mention.anchors:
                    new_anchor_syn_node = None
                    new_anchor_prop_node = None
                    if anchor.anchor_node is not None:
                        new_anchor_syn_node = find_matching_syn_node(anchor.anchor_node, sentence.parse)
                    if anchor.anchor_prop is not None:
                        new_anchor_prop_node = find_matching_proposition(anchor.anchor_prop, sentence)
                    em.add_new_event_mention_anchor(new_anchor_syn_node, new_anchor_prop_node)

                # Create EventMentionTypes
                for event_type in event_mention.event_types:
                    em.add_new_event_mention_type(event_type.event_type, event_type.score)

        return event_mentions
