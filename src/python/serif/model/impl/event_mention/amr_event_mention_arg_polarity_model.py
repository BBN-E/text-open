import json, logging
from collections import defaultdict

from serif.model.document_model import DocumentModel
from serif.util.amr_utils import find_amr_nodes_with_attribute_value, find_event_mentions_aligned_to_amr_node, \
    find_event_mention_args_aligned_to_amr_node
from serif.theory.enumerated_type import Genericity, Polarity, Tense, Modality, DirectionOfChange


class AMREventMentionArgPolarityModel(DocumentModel):

    def __init__(self, **kwargs):
        pass

    def add_polarity_to_event_mentions_args(self, serif_doc):
        '''

        :param serif_doc:
        :return:
        '''

        amr_nodes_with_negative_polarity = find_amr_nodes_with_attribute_value(serif_doc, attribute=":polarity", value="-")

        event_mentions_with_negative_polarity = []
        event_mention_args_with_negative_polarity = []
        for amr_node in amr_nodes_with_negative_polarity:

            logging.info("\nAMR node with negative polarity: {}\n".format(amr_node))

            # add negative polarity to event mentions
            aligned_event_mentions = find_event_mentions_aligned_to_amr_node(amr_node)
            for em in aligned_event_mentions:

                logging.info("\tSetting negative polarity on event mention \"{}\" in \"{}\"".format(em.text, em.sentence.text))

                em.polarity = Polarity.Negative
                event_mentions_with_negative_polarity.append(em)

            # add negative polarity to event mention args
            aligned_event_mention_args = find_event_mention_args_aligned_to_amr_node(amr_node)
            for a in aligned_event_mention_args:

                logging.info("\tSetting negative polarity on event mention arg \"{}\" in \"{}\"".format(a.value.text, a.sentence.text))

                a.polarity = Polarity.Negative
                event_mention_args_with_negative_polarity.append(a)

        return event_mentions_with_negative_polarity, event_mention_args_with_negative_polarity

    def process_document(self, serif_doc):
        self.add_polarity_to_event_mentions_args(serif_doc)
