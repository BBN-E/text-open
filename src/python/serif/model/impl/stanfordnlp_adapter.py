from serif.model.base_model import BaseModel
import stanfordnlp
import copy, re
import logging

logger = logging.getLogger(__name__)

class StanfordNLPAdapter(BaseModel):
    control_characters_re = re.compile(r'(\u202C)')

    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPAdapter, self).__init__(**kwargs)
        self.max_tokens = 100000
        self.trust_tokenization = False

        if "max_tokens" in kwargs:
            self.max_tokens = int(kwargs["max_tokens"])

        stage_one_processors = "tokenize,mwt,lemma"
        if "trust_tokenization" in kwargs:
            stage_one_processors = "tokenize,lemma"
            self.trust_tokenization = True
        self.pipeline_tokenize = \
            stanfordnlp.Pipeline(
                lang=lang,
                models_dir=models_dir, 
                processors=stage_one_processors,
                tokenize_pretokenized=self.trust_tokenization,
                use_gpu=True)
        self.pipeline_parse = \
            stanfordnlp.Pipeline(
                lang=lang,
                models_dir=models_dir, 
                processors="tokenize,pos,depparse", 
                tokenize_pretokenized=True,
                use_gpu=True)

        self.vocab_cls = stanfordnlp.models.tokenize.vocab.Vocab(
            lang=lang)  # we only need to access Vocab.normalize_token()

    def get_offsets_for_token(self, stanford_token, sentence, start_search):
        start = None

        token_pos = 0
        sentence_pos = start_search

        def stanford_normalize_char(ch):
            """Stanford's normalizer is designed to work on token and strips leading space.
            Adding an 'X' to avoid that to work on single character.

            See https://github.com/stanfordnlp/stanza/blob/master/stanza/models/tokenize/vocab.py#L29
            """
            return self.vocab_cls.normalize_token('X' + current_char)[1:]

        while True:
            if token_pos >= len(stanford_token.text):
                break

            current_char = sentence[sentence_pos]

            # A tokenized stanford_token can contain space inside like ": )".
            # To cope with that we only skip leading spaces
            if start is None and current_char.isspace():
                sentence_pos += 1
            # stanford_token.text is normalized so that TAB becomes ' ':
            # we need to keep that in mind when comparing characters
            elif stanford_normalize_char(current_char) != stanford_token.text[token_pos]:
                if current_char == " " and stanford_token.text[token_pos] == "_": # this is OK, we did a substitution in get_tokenized_sentence_text()
                    pass
                else:
                    print("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                           ord(current_char),
                                                                                           stanford_token.text[token_pos],
                                                                                           ord(stanford_token.text[token_pos])))
                print("Sentence:", sentence)
                raise AssertionError()
            else:
                if start is None:
                    start = sentence_pos
                sentence_pos += 1
                token_pos += 1

        return start, sentence_pos - 1

    def get_tokenized_sentence_text(self, token_sequence):
        t = ""
        for token in token_sequence:
            # Spaces can occur in the middle of Stanford tokens
            # But we don't want the token to break in two when we 
            # send it through StanfordNLP a second time with 
            # tokenize_pretokenized=True
            txt = token.text.replace(" ", "_") 
            if len(t) != 0:
                t += " "
            t += txt
        return t

    def process(self, serif_doc):
        for sentence in serif_doc.sentences:
            # Create token sequence from words, could have multiple sentences
            # and multiple words per token

            sentence_text = sentence.text
            if not self.trust_tokenization:
                sentence_text = StanfordNLPAdapter.control_characters_re.sub(" ", sentence_text)
                  
            stanfordnlp_doc = self.pipeline_tokenize(sentence_text)
            last_end = -1
            token_sequence = sentence.add_new_token_sequence(0.7)
            for stanford_sentence in stanfordnlp_doc.sentences:
                for stanford_token in stanford_sentence.tokens:
                    start_offset, end_offset = self.get_offsets_for_token(
                        stanford_token, sentence_text, last_end + 1)
                    last_end = end_offset
                    
                    for stanford_word in stanford_token.words:
                        serif_token = token_sequence.add_new_token(
                             sentence.start_char + start_offset,
                             sentence.start_char + end_offset,
                             stanford_word.text,
                             stanford_word.lemma)

            if len(token_sequence) <= self.max_tokens:
                # Create POS sequence and dependency information from single
                # Serif sentence
                sentence_text = self.get_tokenized_sentence_text(token_sequence)
                stanfordnlp_doc = self.pipeline_parse(sentence_text)
                assert len(stanfordnlp_doc.sentences) == 1,\
                    """Expected single StanfordNLP sentence due to
                       tokenize_pretokenized=True"""
                stanford_sentence = stanfordnlp_doc.sentences[0]
                assert len(stanford_sentence.tokens) == len(token_sequence),\
                    """Expected StanfordNLP tokens to match Serif tokens due to
                       tokenize_pretokenized=True"""

                pos_sequence = sentence.add_new_part_of_speech_sequence(0.7)
                for i in range(len(stanford_sentence.tokens)):
                    stanford_token = stanford_sentence.tokens[i]
                    serif_token = token_sequence[i]
                    word = stanford_token.words[-1]
                    governor = word.governor
                    if governor != 0:
                        # Substract 1 from governor due to Stanford being 1-indexed
                        serif_token.head = token_sequence[governor-1] 
                    pos_sequence.add_new_pos(
                        serif_token, word.xpos, word.upos, 
                        word.dependency_relation.split(":")[0])
            else:
                logger.info(f"Skipping StanfordNLP on long sentence: ({len(token_sequence)})")
            sentence.stanford_sentence = stanford_sentence

def test_serif():
    from serif.theory.document import Document
    from serif.model.impl.sentence_splitter.stanford_nlp_sentence_splitter import StanfordNLPSentenceSplitter

    sent_splitter = StanfordNLPSentenceSplitter("en","/nfs/raid87/u10/shared/Hume/common/stanfordnlp_resources")
    stanford_adapter = StanfordNLPAdapter("en","/nfs/raid87/u10/shared/Hume/common/stanfordnlp_resources")
    serif_doc = Document.from_sgm("/nfs/raid88/u10/users/hqiu/sgm_corpus/cx/ldc_denmark/sgms/Unstructured/ENG_NW_20190620_LDC_DENMARK_0011.sgm","english")
    sent_splitter.process(serif_doc)
    stanford_adapter.process(serif_doc)
    serif_doc.save("/home/hqiu/tmp/test.xml")


def read_serif():
    from serif.theory.document import Document
    serif_doc = Document("/home/hqiu/tmp/test.xml")
    for sent in serif_doc.sentences:
        for sentence_theory in sent.sentence_theories:
            for token in sentence_theory.token_sequence:
                print("Token: {}, lemma: {}, pos: {}, upos: {}, dep: {}, head: {}".format(token.text,token.lemma,token.xpos,token.upos,token.dep_rel,"" if token.head is None else token.head.text))


def test_stanford():
    pretokenized_text = "Barack Obama was born in Hawaii.  He was elected president in 2008."
    pipeline_raw = \
        stanfordnlp.Pipeline(
            lang="en",
            models_dir="/nfs/raid87/u10/shared/Hume/common/stanfordnlp_resources")
    doc = pipeline_raw(pretokenized_text)
    for sentence in doc.sentences:
        for tok in sentence.tokens:
            for word in tok.words:
                print(word)

def test_stanford2():
    p1 = "/nfs/mercury-12/u119/users/jfaschin/runjobs/expts/46733_bpjson_arabic_small_dep_pyserif1/txt/arabic/converted_txt/doc-419_5_9.txt"
    p2 = "/nfs/mercury-12/u119/users/jfaschin/runjobs/expts/46733_bpjson_arabic_small_dep_pyserif1/txt/arabic/converted_txt/doc-116_30_3.txt"
    with open(p2) as fp:
        t = fp.read()
    pipeline = \
        stanfordnlp.Pipeline(
            lang="ar",
            models_dir="/nfs/raid87/u10/shared/Hume/common/stanfordnlp_resources")
    doc = pipeline(t)
    for sent in doc.sentences:
        for token in sent.tokens:
            for word in token.words:
                print(word)


if __name__ == "__main__":
    # test_serif()
    # read_serif()
    # test_stanford()
    test_stanford2()
