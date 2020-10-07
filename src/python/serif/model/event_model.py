from abc import abstractmethod

from serif.model.base_model import BaseModel
from serif.model.validate import *


class EventModel(BaseModel):

    def __init__(self,**kwargs):
        super(EventModel,self).__init__(**kwargs)

    @abstractmethod
    def get_event_info(self, serif_doc):
        """
        :type serif_doc: Document
        :return: List where each element corresponds to one Event. Each
                 element consists of a event type string and a list of 
                 EventMention objects which comprise the Event.
        :rtype: list(tuple(str, list(EventMention)))
        """
        pass

    def add_events_to_document(self, serif_doc):
        # build necessary structure
        event_set = serif_doc.event_set
        if event_set is None:
            event_set = serif_doc.add_new_event_set()
            ''':type: EventSet'''
        
        events = []
        event_info = self.get_event_info(serif_doc)
        for event_type, event_mentions in event_info:
            event = event_set.add_new_event(event_mentions, event_type)
            events.append(event)
        return events
                
    def process(self, serif_doc):
        for i, sentence in enumerate(serif_doc.sentences):
            validate_sentence_tokens(sentence, serif_doc.docid, i)
            validate_sentence_event_mention_sets(
                sentence, serif_doc.docid, i)
        self.add_events_to_document(serif_doc)
