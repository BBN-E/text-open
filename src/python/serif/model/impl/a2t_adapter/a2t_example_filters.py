import abc

from serif.theory.mention import Mention
from serif.theory.event_mention import EventMention

class Filter(object):
    @abc.abstractmethod
    def filter(self, elem) -> bool:
        pass

class MentionTypeFilter(Filter):
    def __init__(self, allowed_mention_types):
        self.allowed_mention_types = set(allowed_mention_types)

    def filter(self, elem) -> bool:
        assert len(elem) == 2
        if isinstance(elem[0],Mention) is False:
            return True
        mention = elem[0]
        if mention.mention_type.value.lower() not in self.allowed_mention_types:
            return False
        return True


class MentionEntityTypeFilter(Filter):
    def __init__(self, allowed_mention_entity_types):
        self.allowed_mention_entity_types = set(allowed_mention_entity_types)
        self.mention_to_best_entity_type = dict()

    def filter(self, elem) -> bool:
        assert len(elem) == 2
        if isinstance(elem[0],Mention) is False:
            return True
        mention = elem[0]
        if mention.resolve_entity_type_from_entity_set() not in self.allowed_mention_entity_types:
            return False
        return True

class PairMentionModelFilter(Filter):
    def __init__(self, allowed_model_names):
        self.allowed_model_names = set(allowed_model_names)

    def filter(self, elem) -> bool:
        assert len(elem) == 3
        mentions = set()
        if isinstance(elem[0], Mention):
            mentions.add(elem[0])
        if isinstance(elem[1], Mention):
            mentions.add(elem[1])
        for mention in mentions:
            if mention.model not in self.allowed_model_names:
                return False
        return True

class PairMentionEntityTypeMentionEntityTypeFilter(Filter):
    def __init__(self, allowed_mention_entity_type_pairs):
        self.allowed_entity_mention_entity_mention_type_pairs = set()
        self.mention_to_best_entity_type = dict()
        for allow_left, allow_right in allowed_mention_entity_type_pairs:
            self.allowed_entity_mention_entity_mention_type_pairs.add((allow_left, allow_right))

    def filter(self, elem):
        assert len(elem) == 3
        left_mention = elem[0]
        right_mention = elem[1]
        left_entity_type = left_mention.resolve_entity_type_from_entity_set()
        right_entity_type = right_mention.resolve_entity_type_from_entity_set()
        if (left_entity_type, right_entity_type) not in self.allowed_entity_mention_entity_mention_type_pairs:
            return False
        return True

class GenericEventArgEntityTypeFilter(Filter):
    def __init__(self, allowed_entity_types):
        self.allowed_entity_type = set(allowed_entity_types)

    def filter(self, elem):
        assert len(elem) == 3
        left_event_mention = elem[0]
        right_mention = elem[1]
        if isinstance(right_mention, EventMention):
            return True
        left_event_type = left_event_mention.event_type
        right_entity_type = right_mention.resolve_entity_type_from_entity_set()
        if right_entity_type not in self.allowed_entity_type:
            return False
        return True

class EventTypeArgEntityTypeFilter(Filter):
    def __init__(self, allowed_event_type_entity_type_pairs):
        self.allowed_event_type_entity_type_pairs = set()
        for allow_left, allow_right in allowed_event_type_entity_type_pairs:
            self.allowed_event_type_entity_type_pairs.add((allow_left, allow_right))

    def filter(self, elem):
        assert len(elem) == 3
        left_event_mention = elem[0]
        right_mention = elem[1]
        if isinstance(right_mention, Mention) is False and isinstance(right_mention, EventMention) is False:
            return True
        left_event_type = left_event_mention.event_type
        if isinstance(right_mention, Mention):
            right_entity_type = right_mention.resolve_entity_type_from_entity_set()
        else:
            right_entity_type = right_mention.event_type
        for allowed_left_type, allowed_right_type in self.allowed_event_type_entity_type_pairs:
            left_passed = False
            right_passed = False
            if allowed_left_type == "*":
                left_passed = True
            elif allowed_left_type == "EventMention":
                left_passed = True
            elif allowed_left_type == left_event_type:
                left_passed = True
            if allowed_right_type == "*":
                right_passed = True
            elif allowed_right_type == "Mention" and isinstance(right_mention, Mention):
                right_passed = True
            elif allowed_right_type == "EventMention" and isinstance(right_mention, EventMention):
                right_passed = True
            elif allowed_right_type == right_entity_type:
                right_passed = True
            if left_passed is True and right_passed is True:
                return True
        return False

class EventArgumentSourceFilter(Filter):
    def __init__(self, allowed_source_pairs):
        self.allowed_source_pairs = set()
        for allowed_source_pair in allowed_source_pairs:
            self.allowed_source_pairs.add(tuple(allowed_source_pair))

    def filter(self, elem):
        assert len(elem) == 3
        left_event_mention = elem[0]
        right_mention = elem[1]
        left_source = left_event_mention.model
        right_source = right_mention.model
        for allowed_left_source, allowed_right_source in self.allowed_source_pairs:
            left_passed = False
            right_passed = False
            if allowed_left_source == "*":
                left_passed = True
            elif allowed_left_source == left_source:
                left_passed = True
            if allowed_right_source == "*":
                right_passed = True
            elif allowed_right_source == right_source:
                right_passed = True
            if left_passed is True and right_passed is True:
                return True
        return False

class DisallowedWordEventFilter(Filter):
    def __init__(self, disallowed_words):
        self.disallowed_words = set(disallowed_words)

    def filter(self, elem):
        assert len(elem) == 2
        event_mention = elem[0]
        tokens = " ".join(i.text.lower() for i in event_mention.tokens)
        if tokens in self.disallowed_words:
            return False
        return True

class_name_to_class = {
    "MentionTypeFilter": MentionTypeFilter,
    "MentionEntityTypeFilter": MentionEntityTypeFilter,
    "PairMentionEntityTypeMentionEntityTypeFilter": PairMentionEntityTypeMentionEntityTypeFilter,
    "PairMentionModelFilter": PairMentionModelFilter,
    "EventTypeArgEntityTypeFilter": EventTypeArgEntityTypeFilter,
    "GenericEventArgEntityTypeFilter": GenericEventArgEntityTypeFilter,
    "EventArgumentSourceFilter": EventArgumentSourceFilter,
    "DisallowedWordEventFilter": DisallowedWordEventFilter
}
