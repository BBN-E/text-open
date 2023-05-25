import logging
import os
import traceback

from trankit import Pipeline

from serif.model.document_model import DocumentModel
from serif.runtime_control import ProductionLevel

logger = logging.getLogger(__name__)


class TrankitAdapter(DocumentModel):
    def __init__(self, lang, dir, **kwargs):
        super(TrankitAdapter, self).__init__(**kwargs)

        self.lang = lang

        self.max_tokens = 100000
        if "max_tokens" in kwargs:
            self.max_tokens = int(kwargs["max_tokens"])

        self.trust_tokenization = False
        stage_one_processors = "tokenize,mwt,pos,lemma"

        if "trust_tokenization" in kwargs:
            self.trust_tokenization = True
            stage_one_processors = "tokenize,lemma,pos"

        if "no_ner" in kwargs:
            pass

        self.run_tokenization_only = False
        if "run_tokenization_only" in kwargs:
            self.run_tokenization_only = True

        self.gpu = len(os.environ.get("CUDA_VISIBLE_DEVICES", "")) > 0

        self.pipeline = Pipeline(lang=lang, gpu=self.gpu, cache_dir=dir)

    def add_name_theory(self, serif_sentence, trankit_sentences, trankit_word_to_serif_token):
        '''
        :type serif_sentence: serifxml3.Sentence
        :type trankit_sentences: list[dict]
        :type trankit_word_to_serif_token: dict{trankit_word_dict_frozenset: serif_token}
        :return:
        '''
        name_theory = serif_sentence.add_new_name_theory(serif_sentence.sentence_theory)

        def get_tagged_sentence_text(tokens):
            '''
            :type tokens: list
            '''
            result = ""
            for t in tokens:
                result += '{}\t{}\n'.format(t['text'], t['ner'])
            return result

        def get_name_type(trankit_token):
            '''
            :type trankit_token: dict
            '''
            return trankit_token["ner"][2:]

        def add_new_name(name_theory, trankit_name_tokens, trankit_word_to_serif_token):
            '''
            :type name_theory
            :type trankit_name_tokens: list
            :type trankit_word_to_serif_token: dict
            '''
            if len(trankit_name_tokens) == 0:
                logger.error("Attempt to add name with no tokens")
                raise AssertionError
            entity_type = get_name_type(trankit_name_tokens[0])

            if 'expanded' in trankit_name_tokens[0]:
                first_word = trankit_name_tokens[0]['expanded'][0]
            else:
                first_word = trankit_name_tokens[0]

            if 'expanded' in trankit_name_tokens[-1]:
                last_word = trankit_name_tokens[-1]['expanded'][-1]
            else:
                last_word = trankit_name_tokens[-1]

            logger.debug(f'Type: {entity_type}, Tokens: {" ".join([t["text"] for t in trankit_name_tokens])}')
            name_theory.add_new_name(
                entity_type,
                trankit_word_to_serif_token[frozenset(first_word.items())],
                trankit_word_to_serif_token[frozenset(last_word.items())])

        trankit_name_tokens = []
        # Note: The ordering of BIOES tags from Stanza is not always strictly
        # correct (e.g. 'I' tag often occurs with no 'B' tag). Log the
        # problems for debugging, but don't raise any errors
        # TODO same problems for Trankit or not?
        for trankit_sentence in trankit_sentences:
            tagged_sentence_text = get_tagged_sentence_text(trankit_sentence['tokens'])
            for trankit_token in trankit_sentence['tokens']:
                if trankit_token['ner'].startswith('B'):
                    if len(trankit_name_tokens) > 0:
                        logger.debug(f'Unexpected name begin token: {trankit_token}')
                        name_text = " ".join([t['text'] for t in trankit_name_tokens])
                        name_tags = " ".join([t['ner'] for t in trankit_name_tokens])
                        logger.debug(f'Pre-existing name: "{name_text}" with tags: "{name_tags}"')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        if get_name_type(trankit_token) != get_name_type(trankit_name_tokens[-1]):
                            add_new_name(name_theory, trankit_name_tokens, trankit_word_to_serif_token)
                            trankit_name_tokens.clear()
                        # raise AssertionError
                    trankit_name_tokens.append(trankit_token)
                elif trankit_token['ner'].startswith('I'):
                    if len(trankit_name_tokens) == 0:
                        logger.debug(f'Unexpected name inside token: {trankit_token}')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        # raise AssertionError
                    trankit_name_tokens.append(trankit_token)
                elif trankit_token['ner'].startswith('E') or trankit_token['ner'].startswith('S'):
                    if trankit_token['ner'].startswith('E') and len(trankit_name_tokens) == 0:
                        logger.debug(f'Unexpected name end token: {trankit_token}')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        # raise AssertionError
                    if trankit_token['ner'].startswith('S') and len(trankit_name_tokens) != 0:
                        logger.debug(f'Unexpected name single-word token: {trankit_token}')
                        name_text = " ".join([t['text'] for t in trankit_name_tokens])
                        name_tags = " ".join([t['ner'] for t in trankit_name_tokens])
                        logger.debug(f'Pre-existing name: "{name_text}" with tags: "{name_tags}"')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        if get_name_type(trankit_token) != get_name_type(trankit_name_tokens[-1]):
                            add_new_name(name_theory, trankit_name_tokens, trankit_word_to_serif_token)
                            trankit_name_tokens.clear()
                        # raise AssertionError
                    trankit_name_tokens.append(trankit_token)
                    add_new_name(name_theory, trankit_name_tokens, trankit_word_to_serif_token)
                    trankit_name_tokens.clear()
                elif trankit_token['ner'].startswith('O'):
                    if len(trankit_name_tokens) != 0:
                        logger.debug(f'Unexpected outside token: {trankit_token}')
                        name_text = " ".join([t['text'] for t in trankit_name_tokens])
                        name_tags = " ".join([t['ner'] for t in trankit_name_tokens])
                        logger.debug(f'Pre-existing name: "{name_text}" with tags: "{name_tags}"')
                        logger.debug(f'Full sentence:\n{tagged_sentence_text}')
                        add_new_name(name_theory, trankit_name_tokens, trankit_word_to_serif_token)
                        trankit_name_tokens.clear()
                        # raise AssertionError

    def get_offsets_for_token(self, trankit_token, sentence, start_search):
        '''
        :param trankit_token:  dict (token object)
        :param sentence:  str
        :param start_search:  int (where in sentence to start search)
        :return:
        '''
        start = None

        token_pos = 0
        sentence_pos = start_search

        # def stanford_normalize_char(current_char):
        #     """Stanford's normalizer is designed to work on token and strips leading space.
        #     Adding an 'X' to avoid that to work on single character.
        #
        #     See https://github.com/stanfordnlp/stanza/blob/master/stanza/models/tokenize/vocab.py#L29
        #     """
        #     return self.vocab_cls.normalize_token('X' + current_char)[1:]

        while True:
            if token_pos >= len(trankit_token['text']):
                break

            current_char = sentence[sentence_pos]

            # A tokenized stanford_token can contain space inside like ": )".
            # To cope with that we only skip leading spaces
            if start is None and current_char.isspace():
                sentence_pos += 1
            # stanford_token.text is normalized so that TAB becomes ' ':
            # we need to keep that in mind when comparing characters
            elif trankit_token['text'][token_pos].isspace() or sentence[sentence_pos].isspace():
                while token_pos < len(trankit_token['text']) and trankit_token['text'][token_pos].isspace():
                    token_pos += 1
                while sentence_pos < len(sentence) and sentence[sentence_pos].isspace():
                    sentence_pos += 1
            elif current_char != trankit_token['text'][token_pos]:
                if current_char == " " and trankit_token['text'][
                    token_pos] == "_":  # this is OK, we did a substitution in get_tokenized_sentence_text()
                    pass
                else:
                    print("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                           ord(current_char),
                                                                                           trankit_token['text'][
                                                                                               token_pos],
                                                                                           ord(trankit_token['text'][
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

            sentence_text = sentence.text
            token_sequence = sentence.add_new_token_sequence()
            token_sequence.set_score(0.7)
            if hasattr(sentence_text, "strip") is False or len(sentence_text.strip()) < 1:
                continue

            try:
                trankit_doc = self.pipeline(sentence_text, is_sent=True)
                last_end = -1

                for trankit_sentence in trankit_doc['sentences']:
                    for trankit_token in trankit_sentence['tokens']:
                        start_offset, end_offset = self.get_offsets_for_token(trankit_token, sentence_text,
                                                                              last_end + 1)
                        last_end = end_offset

                        serif_token = token_sequence.add_new_token(
                            sentence.start_char + start_offset,
                            sentence.start_char + end_offset,
                            trankit_token['text'],
                            trankit_token['lemma'])

                if self.run_tokenization_only is False and len(token_sequence) > 0 and len(
                        token_sequence) <= self.max_tokens:
                    # Create POS sequence and dependency information from single
                    # Serif sentence
                    assert len(trankit_doc['sentences']) == 1, \
                        """Expected single Trankit sentence due to
                           tokenize_pretokenized=True"""
                    trankit_sentence = trankit_doc['sentences'][0]
                    # assert len(trankit_sentence['tokens']) == len(token_sequence), \
                    #    """Expected Trankit tokens to match Serif tokens due to
                    #       tokenize_pretokenized=True"""
                    if len(trankit_sentence['tokens']) == len(token_sequence):
                        pos_sequence = sentence.add_new_part_of_speech_sequence()
                        pos_sequence.set_score(0.7)
                        trankit_word_to_serif_token = dict()
                        for i in range(len(trankit_sentence['tokens'])):
                            trankit_token = trankit_sentence['tokens'][i]
                            serif_token = token_sequence[i]

                            if 'expanded' in trankit_token:
                                word = trankit_token['expanded'][-1]
                            else:
                                word = trankit_token

                            trankit_word_to_serif_token[
                                frozenset(word.items())] = serif_token  # make word dict hashable
                            governor = word['head']
                            if governor != 0:
                                # Substract 1 from governor due to Trankit being 1-indexed
                                serif_token.head = token_sequence[governor - 1]
                            pos_sequence.add_new_pos(
                                serif_token, word['xpos'], word['upos'],
                                word['deprel'].split(":")[0])

                        self.add_name_theory(sentence, [trankit_doc['sentences'][0]], trankit_word_to_serif_token)
                else:
                    if self.run_tokenization_only is False and self.max_tokens >= 0:
                        logger.info(f"Skipping Trankit on long sentence: ({len(token_sequence)})")
                sentence.trankit_sentence = trankit_sentence
            except Exception as e:
                try:
                    logger.exception("doc: {} model: {}".format(serif_doc.docid, "TrankitAdapter"))
                    logger.error(traceback.format_exc())
                    logger.error("doc: {} TrankitAdapter cannot process sentence \"{}\"".format(serif_doc.docid,
                                                                                                sentence.text))
                except Exception as e1:
                    traceback.print_exc()
                finally:
                    if hasattr(self, "argparse") and self.argparse.PRODUCTION_MODE is False:
                        raise e
                    if hasattr(self,
                               "pyserif_pipeline_config_ins") and self.pyserif_pipeline_config_ins.production_level < ProductionLevel.EXTERNAL_USAGE:
                        raise e


def test_trankit():
    import os
    from serif.theory.document import Document
    f_list = ["/nfs/raid66/u11/users/brozonoy-ad/text-open/src/python/test/sample_doc.txt"]
    # stanza_ins = TrankitAdapter('english', '/nfs/raid66/u11/users/brozonoy-ad/text-open/trankit_models/')
    for f in f_list:
        serif_doc = Document.from_text(f, 'english', os.path.basename(f))
        print(serif_doc)
        # stanza_ins.process(serif_doc)
        print(serif_doc.docid)


if __name__ == '__main__':
    test_trankit()
