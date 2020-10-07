import logging
from serif.model.parser_model import ParserModel

import benepar

logger = logging.getLogger(__name__)

class BeneparParser(ParserModel):
    def __init__(self, model, **kwargs):
        super(BeneparParser, self).__init__(**kwargs)
        self.parser = benepar.Parser(model)
        self.max_tokens = 100000
        if "max_tokens" in kwargs:
            self.max_tokens = int(kwargs["max_tokens"])

    def fix_token_for_benepar(self, text):
        text = text.strip()
        text = text.replace(" ", "_")
        # Benepar doesn't like it when we have a bracket with other
        # text in the token. Remove the brackets, leave the text if 
        # possible.
        brackets = ["(", ")", "[", "]", "{", "}"]
        for bracket in brackets:
            if len(text) > 1 and text.find(bracket) != -1:
                text = text.replace(bracket, "")
            if len(text) == 0:
                text = bracket
        return text

    def get_tokens(self, sentence):
        token_texts = []
        for token in sentence.token_sequence._children:
            token_texts.append(self.fix_token_for_benepar(token.text))
        return token_texts

    def get_parse_info(self, sentence):
        if len(sentence.token_sequence) > self.max_tokens:
            logger.info(f"Skipping Benepar on long sentence: ({len(sentence.token_sequence)})")
            sentence_text = " ".join([t.text for t in sentence.token_sequence])
            logger.debug(f"{sentence_text}")
            return None
        token_texts = self.get_tokens(sentence)
        try:
            tree = self.parser.parse(token_texts)
            return tree.pformat()
        except Exception as e:
            logger.error("Error in benepar parser:")
            logger.error(e)
            return None

