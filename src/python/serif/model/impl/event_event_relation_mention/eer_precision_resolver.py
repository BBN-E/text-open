from serif.theory.sentence import Sentence
from serif.theory.event_mention import EventMention
from serif.model.event_event_relation_mention_model import EventEventRelationMentionModel

def get_event_anchor(serif_em:EventMention):
    sentence = serif_em.owner_with_type(Sentence)
    serif_sentence_tokens = sentence.sentence_theory.token_sequence
    if serif_em.semantic_phrase_start is not None:
        serif_em_semantic_phrase_text = " ".join(i.text for i in serif_sentence_tokens[int(serif_em.semantic_phrase_start):int(serif_em.semantic_phrase_end)+1])
        return serif_em_semantic_phrase_text
    elif len(serif_em.anchors) > 0:
        return " ".join(i.anchor_node.text for i in serif_em.anchors)
    else:
        return serif_em.anchor_node.text

class EERPrecisionResolver(EventEventRelationMentionModel):
    def __init__(self,must_have_json_path,**kwargs):
        super(EERPrecisionResolver,self).__init__(**kwargs)

    def get_event_event_relation_mention_info(self,serif_doc):
        raise ValueError("You should not call this")

    def process_document(self, serif_doc):
        eid_to_em = dict()
        valid_eerm = list()
        for serif_eerm in serif_doc.event_event_relation_mention_set or []:

            # Avoid processing icews_eer
            if len(serif_eerm.icews_event_mention_relation_arguments) > 0:
                valid_eerm.append(serif_eerm)
                continue

            serif_em_arg1 = None
            serif_em_arg2 = None
            relation_type = serif_eerm.relation_type
            confidence = serif_eerm.confidence
            pattern = serif_eerm.pattern
            model = serif_eerm.model
            for arg in serif_eerm.event_mention_relation_arguments:
                if arg.role == "arg1":
                    serif_em_arg1 = arg.event_mention
                if arg.role == "arg2":
                    serif_em_arg2 = arg.event_mention
            if serif_em_arg1 is not None and serif_em_arg2 is not None:
                eid_to_em[serif_em_arg1.id] = serif_em_arg1
                eid_to_em[serif_em_arg2.id] = serif_em_arg2

                left_anchor_txt = get_event_anchor(serif_em_arg1)
                right_anchor_txt = get_event_anchor(serif_em_arg2)


                # You can do some filtering here, or even change confidence directly. For now, I trust everything from upstream and pass through directly
                valid_eerm.append(serif_eerm)


        # This line will kill all eerm in the old set
        eerm_set = serif_doc.add_new_event_event_relation_mention_set()

        for serif_eerm in valid_eerm:
            eerm = eerm_set.add_new_event_event_relation_mention(
                serif_eerm.relation_type, serif_eerm.confidence, serif_eerm.model)
            for arg in serif_eerm.event_mention_relation_arguments:
                eerm.add_new_event_mention_argument(arg.role, arg.event_mention)

