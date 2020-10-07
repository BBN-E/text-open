from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *
from serifxml3 import Mention, ValueMention


class EventMentionModel(BaseModel):

    def __init__(self,**kwargs):
        super(EventMentionModel,self).__init__(**kwargs)

    @abstractmethod
    def get_event_mention_info(self, sentence):
        """
        :type sentence: Sentence
        :return: List where each element corresponds to one EventMention.
                 Each element consists of a event type string, an anchor 
                 SynNode (or None for no anchor), a score, and a list of 
                 "argument specs". Each argument spec is a tuple containing 
                 a role string, a Mention and, an arg score. A ValueMention 
                 can be substituted for the Mention.
        :rtype: list(tuple(str, list(tuple(str, SynNode, float, list(tuple(str, Mention, float))))))
        """
        pass

    def add_event_mentions_to_sentence(self, sentence):

        # build necessary structures
        event_mention_set = sentence.event_mention_set
        if event_mention_set is None:
            event_mention_set =\
                sentence.add_new_event_mention_set()
            ''':type: EventMentionSet'''

        # get objects to add
        event_mentions = []
        extractions = self.get_event_mention_info(sentence)

        for event_type, anchor_node, score, argument_list in extractions:

            # construct object
            event_mention = event_mention_set.add_new_event_mention(
                event_type, anchor_node, score)
        
            # add arguments
            for role, mention, arg_score in argument_list:
                if isinstance(mention, Mention):
                    event_mention.add_new_mention_argument(role, mention, arg_score)
                elif isinstance(mention, ValueMention):
                    event_mention.add_new_value_mention_argument(role, mention, arg_score)
                else:
                    raise RuntimeError("Bad argument type in EventMention")
                    
            event_mentions.append(event_mention)

        return event_mentions

    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            event_mention_set = sentence.event_mention_set
            if event_mention_set is None:
                event_mention_set = \
                    sentence.add_new_event_mention_set()
                ''':type: EventMentionSet'''
            self.add_event_mentions_to_sentence(sentence)

