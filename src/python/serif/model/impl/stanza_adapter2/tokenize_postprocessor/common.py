import logging

from serif.model.impl.stanza_adapter2.tokenize_postprocessor.chinese import fix_chinese
from serif.model.impl.stanza_adapter2.tokenize_postprocessor.korean import fix_korean
from serif.model.impl.stanza_adapter2.tokenize_postprocessor.russian import fix_russian

logger = logging.getLogger("stanza_adapter2.tokenize_postprocessor")


class DuckStanzaDocument(object):
    def __init__(self):
        self.sentences = []


class DuckStanzaSentence(object):
    def __init__(self):
        self.tokens = []
        self.text = ""


class DuckStanzaToken(object):
    def __init__(self):
        self.text = ""
        self.words = []


class DuckStanzaWord(object):
    def __init__(self):
        self.text = ""
        self.lemma = ""
        self.head = 0
        self.xpos = "X"
        self.upos = None
        self.deprel = None


class StanzaTokenizationPostprocessor(object):
    def __init__(self, lang):
        if lang == "zh-hans":
            self.processor = fix_chinese
        elif lang == "ko":
            self.processor = fix_korean
        elif lang == "ru":
            self.processor = fix_russian
        else:
            raise NotImplementedError("Unsupported language {}".format(lang))

    def process_stanza_doc(self, stanza_doc):
        """
        https://ami-gitlab-01.bbn.com/text-group/better/-/issues/694
        Stanza output is 4 layer structure, doc, sent, token, word
        Word is for specific language https://stanfordnlp.github.io/stanza/data_objects.html#word
        In the code, we'll create our own StanzaObject using DuckObjects, while doing postprocessing at token level
        And if possible, recover word level as well. By the time of writing, the word level logic is not carefully thought due to we initially only supports korean,russia,chinese and none of these have multiword expansion problem.
        :param stanza_doc:
        :return:
        """
        new_doc = DuckStanzaDocument()
        for stanford_sentence in stanza_doc.sentences:
            new_sentence = DuckStanzaSentence()
            new_sentence.text = stanford_sentence.text
            new_doc.sentences.append(new_sentence)
            for stanford_token in stanford_sentence.tokens:
                token_text = stanford_token.text
                # We do filtering in case Stanza return us token that contains [SPACE]
                new_token_texts = list(filter(lambda x: len(x) > 0, self.processor(token_text).split(" ")))
                current_new_token_array = list()
                for new_token_text in new_token_texts:
                    new_token = DuckStanzaToken()
                    new_token.text = new_token_text
                    new_sentence.tokens.append(new_token)
                    current_new_token_array.append(new_token)
                if len(stanford_token.words) > 1:
                    logger.warning(
                        "We cannot handle subword split logic because alignment in between token and word is unclear, creating dummy word")
                    for new_token in current_new_token_array:
                        new_word = DuckStanzaWord()
                        new_word.text = new_token.text
                        new_word.lemma = new_token.text
                        new_token.words.append(new_word)
                elif len(stanford_token.words) < 1:
                    # This is an upstream stanza problem, we'll keep everything as is.
                    pass
                else:
                    # This is for making sure that after postprocessor, we can recover lemma and word as much as possible
                    new_word_texts = list(
                        filter(lambda x: len(x) > 0, self.processor(stanford_token.words[0].text).split(" ")))
                    if stanford_token.words[0].lemma is not None:
                        new_word_lemmas = list(
                            filter(lambda x: len(x) > 0, self.processor(stanford_token.words[0].lemma).split(" ")))
                    else:
                        new_word_lemmas = list("" for _ in range(len(new_word_texts)))
                    if len(new_word_lemmas) < len(new_word_texts):
                        logger.warning(
                            "Len of new word is not equal to new lemma, {} {}".format(new_word_texts, new_word_lemmas))
                        while len(new_word_lemmas) < len(new_word_texts):
                            new_word_lemmas.append(new_word_texts[len(new_word_lemmas)])
                    elif len(new_word_lemmas) > len(new_word_texts):
                        logger.warning(
                            "Len of new word is not equal to new lemma, {} {}".format(new_word_texts, new_word_lemmas))
                        new_word_lemmas = new_word_lemmas[:len(new_word_texts)]
                    if len(new_word_texts) == len(new_token_texts):
                        for new_token, new_word_text, new_lemma_text in zip(current_new_token_array, new_word_texts,
                                                                            new_word_lemmas):
                            new_word = DuckStanzaWord()
                            new_word.text = new_word_text
                            new_word.lemma = new_lemma_text
                            new_token.words.append(new_word)
                    else:
                        logger.warning(
                            "We cannot handle subword split logic because alignment in between token and word is unclear, creating dummy word")
                        for new_token in current_new_token_array:
                            new_word = DuckStanzaWord()
                            new_word.text = new_token.text
                            new_word.lemma = new_token.text
                            new_token.words.append(new_word)
        return new_doc
