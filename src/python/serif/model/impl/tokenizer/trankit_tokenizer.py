import logging

import torch
import trankit

from serif.model.tokenizer_model import TokenizerModel

logger = logging.getLogger(__name__)


class TrankitTokenizer(TokenizerModel):
    def __init__(self, cache_dir, lang, **kwargs):
        super(TrankitTokenizer, self).__init__(**kwargs)
        self.cache_dir = cache_dir
        self.lang = lang
        self.trankit_processor = None

    def load_model(self):
        import torch
        use_gpu = False
        if torch.cuda.is_available():
            use_gpu = True
        self.trankit_processor = trankit.Pipeline(self.lang, cache_dir=self.cache_dir, gpu=use_gpu)

    def unload_model(self):
        del self.trankit_processor
        self.trankit_processor = None
        if torch.cuda.is_available():
            torch.cuda.empty_cache()

    def add_tokens_to_sentence(self, sentence):
        if sentence.token_sequence is None:
            token_sequence = sentence.add_new_token_sequence()
            token_sequence.set_score(0.7)
        token_sequence = sentence.token_sequence
        sentence_text = sentence.text
        if len(sentence_text) < 1:
            return []
        trankit_sentence = self.trankit_processor.tokenize(sentence_text, is_sent=True)
        sent_start = sentence.start_char
        ret = []
        for token in trankit_sentence["tokens"]:
            ret.append(TokenizerModel.add_new_token(token_sequence, token["text"], sent_start + token["span"][0],
                                                    sent_start + token["span"][-1] - 1))
        return ret
