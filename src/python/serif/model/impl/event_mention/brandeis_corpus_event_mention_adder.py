from serif.model.event_mention_model import EventMentionModel


class BrandeisCorpusEventMentionAdder(EventMentionModel):

    def __init__(self, **kwargs):
        super(BrandeisCorpusEventMentionAdder, self).__init__(**kwargs)

    def process_document(self, serif_doc):
        sent_id_to_spans = dict()
        for sent_idx,start_idx,end_idx in serif_doc.brandeis_article.event_spans:
            sent_id_to_spans.setdefault(sent_idx,set()).add((start_idx,end_idx))
        ret = list()
        for sentence in serif_doc.sentences:
            if sentence.event_mention_set is None:
                sentence.add_new_event_mention_set()
            event_mention_set = sentence.event_mention_set
            for start_idx,end_idx in sent_id_to_spans.get(sentence.sent_no,()):
                ret.append(EventMentionModel.add_new_event_mention(event_mention_set,"Event",sentence.token_sequence[start_idx],sentence.token_sequence[end_idx],model="Brandeis"))
        return ret

    def add_event_mentions_to_sentence(self, serif_sentence):
        raise NotImplementedError()