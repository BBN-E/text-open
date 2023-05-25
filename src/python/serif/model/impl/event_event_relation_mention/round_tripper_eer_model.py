import serifxml3
from serif.model.event_event_relation_mention_model \
    import EventEventRelationMentionModel
from serif.model.impl.round_tripper_util import find_matching_event_mention
from serif.theory.sentence import Sentence


class RoundTripperEERModel(EventEventRelationMentionModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperEERModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_event_event_relation_mentions_to_document(self, document):
        added_eerms = list()
        for eer in self.serif_doc.event_event_relation_mention_set or ():
            arg_role_to_em = dict()
            for argument in eer.event_mention_relation_arguments:
                sent_no = argument.event_mention.owner_with_type(
                    Sentence).sent_no
                new_sentence = document.sentences[sent_no]
                new_event_mention = find_matching_event_mention(
                    argument.event_mention, new_sentence)
                arg_role_to_em[argument.role] = new_event_mention
            added_eerms.extend(
                EventEventRelationMentionModel.add_new_event_event_relation_mention(
                    document.event_event_relation_mention_set,
                    eer.relation_type,
                    eer.confidence,
                    eer.model,
                    arg_role_to_em['arg1'],
                    arg_role_to_em['arg2'],
                    pattern=eer.pattern,
                    polarity=eer.polarity,
                    trigger_text=eer.trigger_text))
        return added_eerms
