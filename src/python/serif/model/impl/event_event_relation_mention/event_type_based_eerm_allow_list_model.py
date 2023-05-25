from serif.model.event_event_relation_mention_model \
    import EventEventRelationMentionModel


class EventTypeBasedEERMAllowListModel(EventEventRelationMentionModel):
    def __init__(self, input_tabular_path,**kwargs):
        super(EventTypeBasedEERMAllowListModel, self).__init__(**kwargs)
        self.type_pair_to_eer_type = dict()
        with open(input_tabular_path) as fp:
            for i in fp:
                i = i.strip()
                src_type,eer_type,tgt_type = i.split(",")
                self.type_pair_to_eer_type.setdefault((src_type,tgt_type),set()).add(eer_type)

    def add_event_event_relation_mentions_to_document(self, serif_doc):
        ret = list()
        for sentence in serif_doc.sentences:
            for src_event_mention in sentence.event_mention_set or ():
                src_types = set()
                src_types.add(src_event_mention.event_type)
                src_types.update(t.event_type for t in src_event_mention.event_types)
                src_types.update(t.event_type for t in src_event_mention.factor_types)
                for tgt_event_mention in sentence.event_mention_set or ():
                    if src_event_mention is tgt_event_mention:
                        continue
                    tgt_types = set()
                    tgt_types.add(tgt_event_mention.event_type)
                    tgt_types.update(t.event_type for t in tgt_event_mention.event_types)
                    tgt_types.update(t.event_type for t in tgt_event_mention.factor_types)
                    for src_type in src_types:
                        for tgt_type in tgt_types:
                            if len(self.type_pair_to_eer_type.get((src_type,tgt_type),set())) > 0:
                                for eerm_type in self.type_pair_to_eer_type.get((src_type,tgt_type),set()):
                                    ret.extend(EventEventRelationMentionModel.add_new_event_event_relation_mention(serif_doc.event_event_relation_mention_set,eerm_type,0.75,type(self).__name__,src_event_mention,tgt_event_mention))
        return ret
