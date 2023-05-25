from collections import defaultdict

import serifxml3
from serif.model.event_mention_coref_model import EventMentionCoreferenceModel
from serif.model.impl.round_tripper_util import find_matching_event_mention, find_matching_mention, \
    find_matching_entity, find_matching_value, find_matching_syn_node, find_matching_proposition
from serif.theory.sentence import Sentence


class RoundTripperEventModel(EventMentionCoreferenceModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperEventModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_new_events_to_document(self, document):

        added_events = []
        old_to_new_event_mapping = defaultdict(list)
        for event in self.serif_doc.event_set or ():
            # EventMentions make up an Event
            list_of_new_event_mentions = []
            for event_mention in event.event_mentions:
                sent_no = event_mention.owner_with_type(Sentence).sent_no
                new_sentence = document.sentences[sent_no]
                new_event_mention = find_matching_event_mention(
                    event_mention, new_sentence)
                list_of_new_event_mentions.append(new_event_mention)
            new_events = EventMentionCoreferenceModel.add_new_event(
                    document.event_set, list_of_new_event_mentions,
                    event_type=event.event_type,
                    genericity=event.genericity,
                    modality=event.modality,
                    tense=event.tense,
                    polarity=event.polarity,
                    completion=event.completion,
                    coordinated=event.coordinated,
                    over_time=event.over_time,
                    granular_template_type_attribute=event.granular_template_type_attribute,
                    annotation_id=event.annotation_id
            )

            # Add all child arguments
            for old_argument in event.arguments:
                if old_argument.entity is not None:
                    argument_object = find_matching_entity(old_argument.entity, document)
                elif old_argument.value is not None:
                    argument_object = find_matching_value(old_argument.value, document)
                elif old_argument.event_mention is not None:
                    sent_no = old_argument.event_mention.sent_no
                    new_sentence = document.sentences[sent_no]
                    argument_object = find_matching_event_mention(old_argument.event_mention, new_sentence)
                elif old_argument.mention is not None:
                    sent_no = old_argument.mention.sent_no
                    new_sentence = document.sentences[sent_no]
                    argument_object = find_matching_mention(old_argument.mention, new_sentence)
                else:
                    raise ValueError
                for new_event in new_events:
                    new_event.add_new_argument(old_argument.role, argument_object, old_argument.score)

            # Add all child anchors
            for old_anchor in event.anchors:
                anchor_prop = None
                if old_anchor.anchor_node is not None:
                    sent_no = old_anchor.anchor_node.sent_no
                    new_sentence = document.sentences[sent_no]
                    anchor_object = find_matching_syn_node(old_anchor.anchor_node, new_sentence.parse)
                elif old_anchor.anchor_event_mention is not None:
                    sent_no = old_anchor.anchor_event_mention.sent_no
                    new_sentence = document.sentences[sent_no]
                    anchor_object = find_matching_event_mention(old_anchor.anchor_event_mention, new_sentence)
                else:
                    raise ValueError
                if old_anchor.anchor_prop is not None:
                    sent_no = old_anchor.anchor_prop.sent_no
                    new_sentence = document.sentences[sent_no]
                    anchor_prop = find_matching_proposition(old_anchor.anchor_prop, new_sentence)
                for new_event in new_events:
                    new_event.add_new_event_anchor(anchor_object, anchor_prop)

            added_events.extend(new_events)

        return added_events
