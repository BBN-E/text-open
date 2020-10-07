from serif.model.base_model import BaseModel
import stanza
import copy


class StanzaAdapter(BaseModel):
    def __init__(self, lang, dir, **kwargs):
        super(StanzaAdapter, self).__init__(**kwargs)
        self.pipeline_raw = \
            stanza.Pipeline( # initialize Stanza pipeline for language
                lang=lang,
                dir=dir,
                package='default',
                processors=dict(),
                logging_level='INFO',
                verbose=None,
                use_gpu=True)

        self.vocab_cls = stanza.models.tokenize.vocab.Vocab(
            lang=lang)  # we only need to access Vocab.normalize_token()

    def get_offsets_for_token(self, stanza_token, sentence, start_search):
        '''
        :type stanza_token: stanza.Token
        :type sentence: raw text (str)
        :param start_search: int
        :return: start and end char offsets for tokens in sentence
        '''
        start = None

        token_pos = 0
        sentence_pos = start_search

        def stanza_normalize_char(ch):
            """Stanza's normalizer is designed to work on token and strips leading space.
            Adding an 'X' to avoid that to work on single character.

            See https://github.com/stanfordnlp/stanza/blob/master/stanza/models/tokenize/vocab.py#L29
            """
            return self.vocab_cls.normalize_token('X' + ch)[1:]

        while True:
            if token_pos >= len(stanza_token.text):
                break

            current_char = sentence[sentence_pos]

            # A tokenized stanza_token can contain space inside like ": )".
            # To cope with that we only skip leading spaces
            if start is None and current_char.isspace():
                sentence_pos += 1
            # stanza_token.text is normalized so that TAB becomes ' ':
            # we need to keep that in mind when comparing characters
            elif stanza_normalize_char(current_char) != stanza_token.text[token_pos]:
                print("Character mismatch in tokenizer! %s (ord=%d) != %s (ord=%d)" % (current_char,
                                                                                       ord(current_char),
                                                                                       stanza_token.text[token_pos],
                                                                                       ord(stanza_token.text[
                                                                                               token_pos])))
                print("Sentence:", sentence)
                raise AssertionError()
            else: # we've hit the token
                if start is None:
                    start = sentence_pos
                sentence_pos += 1
                token_pos += 1

        return start, sentence_pos - 1

    def build_stan_off_to_stan_word_obj(self, doc, raw_text):
        '''
        :type doc: stanza.Document
        :type raw_text: str
        :return:
            :type stanza_sentences: list
            :type span_off_to_stanza_words:
            :type stanza_token_to_parent:
        '''
        span_off_to_stanza_words = dict()
        stanza_token_to_parent = dict()
        stanza_sentences = []

        last_end = -1
        for stanza_sentence in doc.sentences:

            # word means word inside mwt, e.g. the Spanish mwt "damelo" (1,3) has three words "da" [1], "me" [2], "lo" [3]
            word_idx_to_stanza_word = dict()
            for stanza_token in stanza_sentence.tokens:
                token_mwt_tup = [int(idx) for idx in stanza_token.id.split("-")] # tuple (1,3) if mwt or (1,) if single-word token
                token_word_ids = list(range(token_mwt_tup[0], token_mwt_tup[-1]+1))
                for i, stanza_word in enumerate(stanza_token.words):
                    stanza_word_index = token_word_ids[i]
                    if int(stanza_word_index) in word_idx_to_stanza_word:
                        raise ValueError()
                    word_idx_to_stanza_word[int(stanza_word_index)] = stanza_word

            for stanza_token in stanza_sentence.tokens:
                start_offset, end_offset = self.get_offsets_for_token(stanza_token, raw_text, last_end + 1)
                last_end = end_offset
                for stanza_word in stanza_token.words:
                    span_off_to_stanza_words.setdefault((start_offset, end_offset), list()).append(stanza_word)
                    if int(stanza_word.head) != 0 and int(stanza_word.head) in word_idx_to_stanza_word and \
                            word_idx_to_stanza_word[int(stanza_word.head)] != stanza_word:
                        stanza_token_to_parent[stanza_word] = word_idx_to_stanza_word[int(stanza_word.head)]
                    else:
                        stanza_token_to_parent[stanza_word] = None

            stanza_sentences.append(stanza_sentence)

        return stanza_sentences, span_off_to_stanza_words, stanza_token_to_parent


    def process(self, serif_doc):
        '''
        :param serif_doc:
        :return:
        '''
        for sentence in serif_doc.sentences:
            print(sentence.text)
            stanza_doc = self.pipeline_raw(sentence.text)

            stanza_sentences, span_off_to_stanza_words, stanza_token_to_parent = self.build_stan_off_to_stan_word_obj(
                stanza_doc, sentence.text)

            sentence_theory = sentence.sentence_theory
            sentence.stanford_sentences = stanza_sentences

            token_sequence = sentence.add_new_token_sequence(0.7, sentence_theory)
            part_of_speech_sequence = sentence.add_new_part_of_speech_sequence(0.7, sentence_theory)

            serif_token_to_stanza_word = dict()
            for token_offset, stanza_words in sorted(span_off_to_stanza_words.items(), key=lambda x: x[0][0]):
                token_start, token_end = token_offset
                for stanza_word in stanza_words:
                    serif_token = token_sequence.add_new_token(sentence.start_char + token_start,
                                                               sentence.start_char + token_end, stanza_word.text,
                                                               stanza_word.lemma)
                    serif_token_to_stanza_word[serif_token] = stanza_word
                    pos = part_of_speech_sequence.add_new_pos(serif_token, stanza_word.xpos, stanza_word.upos,
                                                              stanza_word.deprel.split(":")[0])

            stanza_word_to_serif_token = {v: k for k, v in serif_token_to_stanza_word.items()}

            for serif_token, stan_word in serif_token_to_stanza_word.items():
                parent_stan_word = stanza_token_to_parent.get(stan_word, None)
                if parent_stan_word is not None:
                    parent_serif_token = stanza_word_to_serif_token[parent_stan_word]
                    serif_token.head = parent_serif_token



if __name__ == "__main__":
    # test_serif()
    # read_serif()
    # test_stanford()
    #test_stanford2()
    #import stanza
    #stanza.download(lang="ar", dir="/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/")
    #nlp = stanza.Pipeline("en", "/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/")
    #doc = nlp("John has a dog. John's dog likes to bark.")
    #import serifxml3
    hi = "18"
    token_mwt_tup = [int(idx) for idx in hi.split("-")]  # tuple (1,3) if mwt or (1,) if single-word token
    token_word_ids = list(range(token_mwt_tup[0], token_mwt_tup[-1]+1))
    print(token_word_ids)

    #SA = StanzaAdapter(lang="ru", dir="/nfs/raid66/u11/users/brozonoy/text-open/stanza_models/")
    #t = doc.sentences[0].tokens[4]
    #print(t)
    #s,e = SA.get_offsets_for_token(stanza_token=doc.sentences[0].tokens[3],
    #                         sentence="John has a dog.",
    #                         start_search=11)
    #print(s,e)
    #serifdoc = serifxml3.Document("/nfs/raid66/u11/users/brozonoy/text-open/src/python/test/rundir/output/russian_sample.xml")
    #SA.process(serifdoc)
    #serifdoc.save("/nfs/raid66/u11/users/brozonoy/text-open/src/python/test/rundir/output/russian_sample_processed.xml")