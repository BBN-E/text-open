import logging

import tensorflow as tf
from coref import util as util  # Lives in COREF_DIR
from coref.bert import tokenization
from coref.minimize import DocumentState  # Lives in COREF_DIR

from serif.model.mention_coref_model import MentionCoreferenceModel

logger = logging.getLogger(__name__)

DUMMY_ENTITY_TYPE = "UWCOREF_MENTION"


class UWCoreferenceModel(MentionCoreferenceModel):
    def __init__(self, lang, model_name, config_file_path, vocab_file, **kwargs):
        super(UWCoreferenceModel, self).__init__(**kwargs)
        self.lang = lang
        self.vocab_file = vocab_file
        self.config = util.initialize_from_env_and_params(model_name, config_file_path)

    def load_model(self):
        self.model = util.get_model(self.config)
        self.saver = tf.train.Saver()
        self.session = tf.Session()
        self.model.restore(self.session)

    def unload_model(self):
        del self.model
        self.model = None
        del self.saver
        self.saver = None
        self.session.close()
        del self.session
        self.session = None
        tf.keras.backend.clear_session()

    def get_sentence_map(self, segments, sentence_end):
        current = 0
        sent_map = []
        sent_end_idx = 0

        assert len(sentence_end) == sum([len(s) - 2 for s in segments])
        for segment in segments:
            sent_map.append(current)
            for i in range(len(segment) - 2):
                sent_map.append(current)
                current += int(sentence_end[sent_end_idx])
                sent_end_idx += 1
            sent_map.append(current)
        return sent_map

    def construct_fake_row_info(self, word_idx, word, pos):
        # row = ['bc/cctv/00/cctv_0000', '0', '0', 'In', 'IN', '(TOP(S(PP*', '-', '-', '-', 'Speaker#1', '*', '*', '*', '*', '(ARGM-TMP*', '*', '-']
        row = [''] + ['0', str(word_idx)] + [word, pos] + ['', '-', '-', '-', 'Speaker#1', '*', '*', '*', '*', '', '*',
                                                           '-']
        return row

    # first try to satisfy constraints1, and if not possible, constraints2.
    def split_into_segments(self, document_state, max_segment_len, constraints1, constraints2):
        current = 0
        previous_token = 0
        while current < len(document_state.subtokens):
            end = min(current + max_segment_len - 1 - 2, len(document_state.subtokens) - 1)
            while end >= current and not constraints1[end]:
                end -= 1
            if end < current:
                end = min(current + max_segment_len - 1 - 2, len(document_state.subtokens) - 1)
                while end >= current and not constraints2[end]:
                    end -= 1
                if end < current:
                    raise Exception("Can't find valid segment")
            document_state.segments.append(['[CLS]'] + document_state.subtokens[current:end + 1] + ['[SEP]'])
            subtoken_map = document_state.subtoken_map[current: end + 1]
            document_state.segment_subtoken_map.append([previous_token] + subtoken_map + [subtoken_map[-1]])
            info = document_state.info[current: end + 1]
            document_state.segment_info.append([None] + info + [None])
            current = end + 1
            previous_token = subtoken_map[-1]

    def convert_to_coref_doc(self, serif_doc, language, seg_len, tokenizer):
        document_state = DocumentState(serif_doc.docid)
        token_map = dict()

        for sentence in serif_doc.sentences:
            word_idx = -1
            for token in sentence.token_sequence:
                word_idx += 1
                word = token.text

                # get pos from upos; we need to correct pronouns
                pos = token.upos
                if pos == "PRON":
                    pos = "PRP"

                # print (pos + "\t" + word)
                subtokens = tokenizer.tokenize(word)
                document_state.tokens.append(word)
                document_state.token_end += ([False] * (len(subtokens) - 1)) + [True]
                for sidx, subtoken in enumerate(subtokens):
                    document_state.subtokens.append(subtoken)

                    row = self.construct_fake_row_info(word_idx, word, pos)
                    info = None if sidx != 0 else (row + [len(subtokens)])
                    document_state.info.append(info)
                    document_state.sentence_end.append(False)
                    document_state.subtoken_map.append(word_idx)

            document_state.sentence_end[-1] = True

        constraints1 = document_state.sentence_end if language != 'arabic' else document_state.token_end
        self.split_into_segments(document_state, seg_len, constraints1, document_state.token_end)
        document = document_state.finalize()

        return document

    def decode(self, serif_doc, model, vocab_file, language):
        # language = "arabic"
        seg_len = 128
        do_lower_case = True

        tokenizer = tokenization.FullTokenizer(
            vocab_file=vocab_file, do_lower_case=do_lower_case)

        coref_doc = self.convert_to_coref_doc(serif_doc, language, seg_len, tokenizer)

        tensorized_example = self.model.tensorize_example(coref_doc, is_training=False)
        feed_dict = {i: t for i, t in zip(self.model.input_tensors, tensorized_example)}
        _, _, _, top_span_starts, top_span_ends, top_antecedents, top_antecedent_scores = self.session.run(
            self.model.predictions, feed_dict=feed_dict)
        predicted_antecedents = self.model.get_predicted_antecedents(top_antecedents, top_antecedent_scores)
        coref_doc["predicted_clusters"], _ = self.model.get_predicted_clusters(top_span_starts, top_span_ends,
                                                                               predicted_antecedents)
        coref_doc["top_spans"] = list(zip((int(i) for i in top_span_starts), (int(i) for i in top_span_ends)))
        coref_doc['head_scores'] = []

        return coref_doc

    def add_entities_to_document(self, serif_doc):
        coref_doc = None
        try:
            coref_doc = self.decode(serif_doc, self.model, self.vocab_file, self.lang)
        except Exception as e:
            logger.exception(str(e))
            return []

        # Translate to Serif start and end tokens
        clusters = []
        for predicted_cluster in coref_doc["predicted_clusters"]:
            cluster = []
            for start, end in predicted_cluster:
                start_sent = coref_doc["sentence_map"][start]
                end_sent = coref_doc["sentence_map"][end]
                start_token_number = coref_doc["subtoken_map"][start]
                end_token_number = coref_doc["subtoken_map"][end]

                if start_sent != end_sent:
                    logger.warning("Throwing out coref due to sentence boundary condition")
                    continue
                if start_sent >= len(serif_doc.sentences) or start_token_number >= len(serif_doc.sentences[start_sent].token_sequence) or end_token_number >= len(serif_doc.sentences[start_sent].token_sequence):
                    logger.warning(
                        "If a sentence is not ended properly with punctuation, and the last token is valid, it will cause inappropriate sentence breaking in uw coreference")
                    continue
                sentence = serif_doc.sentences[start_sent]
                token_sequence = sentence.token_sequence
                cluster.append((start_sent, token_sequence[start_token_number], token_sequence[end_token_number]))
            if len(cluster) > 1:
                clusters.append(cluster)

        return MentionCoreferenceModel.resolve_clustering_result(serif_doc, clusters, DUMMY_ENTITY_TYPE, "NONE",
                                                                 type(self).__name__)
