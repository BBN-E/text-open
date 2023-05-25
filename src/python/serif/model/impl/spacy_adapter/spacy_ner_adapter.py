import logging

from serif.model.mention_model import MentionModel
from serif.model.name_model import NameModel

logger = logging.getLogger(__name__)

class SpacyNameModel(NameModel):
    def add_names_to_sentence(self, serif_sentence):
        raise NotImplementedError()

class SpacyMentionModel(MentionModel):
    def add_mentions_to_sentence(self, sentence):
        raise NotImplementedError()


def spacy_ner_adder(serif_sentence):
    if serif_sentence.name_theory is None:
        serif_sentence.add_new_name_theory()
    if serif_sentence.mention_set is None:
        serif_sentence.add_new_mention_set()
    if "spacy_sentence" not in serif_sentence.aux:
        logger.warning("Cannot find spacy_sentence for {}, skipping!!".format(serif_sentence.text))

    name_model = SpacyNameModel()
    mention_model = SpacyMentionModel()
    spacy_sentence = serif_sentence.aux["spacy_sentence"]
    spacy_entities = spacy_sentence.ents

    for spacy_span in spacy_entities:

        start_index_in_serif_sentence = spacy_span.start - spacy_sentence.start
        end_index_in_serif_sentence = spacy_span.end - spacy_sentence.start - 1
        serif_start_token = serif_sentence.token_sequence[start_index_in_serif_sentence]
        serif_end_token = serif_sentence.token_sequence[end_index_in_serif_sentence]
        entity_type = spacy_span.label_

        name_model.add_or_update_name(serif_sentence.name_theory, entity_type, serif_start_token, serif_end_token)
        mention_model.add_or_update_mention(serif_sentence.mention_set, entity_type, "NAME", serif_start_token,
                                            serif_end_token, model="SpacyAdapter", loose_synnode_constraint=True)
