from collections import defaultdict

import serifxml3

from serif.model.mention_model import MentionModel

from serif.model.impl.round_tripper_util import find_matching_token


class RoundTripperMentionModel(MentionModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperMentionModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_mentions_to_sentence(self, sentence):
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        if serif_doc_sentence.mention_set is not None:
            sentence.mention_set.name_score = serif_doc_sentence.mention_set.name_score
            sentence.mention_set.desc_score = serif_doc_sentence.mention_set.desc_score

        added_mentions = []
        old_to_new_mention_mapping = dict()
        for mention in serif_doc_sentence.mention_set:
            start_token = mention.start_token
            end_token = mention.end_token
            # if start_token and end_token are None, syn_node should be present
            if start_token is None or end_token is None and mention.syn_node:
                start_token = mention.syn_node.start_token
                end_token = mention.syn_node.end_token
            new_start_token = find_matching_token(start_token, sentence.token_sequence)
            new_end_token = find_matching_token(end_token, sentence.token_sequence)
            new_mentions = self.add_new_mention(sentence.mention_set, mention.entity_type,
                                                mention.mention_type.value,
                                                new_start_token, new_end_token,
                                                entity_subtype=mention.entity_subtype,
                                                is_metonymy=mention.is_metonymy,
                                                intended_type=mention.intended_type,
                                                role_type=mention.role_type,
                                                link_confidence=mention.link_confidence,
                                                confidence=mention.confidence,
                                                # Have to wait until all mentions are created to set pointers
                                                # parent_mention=mention.parent_mention,
                                                # child_mention=mention.child_mention,
                                                # next_mention=mention.next_mention,
                                                model=mention.model,
                                                pattern=mention.pattern,
                                                loose_synnode_constraint=True)
            old_to_new_mention_mapping[mention.id] = new_mentions
            added_mentions.extend(new_mentions)

        # Do a second pass over the MentionSet to update any pointers to other Mentions
        for mention in serif_doc_sentence.mention_set:
            new_parent, new_child, new_next = None, None, None
            if mention.parent_mention is not None:
                new_parents = old_to_new_mention_mapping[mention.parent_mention.id]
                new_parent = new_parents[0] if len(new_parents) > 0 else None
            if mention.child_mention is not None:
                new_children = old_to_new_mention_mapping[mention.child_mention.id]
                new_child = new_children[0] if len(new_children) > 0 else None
            if mention.next_mention is not None:
                new_nexts = old_to_new_mention_mapping[mention.next_mention.id]
                new_next = new_nexts[0] if len(new_nexts) > 0 else None
            for new_mention in old_to_new_mention_mapping[mention.id]:
                MentionModel.modify_mention_properties(new_mention, parent_mention=new_parent,
                                                       child_mention=new_child, next_mention=new_next)

        return added_mentions


