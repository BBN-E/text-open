from serif.model.document_model import DocumentModel

# Removes any event mention arg that points to a Mention
# which doesn't have a SynNode. Also removes mentions with
# this characteristic. These are unloadable in JSerif.

class EventArgRemoverModel(DocumentModel):
    
    def __init__(self, **kwargs):
        super(EventArgRemoverModel,self).__init__(**kwargs)

    def process_document(self, serif_doc):
        for sentence in serif_doc.sentences:
            for event_mention in sentence.event_mention_set:
                new_args = []
                for arg in event_mention.arguments:
                    if arg.mention is None:
                        new_args.append(arg)
                        continue
                    if arg.mention.syn_node is not None:
                        new_args.append(arg)
                event_mention.arguments = new_args
            
            new_mentions = []
            for mention in sentence.mention_set:
                if mention.syn_node is not None:
                    new_mentions.append(mention)
            sentence.mention_set._children = new_mentions

        return serif_doc
