import serifxml3
from serif.model.impl.round_tripper_util import find_matching_mention
from serif.model.relation_mention_model import RelationMentionModel


class RoundTripperRelationMentionModel(RelationMentionModel):

    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperRelationMentionModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    # Overrides RelationMentionModel.add_relation_mentions_to_sentence
    def add_relation_mentions_to_sentence(self, sentence):
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.rel_mention_set is None:
            return []

        sentence.rel_mention_set.score = serif_doc_sentence.rel_mention_set.score

        relation_mentions = []
        for relation_mention in serif_doc_sentence.rel_mention_set or ():
            new_left_mention = find_matching_mention(relation_mention.left_mention, sentence)
            new_right_mention = find_matching_mention(relation_mention.right_mention, sentence)
            relation_mentions.extend(
                RelationMentionModel.add_new_relation_mention(sentence.rel_mention_set, relation_mention.type,
                                                              new_left_mention, new_right_mention,
                                                              relation_mention.tense, relation_mention.modality,
                                                              pattern=relation_mention.pattern,
                                                              score=relation_mention.score,
                                                              confidence=relation_mention.score,
                                                              raw_type=relation_mention.raw_type,
                                                              time_arg=relation_mention.time_arg,
                                                              time_arg_role=relation_mention.time_arg_role,
                                                              time_arg_score=relation_mention.time_arg_score
                                                              ))

        return relation_mentions
