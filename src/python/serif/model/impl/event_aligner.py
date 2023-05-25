from serif.model.document_model import DocumentModel
from serif.theory.event_mention import EventMention


# Types of the new_events override the types of old_events
def merge_events(new_em_list, old_em_dict):
    new_em_dict = {}
    for new_em in new_em_list:
        trigger_loc = (new_em.semantic_phrase_start, new_em.semantic_phrase_end)
        if trigger_loc in old_em_dict:
            old_em = old_em_dict[trigger_loc]
            old_em.event_type = new_em.event_type
            old_em.arguments = new_em.arguments
            old_em.score = new_em.score
            if new_em.model:
                if old_em.model:
                    old_em.model = old_em.model + "," + new_em.model
                else:
                    old_em.model = new_em.model
            if new_em.polarity:
                old_em.polarity = new_em.polarity
        else:
            new_em_dict[trigger_loc] = new_em
    return {**old_em_dict, **new_em_dict}


class EventAlignerModel(DocumentModel):
    
    def __init__(self, **kwargs):
        super(EventAlignerModel,self).__init__(**kwargs)

    def process_document(self, serif_doc):

        for sentence in serif_doc.sentences:
            unclassified_mtdp_ems_dict = {}
            nlplingo_event_mentions = []
            nlplingo_classified_event_mentions = []
            amr_event_mentions = []

            for event_mention in sentence.event_mention_set:
                if event_mention.event_type == "MTDP_EVENT":
                    unclassified_mtdp_ems_dict[(event_mention.semantic_phrase_start, event_mention.semantic_phrase_end)] = event_mention
                elif event_mention.model == "NLPLingo_classification":
                    nlplingo_classified_event_mentions.append(event_mention)
                elif event_mention.model == "AMR":
                    amr_event_mentions.append(event_mention)
                elif event_mention.model == "NLPLingo":
                    nlplingo_event_mentions.append(event_mention)

            # classify MTDP events with NLPLingo classification types
            classified_mtdp_ems_dict = merge_events(nlplingo_classified_event_mentions, unclassified_mtdp_ems_dict)

            # add AMR events over classified MTDP events
            amr_ems_dict = merge_events(amr_event_mentions, classified_mtdp_ems_dict)

            # add NLPLingo events over AMR + classified MTDP events
            nlplingo_ems_dict = merge_events(nlplingo_event_mentions, amr_ems_dict)

            sentence.event_mention_set._children = list(nlplingo_ems_dict.values())

        return serif_doc
