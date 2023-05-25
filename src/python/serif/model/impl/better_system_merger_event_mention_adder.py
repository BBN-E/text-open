import gzip
import logging

from serif.model.document_model import DocumentModel

event_mention_like_role_str = {"REF_EVENT"}
mention_like_role_str = {"AGENT", "PATIENT"}

logger = logging.getLogger(__name__)


class EventFrame():
    def __init__(self, doc_id, sent_no, trigger_start_idx, trigger_end_idx, trigger_type, trigger_score):
        self.doc_id = doc_id
        self.sent_no = sent_no
        self.trigger_start_idx = trigger_start_idx
        self.trigger_end_idx = trigger_end_idx
        self.trigger_type = trigger_type
        self.role_mention_span_pairs = []
        self.role_event_mention_span_pairs = []
        self.trigger_score = trigger_score


def find_synnode_for_token_span(serif_sentence, start_token, end_token):
    if serif_sentence.parse is not None:
        node = serif_sentence.parse.get_covering_syn_node(start_token, end_token, [])
        if node.start_token == start_token and node.end_token == end_token:
            return node
    return None


def find_or_create_a_mention(serif_sentence, start_token, end_token, model):
    for mention in serif_sentence.mention_set or ():
        if mention.start_token == start_token and mention.end_token == end_token:
            return mention
    potential_synnode = find_synnode_for_token_span(serif_sentence, start_token, end_token)
    if potential_synnode is None:
        mention = serif_sentence.mention_set.add_new_mention_from_tokens("NONE", "NONE", start_token, end_token)
    else:
        mention = serif_sentence.mention_set.add_new_mention(potential_synnode, "NONE", "NONE")
    mention.model = model
    return mention


def find_event_mention(serif_sentence, start_token, end_token):
    for event_mention in serif_sentence.event_mention_set or ():
        if event_mention.start_token == start_token and event_mention.end_token == end_token:
            return event_mention
    return None


class BETTERSystemMergerEventMentionAdder(DocumentModel):
    def __init__(self, combined_entries_path, **kwargs):
        super(BETTERSystemMergerEventMentionAdder, self).__init__(**kwargs)
        self.grouped_event_frames = dict()  # doc_id, sent_no to event_frames
        with gzip.open(combined_entries_path, 'rt') as rfp:
            for i in rfp:
                # Business-Event-or-SoA 7b661780-68ea-52b6-8b4b-818afa0bb026 TRIGGER 6#4#4 0.0332639 AGENT 6#0#0 0.0306142 AGENT 6#2#2 0.0306142 PATIENT 6#5#5 0.0292262 PATIENT 6#10#10 0.0292262 REF_EVENT 6#18#18 0.197259
                i = i.strip()
                entries = i.split("\t")
                event_type, doc_id, trigger, trigger_offset, trigger_score = entries[:5]
                trigger_score = float(trigger_score)
                sent_no, trigger_start, trigger_end = trigger_offset.split("#")
                sent_no = int(sent_no)
                trigger_start = int(trigger_start)
                trigger_end = int(trigger_end)
                event_frame = EventFrame(doc_id, sent_no, trigger_start, trigger_end, event_type, trigger_score)
                self.grouped_event_frames.setdefault((doc_id, sent_no), list()).append(event_frame)
                arguments = entries[5:]
                for mover in range(0, len(arguments), 3):
                    role, role_offset, role_score = arguments[mover], arguments[mover + 1], float(arguments[mover + 2])
                    sent_no, role_start, role_end = role_offset.split("#")
                    sent_no = int(sent_no)
                    role_start = int(role_start)
                    role_end = int(role_end)
                    if role in event_mention_like_role_str:
                        event_frame.role_event_mention_span_pairs.append(
                            [role, sent_no, role_start, role_end, role_score])
                    elif role in mention_like_role_str:
                        event_frame.role_mention_span_pairs.append([role, sent_no, role_start, role_end, role_score])
                    else:
                        raise NotImplementedError("Not sure how to handle {}".format(role))

    def process_document(self, serif_doc):
        event_frame_to_em = dict()
        for sent_no, serif_sentence in enumerate(serif_doc.sentences):
            if serif_sentence.mention_set is None:
                serif_sentence.add_new_mention_set()
            if serif_sentence.event_mention_set is None:
                serif_sentence.add_new_event_mention_set()
            # First pass, resolve event_mention(trigger) and mention like arguments
            # We don't have to erase mention set. But we do have to erase event mention set here.
            serif_sentence.event_mention_set._children.clear()
            for event_frame in self.grouped_event_frames.get((serif_doc.docid, sent_no), ()):
                potential_syn_node = find_synnode_for_token_span(serif_sentence, serif_sentence.token_sequence[
                    event_frame.trigger_start_idx], serif_sentence.token_sequence[event_frame.trigger_end_idx])
                em = serif_sentence.event_mention_set.add_new_event_mention(event_frame.trigger_type,
                                                                            potential_syn_node,
                                                                            event_frame.trigger_score)
                em.semantic_phrase_start = event_frame.trigger_start_idx
                em.semantic_phrase_end = event_frame.trigger_end_idx
                em.add_new_event_mention_type(event_frame.trigger_type, event_frame.trigger_score)
                em.model = type(self).__name__
                event_frame_to_em[event_frame] = em
                # This pass, we only resolve mention as event_mention may not be created yet
                for role, m_sent_no, role_start, role_end, role_score in event_frame.role_mention_span_pairs:
                    another_serif_sentence = serif_doc.sentences[m_sent_no]
                    entity_mention = find_or_create_a_mention(another_serif_sentence,
                                                              another_serif_sentence.token_sequence[role_start],
                                                              another_serif_sentence.token_sequence[role_end],
                                                              type(self).__name__)
                    event_mention_argument = em.add_new_mention_argument(role, entity_mention, role_score)
                    event_mention_argument.model = type(self).__name__
        for event_frame, event_mention in event_frame_to_em.items():
            # This pass, we only handle event mention like argument
            for role, m_sent_no, role_start, role_end, role_score in event_frame.role_event_mention_span_pairs:
                another_serif_sentence = serif_doc.sentences[m_sent_no]
                candidate_event_mention = find_event_mention(another_serif_sentence,
                                                             another_serif_sentence.token_sequence[role_start],
                                                             another_serif_sentence.token_sequence[role_end])
                if candidate_event_mention is None:
                    logger.warning(
                        "{} {} {} {} under {} {} {} {} {} is not add-able due to missing event mention!".format(
                            role,
                            m_sent_no,
                            role_start,
                            role_end,
                            event_frame.doc_id,
                            event_frame.sent_no,
                            event_frame.trigger_start_idx,
                            event_frame.trigger_end_idx,
                            event_frame.trigger_score
                        ))
                else:
                    event_mention_argument = event_mention.add_new_event_mention_argument(role, candidate_event_mention,
                                                                                          role_score)
                    event_mention_argument.model = type(self).__name__
        return serif_doc
