import logging
import re
import traceback

import stanza
import torch

from serif.model.document_model import DocumentModel
from serif.model.impl.stanza_adapter2.tokenize_postprocessor.common import StanzaTokenizationPostprocessor
from serif.runtime_control import ProductionLevel

logger = logging.getLogger(__name__)

TAG_PATTERN = re.compile('<[^<>]+>', re.MULTILINE)


def remove_xml_marking(text):
    buffer = ""
    current_end = 0
    for i in TAG_PATTERN.finditer(text):
        start_char, end_char = i.span()
        buffer += text[current_end:start_char] + (" " * (end_char - start_char))
        current_end = end_char
    buffer += text[current_end:]
    assert len(buffer) == len(text)
    return buffer


class StanzaAdapter(DocumentModel):

    def text_fa_removing_multiple_ending_dot(self, text):
        last_dot_offset = text.rfind(".")
        if text.count(".") > 1 and last_dot_offset == len(text.rstrip()) - len("."):
            dot_string_length = 1
            for i in range(last_dot_offset - 1, 0, -1):
                if text[i] == ".":
                    dot_string_length += 1
                else:
                    break
            first_dot_offset = last_dot_offset - dot_string_length + 1
            # This is for sentences tailing with multiple dot
            escaped_text = text[:first_dot_offset + 1] + " " * (dot_string_length - 1) + text[
                                                                                         last_dot_offset + 1:]  # We still want to keep 1 dot as well as tailing control characters
            if escaped_text != text:
                logger.warning("TextPreprocessor has changed \"{}\" to \"{}\"".format(text, escaped_text))
            return escaped_text
        else:
            return text

    def text_fa_removing_bad_ending_punct(self, text):
        if text.rstrip().endswith("؟."):  # https://ami-gitlab-01.bbn.com/text-group/text-open/-/issues/112
            escaped_text = text.rstrip()[:-1] + " " * (len(text) - len(text.rstrip()) + 1)
            logger.warning("TextPreprocessor has changed \"{}\" to \"{}\"".format(text, escaped_text))
            return escaped_text
        return text

    def text_preprocessor(self, text):
        """
        For some stanza internal reasons, specific text will fail stanza and this function is for
        fixing them. Ideally we shouldn't use this function unless it's absolutely needed.
        :param text: original sentence text
        :return:
        """
        escaped_text = text
        if self.lang == "fa" and self.trust_tokenization is False:
            escaped_text = self.text_fa_removing_multiple_ending_dot(escaped_text)
            escaped_text = self.text_fa_removing_bad_ending_punct(escaped_text)
        if self.drop_xml_marking_before_processing is True:
            escaped_text = remove_xml_marking(escaped_text)
        return escaped_text

    def __init__(self, lang, dir, **kwargs):
        super(StanzaAdapter, self).__init__(**kwargs)
        self.lang = lang
        self.dir = dir
        self.lang_2_stage_one_processor_special_case = {
            "ru": "tokenize,pos,lemma",  # no default mwt for russian
            "zh": "tokenize,pos,lemma",
            # no default mwt for chinese (this currently is switched to zh-hans, but doesn't hurt)
            "zh-hans": "tokenize,pos,lemma",  # no default mwt for chinese (simplified)
            "zh-hant": "tokenize,pos,lemma",  # no default mwt for chinese (traditional)
            "lzh": "tokenize,pos,lemma",  # no default mwt for chinese (literary)
            "fa": "tokenize,pos,lemma",
            "ko": "tokenize,pos,lemma",
        }
        self.lang_2_processor_special_case = {
            "uk": "tokenize,pos,lemma,depparse",  # no default ner for ukrainian
            "fa": "tokenize,pos,lemma,depparse",
            "ko": "tokenize,pos,lemma,depparse",
        }

        self.max_tokens = 100000
        if "max_tokens" in kwargs:
            self.max_tokens = int(kwargs["max_tokens"])

        self.trust_tokenization = False
        self.stage_one_processors = "tokenize,mwt,pos,lemma"

        if "trust_tokenization" in kwargs:
            self.trust_tokenization = True
            self.stage_one_processors = "tokenize,lemma,pos"
        if lang in self.lang_2_stage_one_processor_special_case:
            self.stage_one_processors = self.lang_2_stage_one_processor_special_case[lang]

        if lang in self.lang_2_processor_special_case:
            self.pipeline_parse_processors = self.lang_2_processor_special_case[lang]
        else:
            self.pipeline_parse_processors = "tokenize,pos,lemma,depparse,ner"

        if "no_ner" in kwargs and self.pipeline_parse_processors.split(",")[-1] == "ner":
            self.pipeline_parse_processors = ",".join(self.pipeline_parse_processors.split(",")[:-1])

        self.run_tokenization_only = False
        if "run_tokenization_only" in kwargs:
            self.run_tokenization_only = True
        self.tokenize_postprocessor = None

        self.drop_xml_marking_before_processing = False
        if "drop_xml_marking_before_processing" in kwargs:
            self.drop_xml_marking_before_processing = True

    def load_model(self):
        self.pipeline_tokenize = \
            stanza.Pipeline(  # initialize Stanza pipeline for language
                lang=self.lang,
                dir=self.dir,
                package='default',
                processors=self.stage_one_processors,
                logging_level='INFO',
                verbose=None,
                tokenize_pretokenized=self.trust_tokenization,
                use_gpu=True)
        self.pipeline_parse = \
            stanza.Pipeline(  # initialize Stanza pipeline for language
                lang=self.lang,
                dir=self.dir,
                package='default',
                processors=self.pipeline_parse_processors,
                logging_level='INFO',
                verbose=None,
                tokenize_pretokenized=True,
                use_gpu=True)
        if hasattr(stanza.models, "tokenize"):
            self.vocab_cls = stanza.models.tokenize.vocab.Vocab(lang=self.lang)
        else:
            self.vocab_cls = stanza.models.tokenization.vocab.Vocab(
                lang=self.lang)  # we only need to access Vocab.normalize_token()
        if stanza.__version__ == "1.3.0" and self.lang in {"zh-hans", "ko", "ru"} and self.trust_tokenization is False:
            logger.info("Will apply stanza tokenizer postprocessing for stanza {} lang {}".format(stanza.__version__,
                                                                                                  self.lang))
            self.tokenize_postprocessor = StanzaTokenizationPostprocessor(self.lang)

    def unload_model(self):
        del self.pipeline_tokenize
        del self.pipeline_parse
        del self.vocab_cls
        del self.tokenize_postprocessor
        self.pipeline_tokenize = None
        self.pipeline_parse = None
        self.vocab_cls = None
        self.tokenize_postprocessor = None
        torch.cuda.empty_cache()

    def add_name_theory(self, serif_sentence, stanza_sentences, stanza_word_to_serif_token):
        '''
        :type serif_sentence: serifxml3.Sentence
        :type stanza_sentences: list of stanza.Sentence
        :type stanza_word_to_serif_token: dict
        :return:
        '''
        name_theory = serif_sentence.add_new_name_theory(serif_sentence.sentence_theory)

        def get_tagged_sentence_text(tokens):
            '''
            :type tokens: list
            '''
            result = ""
            for t in tokens:
                result += '{}\t{}\n'.format(t.text, t.ner)
            return result

        def get_name_type(stanza_token):
            '''
            :type stanza_token: stanza.Token
            '''
            return stanza_token.ner[2:]

        def add_new_name(name_theory, stanza_name_tokens, stanza_word_to_serif_token):
            '''
            :type name_theory
            :type stanza_name_tokens: list
            :type stanza_word_to_serif_token: dict
            '''
            if len(stanza_name_tokens) == 0:
                logger.error("Attempt to add name with no tokens")
                raise AssertionError
            entity_type = get_name_type(stanza_name_tokens[0])
            first_word = stanza_name_tokens[0].words[0]
            last_word = stanza_name_tokens[-1].words[-1]
            logger.debug(f'Type: {entity_type}, Tokens: {" ".join([t.text for t in stanza_name_tokens])}')
            name_theory.add_new_name(
                entity_type,
                stanza_word_to_serif_token[first_word],
                stanza_word_to_serif_token[last_word])

        stanza_name_tokens = []
        # Note: The ordering of BIOES tags from Stanza is not always strictly
        # correct (e.g. 'I' tag often occurs with no 'B' tag). Log the
        # problems for debugging, but don't raise any errors
        for stanza_sentence in stanza_sentences:
            tagged_sentence_text = get_tagged_sentence_text(stanza_sentence.tokens)
            for stanza_token in stanza_sentence.tokens:
                if stanza_token.ner.startswith('B'):
                    if len(stanza_name_tokens) > 0:
                        logger.debug(f'Unexpected name begin token: {stanza_token}')
                        name_text = " ".join([t.text for t in stanza_name_tokens])
                        name_tags = " ".join([t.ner for t in stanza_name_tokens])
                        logger.debug(f'Pre-existing name: "{name_text}" with tags: "{name_tags}"')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        if get_name_type(stanza_token) != get_name_type(stanza_name_tokens[-1]):
                            add_new_name(name_theory, stanza_name_tokens, stanza_word_to_serif_token)
                            stanza_name_tokens.clear()
                        # raise AssertionError
                    stanza_name_tokens.append(stanza_token)
                elif stanza_token.ner.startswith('I'):
                    if len(stanza_name_tokens) == 0:
                        logger.debug(f'Unexpected name inside token: {stanza_token}')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        # raise AssertionError
                    stanza_name_tokens.append(stanza_token)
                elif stanza_token.ner.startswith('E') or stanza_token.ner.startswith('S'):
                    if stanza_token.ner.startswith('E') and len(stanza_name_tokens) == 0:
                        logger.debug(f'Unexpected name end token: {stanza_token}')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        # raise AssertionError
                    if stanza_token.ner.startswith('S') and len(stanza_name_tokens) != 0:
                        logger.debug(f'Unexpected name single-word token: {stanza_token}')
                        name_text = " ".join([t.text for t in stanza_name_tokens])
                        name_tags = " ".join([t.ner for t in stanza_name_tokens])
                        logger.debug(f'Pre-existing name: "{name_text}" with tags: "{name_tags}"')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        if get_name_type(stanza_token) != get_name_type(stanza_name_tokens[-1]):
                            add_new_name(name_theory, stanza_name_tokens, stanza_word_to_serif_token)
                            stanza_name_tokens.clear()
                        # raise AssertionError
                    stanza_name_tokens.append(stanza_token)
                    add_new_name(name_theory, stanza_name_tokens, stanza_word_to_serif_token)
                    stanza_name_tokens.clear()
                elif stanza_token.ner.startswith('O'):
                    if len(stanza_name_tokens) != 0:
                        logger.debug(f'Unexpected outside token: {stanza_token}')
                        name_text = " ".join([t.text for t in stanza_name_tokens])
                        name_tags = " ".join([t.ner for t in stanza_name_tokens])
                        logger.debug(f'Pre-existing name: "{name_text}" with tags: "{name_tags}"')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        add_new_name(name_theory, stanza_name_tokens, stanza_word_to_serif_token)
                        stanza_name_tokens.clear()
                        # raise AssertionError

    def get_offsets_for_token(self, stanford_token, sentence, start_search):
        start = None

        token_pos = 0
        sentence_pos = start_search

        def stanford_normalize_char(current_char):
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
            elif stanford_token.text[token_pos].isspace() or sentence[sentence_pos].isspace():
                while token_pos < len(stanford_token.text) and stanford_token.text[token_pos].isspace():
                    token_pos += 1
                while sentence_pos < len(sentence) and sentence[sentence_pos].isspace():
                    sentence_pos += 1
            elif stanford_normalize_char(current_char) != stanford_token.text[token_pos]:
                if current_char == " " and stanford_token.text[
                    token_pos] == "_":  # this is OK, we did a substitution in get_tokenized_sentence_text()
                    pass
                else:
                    print("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                           ord(current_char),
                                                                                           stanford_token.text[
                                                                                               token_pos],
                                                                                           ord(stanford_token.text[
                                                                                                   token_pos])))
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

    def process_document(self, serif_doc):

        for sentence in serif_doc.sentences:
            # Create token sequence from words, could have multiple sentences
            # and multiple words per token

            sentence_text = serif_doc.get_original_text_substring(sentence.start_char, sentence.end_char)
            token_sequence = sentence.add_new_token_sequence()
            token_sequence.set_score(0.7)
            if hasattr(sentence_text, "strip") is False or len(sentence_text.strip()) < 1:
                continue

            try:
                escaped_sentence_text = self.text_preprocessor(sentence_text)
                try:
                    stanfordnlp_doc = self.pipeline_tokenize(escaped_sentence_text)
                except Exception as e:
                    if self.lang == "fa" and len(escaped_sentence_text) > 1:
                        logger.warning("Try removing last char under fa for sentence \"{}\" to \"{}\"".format(
                            escaped_sentence_text, escaped_sentence_text[:-1] + " "))
                        stanfordnlp_doc = self.pipeline_tokenize(escaped_sentence_text[:-1] + " ")
                    else:
                        raise e
                if self.tokenize_postprocessor is not None:
                    stanfordnlp_doc = self.tokenize_postprocessor.process_stanza_doc(stanfordnlp_doc)
                last_end = -1
                for stanford_sentence in stanfordnlp_doc.sentences:
                    for stanford_token in stanford_sentence.tokens:
                        start_offset, end_offset = self.get_offsets_for_token(
                            stanford_token, escaped_sentence_text, last_end + 1)
                        last_end = end_offset

                        for stanford_word in stanford_token.words:
                            serif_token = token_sequence.add_new_token(
                                sentence.start_char + start_offset,
                                sentence.start_char + end_offset,
                                stanford_word.text,
                                stanford_word.lemma)

                if self.run_tokenization_only is False and len(token_sequence) > 0 and len(
                        token_sequence) <= self.max_tokens:
                    # Create POS sequence and dependency information from single
                    # Serif sentence
                    sentence_text = self.get_tokenized_sentence_text(token_sequence)
                    stanfordnlp_doc = self.pipeline_parse(sentence_text)
                    assert len(stanfordnlp_doc.sentences) == 1, \
                        """Expected single StanfordNLP sentence due to
                           tokenize_pretokenized=True"""
                    stanford_sentence = stanfordnlp_doc.sentences[0]
                    assert len(stanford_sentence.tokens) == len(token_sequence), \
                        """Expected StanfordNLP tokens to match Serif tokens due to
                           tokenize_pretokenized=True"""

                    pos_sequence = sentence.add_new_part_of_speech_sequence()
                    pos_sequence.set_score(0.7)
                    stanza_word_to_serif_token = dict()
                    for i in range(len(stanford_sentence.tokens)):
                        stanford_token = stanford_sentence.tokens[i]
                        serif_token = token_sequence[i]
                        word = stanford_token.words[-1]
                        stanza_word_to_serif_token[word] = serif_token
                        governor = word.head
                        if governor != 0:
                            # Substract 1 from governor due to Stanford being 1-indexed
                            serif_token.head = token_sequence[governor - 1]
                        pos_sequence.add_new_pos(
                            serif_token, word.xpos, word.upos,
                            word.deprel.split(":")[0])
                    if "ner" in self.pipeline_parse_processors.split(","):
                        self.add_name_theory(sentence, [stanfordnlp_doc.sentences[0]], stanza_word_to_serif_token)
                else:
                    if self.run_tokenization_only is False and self.max_tokens >= 0:
                        logger.info(f"Skipping StanfordNLP on long sentence: ({len(token_sequence)})")
                sentence.stanford_sentence = stanford_sentence
            except Exception as e:
                try:
                    logger.exception("doc: {} model: {}".format(serif_doc.docid, "StanzaAdapter"))
                    logger.error(traceback.format_exc())
                    logger.error(
                        "doc: {} StanzaAdapter cannot process sentence \"{}\"".format(serif_doc.docid, sentence.text))
                except Exception as e1:
                    traceback.print_exc()
                finally:
                    if hasattr(self, "argparse") and self.argparse.PRODUCTION_MODE is False:
                        raise e
                    if hasattr(self,
                               "pyserif_pipeline_config_ins") and self.pyserif_pipeline_config_ins.production_level < ProductionLevel.EXTERNAL_USAGE:
                        raise e


def test_stanza():
    import os
    from serif.theory.document import Document
    from serif.model.impl.sentence_splitter.stanza_sentence_splitter import StanzaSentenceSplitter
    f_list = [
        "/nfs/mercury-12/u119/better/data/corpora/BETTER-English-IR-data.v1/text_files.preprocessed/5b/5b3dbf43-b016-4393-af68-085b103262d1.txt",
        "/nfs/mercury-12/u119/better/data/corpora/BETTER-English-IR-data.v1/text_files.preprocessed/ed/ed9942d9-af87-4cf7-988a-39f433fa47bc.txt"]
    stanza_ins = StanzaAdapter('en', '/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/')
    sent_brek = StanzaSentenceSplitter('en', '/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/')
    for f in f_list:
        serif_doc = Document.from_text(f, 'english', os.path.basename(f))
        sent_brek.process_document(serif_doc)
        stanza_ins.process_document(serif_doc)
        print(serif_doc.docid)


if __name__ == "__main__":
    # test_serif()
    # read_serif()
    # test_stanford()
    # test_stanford2()
    # import stanza
    # stanza.download(lang="ar", dir="/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/")
    # nlp = stanza.Pipeline("en", "/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/")
    # doc = nlp("John has a dog. John's dog likes to bark.")
    # import serifxml3
    # hi = "18"
    # token_mwt_tup = [int(idx) for idx in hi.split("-")]  # tuple (1,3) if mwt or (1,) if single-word token
    # token_word_ids = list(range(token_mwt_tup[0], token_mwt_tup[-1]+1))
    # print(token_word_ids)
    test_stanza()
    # SA = StanzaAdapter(lang="ru", dir="/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/")
    # t = doc.sentences[0].tokens[4]
    # print(t)
    # s,e = SA.get_offsets_for_token(stanza_token=doc.sentences[0].tokens[3],
    #                         sentence="John has a dog.",
    #                         start_search=11)
    # print(s,e)
    # serifdoc = serifxml3.Document("/nfs/raid66/u11/users/brozonoy/text-open/src/python/test/rundir/output/russian_sample.xml")
    # SA.process(serifdoc)
    # serifdoc.save("/nfs/raid66/u11/users/brozonoy/text-open/src/python/test/rundir/output/russian_sample_processed.xml")
