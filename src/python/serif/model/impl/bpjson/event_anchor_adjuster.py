import sys, os, logging

from serif.model.document_model import DocumentModel

logger = logging.getLogger(__name__)

class EventAnchorAdjuster(DocumentModel):
    def __init__(self, **kwargs):
        super(EventAnchorAdjuster, self).__init__(**kwargs)
        
        self.conversion_info = { "Epidemiplate": [ "outbreak-event", "disease" ] }

    def get_anchor_from_arg(self, event_arg):
        if event_arg.event_mention:
            return event_arg.event_mention.anchor_node
        if event_arg.mention:
            return event_arg.mention.atomic_head
        if event_arg.entity:
            mention = event_arg.entity.representative_mention()
            if mention is not None and mention.atomic_head is not None:
                return mention.atomic_head
            if mention.end_token is not None:
                return mention.end_token.syn_node
        return None

    def process_document(self, serif_doc):
        if serif_doc.event_set is None:
            return

        for event in serif_doc.event_set:

            if event.event_type not in self.conversion_info:
                continue
    
            #print ("Serif doc: " + serif_doc.docid)
            #print ("Original anchor: " + event.anchors[0].anchor_node.text)
            # Replace anchor
            good_anchor_roles = self.conversion_info[event.event_type]
            replaced = False
            for r in good_anchor_roles:
                for a in event.arguments:
                    if a.role == r:
                        anchor_synnode = self.get_anchor_from_arg(a)
                        if anchor_synnode is None:
                            continue
                        event.anchors = []
                        event.add_new_event_anchor(anchor_synnode)
                        replaced = True
                        break
                if replaced:
                    break
            
