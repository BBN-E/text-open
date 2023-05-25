import logging
from serif.model.document_model import DocumentModel
from serif.theory.document import Document
from serif.theory.mention import Mention
from serif.theory.value_mention import ValueMention
from serif.xmlio import DanglingPointer

logger = logging.getLogger(__name__)

def find_lowest_common_ancestor(syn_node_1, syn_node_2):
    # https://www.hrwhisper.me/algorithm-lowest-common-ancestor-of-a-binary-tree
    visited = set()
    while syn_node_1 is not None and syn_node_2 is not None:
        if syn_node_1 is not None:
            if syn_node_1 in visited:
                return syn_node_1
            visited.add(syn_node_1)
            syn_node_1 = syn_node_1.parent
        if syn_node_2 is not None:
            if syn_node_2 in visited:
                return syn_node_2
            visited.add(syn_node_2)
            syn_node_2 = syn_node_2.parent
    return None


class EventMentionCleanerForJSerif(DocumentModel):
    def __init__(self, **kwargs):
        super(EventMentionCleanerForJSerif, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        # This is incomplete and only clean event_mention not consider event coreference and event event relation
        assert isinstance(serif_doc,Document)
        for sentence in serif_doc.sentences or []:
            if sentence.event_mention_set is not None:
                good_event_mentions = list()
                for event_mention in sentence.event_mention_set:
                    if event_mention.anchor_node is None and event_mention.semantic_phrase_start is not None and event_mention.semantic_phrase_end is not None:
                        start_token = event_mention.sentence.token_sequence[event_mention.semantic_phrase_start]
                        end_token = event_mention.sentence.token_sequence[event_mention.semantic_phrase_end]
                        if start_token.syn_node is not None and end_token.syn_node is not None:
                            common_syn_node = find_lowest_common_ancestor(start_token.syn_node, end_token.syn_node)
                            if common_syn_node is not None:
                                if abs(common_syn_node.start_token.index() - event_mention.semantic_phrase_start) < 2 and abs(common_syn_node.end_token.index() - event_mention.semantic_phrase_end) < 2:
                                    event_mention.anchor_node = common_syn_node
                    if event_mention.anchor_node is not None:
                        good_event_mentions.append(event_mention)
                sentence.event_mention_set._children.clear()
                sentence.event_mention_set._children.extend(good_event_mentions)
                for event_mention in sentence.event_mention_set:
                    allowed_event_mention_args = list()
                    for event_mention_arg in event_mention.arguments:
                        value = event_mention_arg.value
                        if isinstance(value, Mention) or isinstance(value, ValueMention):
                            allowed_event_mention_args.append(event_mention_arg)
                        elif isinstance(value, DanglingPointer):
                            pass
                        else:
                            logger.warning("Cannot preserve {} for JSerif".format(type(value)))
                    event_mention.arguments.clear()
                    event_mention.arguments.extend(allowed_event_mention_args)
        return serif_doc
