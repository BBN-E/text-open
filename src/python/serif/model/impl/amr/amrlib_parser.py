import logging
import traceback

import spacy
import amrlib
from amrlib.alignments.faa_aligner import FAA_Aligner

from serif.model.amr_parser_model import AMRParserModel

logger = logging.getLogger(__name__)


class AMRLibParser(AMRParserModel):
    def __init__(self, model_dir, **kwargs):
        super(AMRLibParser, self).__init__(**kwargs)
        self.model_dir = model_dir

    def load_model(self, **kwargs):

        # self.stog = amrlib.load_stog_model(model_dir=self.model_dir, **kwargs)

        amrlib.setup_spacy_extension()
        self.nlp = spacy.load('/nfs/raid87/u10/nlp/spacy_3_0_6/en_core_web_sm/')
        self.inference = FAA_Aligner()

    def unload_model(self):
        # del self.stog
        # self.stog = None
        del self.nlp
        del self.inference
        self.nlp = None
        self.inference = None

    def add_amr_parse_to_sentence(self, sentence):

        try:

            # amr_str = self.stog.parse_sents([sentence.text])[0]

            text_tokens = self.preprocess_penn_treebank_bracket_notation(\
                [token.text for token in sentence.token_sequence])
            sentence_starts = [True] + [False] * (len(sentence.token_sequence) - 1)

            doc = spacy.tokens.Doc(self.nlp.vocab, text_tokens, sent_starts=sentence_starts)
            amr_strings = doc._.to_amr()
            assert len(amr_strings) == 1
            amr_string = amr_strings[0]

            amr_surface_aligns, alignment_strings = self.inference.align_sents([" ".join(text_tokens)], [amr_string])

            return self.add_new_amr_parse(sentence, amr_surface_aligns[0])

        except Exception as e:
            logger.exception(traceback.format_exc())
            return []
