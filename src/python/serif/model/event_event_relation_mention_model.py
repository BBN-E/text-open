from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *
from serifxml3 import EventMention, ICEWSEventMention


class EventEventRelationMentionModel(BaseModel):

    def __init__(self,**kwargs):
        super(EventEventRelationMentionModel,self).__init__(**kwargs)

    @abstractmethod
    def get_event_event_relation_mention_info(self, serif_doc):
        """
        :type serif_doc: Document
        :return: List where each element corresponds to one 
                 EventEventRelationMention. Each element consists of a 
                 relation type, a confidence, a model name string (can
                 be anything), and a list of "argument specs". Each 
                 argument spec is a tuple containing a role string and 
                 an EventMention. An ICEWSEventMention can be substituted
                 for the EventMention.
        :rtype: list(tuple(str, float, str, list(tuple(str, EventMention))))
        """
        pass

    def add_event_event_relation_mentions_to_document(self, serif_doc):
        
        # build necessary structure
        eerm_set = serif_doc.event_event_relation_mention_set
        if eerm_set is None:
            eerm_set =\
                serif_doc.add_new_event_event_relation_mention_set()
            ''':type: EventEventRelationMentionSet'''

        event_event_relation_mentions = []
        event_event_relation_mention_info =\
            self.get_event_event_relation_mention_info(serif_doc)

        for (relation_type, confidence, model_name, argument_list) in\
                event_event_relation_mention_info:

            # construct object
            eerm = eerm_set.add_new_event_event_relation_mention(
                relation_type, confidence, model_name)
                
            # add arguments
            for role, event_mention in argument_list:
                if isinstance(event_mention, EventMention):
                    eerm.add_new_event_mention_argument(role, event_mention)
                elif isinstance(event_mention, ICEWSEventMention):
                    eerm.add_new_icews_event_mention_argument(
                        role, event_mention)
                else:
                    raise RuntimeError(
                        "Bad argument type in EventEventRelationMention")

            event_event_relation_mentions.append(eerm)
        return event_event_relation_mentions
            
    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            validate_sentence_event_mention_sets(
                sentence, serif_doc.docid, i)
        self.add_event_event_relation_mentions_to_document(serif_doc)
