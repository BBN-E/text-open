import logging

from serif.model.parser_model import ParserModel
from serif.theory.syn_node import SynNode
from serif.theory.parse import Parse

logger = logging.getLogger(__name__)


class DuckParseModel(ParserModel):
    def __init__(self, **kwargs):
        super(ParserModel, self).__init__(**kwargs)
        self.add_heads = True

    def add_new_parse(self, sentence, treebank_string, score=0.9):
        ret = list()
        if treebank_string is not None:
            parse = sentence.add_new_parse(score, sentence.token_sequence, treebank_string)
            if self.add_heads:
                parse.add_heads()
            ret.append(parse)
        return ret

    def add_parse_to_sentence(self, serif_sentence):
        raise NotImplementedError


def spacy_parsing_adder(serif_sentence):
    original_parse_model = DuckParseModel()
    if "spacy_sentence" not in serif_sentence.aux:
        logger.warning("Cannot find spacy_sentence for {}, skipping!!".format(serif_sentence.text))
        original_parse_model.add_new_parse(serif_sentence, "(S ())")
        return

    spacy_sentence = serif_sentence.aux["spacy_sentence"]
    try:
        assert spacy_sentence._.parse_string is not None
    except Exception as e:
        logger.warning("Cannot find constintuency parse string for {}, skipping!!".format(serif_sentence.text))
        return

    original_parse_model.add_new_parse(serif_sentence, spacy_sentence._.parse_string)
