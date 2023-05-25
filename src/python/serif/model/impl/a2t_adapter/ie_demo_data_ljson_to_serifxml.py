import json
import logging
import os
import random

# import stanza

import serifxml3
from serif.model.event_mention_model import EventMentionModel
from serif.model.impl.a2t_adapter.json_doc import serifxml_to_string, serif_json_seralizer
from serif.model.impl.nlplingo_adapter.driver import NLPLingoDecoder
from serif.model.parser_model import ParserModel
from serif.model.sentence_splitter_model import SentenceSplitterModel

logger = logging.getLogger(__name__)

a2t_adapter_root = os.path.realpath(os.path.join(__file__, os.pardir))


def list_spliter_by_num_of_batches(my_list, num_of_batches):
    k, m = divmod(len(my_list), num_of_batches)
    return list(my_list[i * k + min(i, m):(i + 1) * k + min(i + 1, m)] for i in range(num_of_batches))


class DummyParserModel(ParserModel):
    def __init__(self, **kwargs):
        super(DummyParserModel, self).__init__(**kwargs)
        self.add_heads = True

    def add_parse_to_sentence(self, serif_sentence):
        pass


def StanzaNERGolderEntityMentionAdder(sentence_en, stanza_processor, serif_sentence):
    span_to_stanza_entity_types = stanza_processor.process_token_array(sentence_en["tokens"])
    token_to_entity_types = ["UNDET" for _ in range(len(sentence_en["tokens"]))]
    mention_set = serif_sentence.add_new_mention_set()
    for (start_token_idx, end_token_idx), entity_types in sorted(span_to_stanza_entity_types.items(),
                                                                 key=lambda x: abs(x[0][0] - x[0][1])):
        for token_mover in range(start_token_idx, end_token_idx + 1):
            for entity_type in entity_types:
                if token_to_entity_types[token_mover] == "UNDET":
                    token_to_entity_types[token_mover] = entity_type

    for token_start_idx, token_end_idx, entity_type, arg_role in sentence_en["entity_spans"]:
        entity_type_stanza_to_cnt = dict()
        for stanza_entity_type in token_to_entity_types[token_start_idx: token_end_idx + 1]:
            if stanza_entity_type != "UNDET":
                entity_type_stanza_to_cnt[stanza_entity_type] = entity_type_stanza_to_cnt.get(stanza_entity_type, 0) + 1
        resolved_entity_type = entity_type
        if len(entity_type_stanza_to_cnt) > 0:
            stanza_entity_type = max(entity_type_stanza_to_cnt.items(), key=lambda x: x[1])[0]
            logger.info("Changing entity type from {} to {}".format(entity_type, stanza_entity_type))
            resolved_entity_type = stanza_entity_type
        new_mention = mention_set.add_new_mention_from_tokens("NAME", resolved_entity_type,
                                                              serif_sentence.token_sequence[token_start_idx],
                                                              serif_sentence.token_sequence[token_end_idx])
        new_mention.model = "BPAnnotated"


def NLPLingoNERAdder(nlplingo_config_path, serif_docs):
    nlplingo_decoder = NLPLingoDecoder(nlplingo_config_path)
    nlplingo_decoder.load_model()
    nlplingo_decoder.process_documents(serif_docs)


def build_serif_article_from_sentencn_en(doc_id, sent_ens, stanza_processor):
    parse_model_ins = DummyParserModel()
    original_text = "".join(" ".join(j for j in i["tokens"]) for i in sent_ens)
    serif_doc = serifxml3.Document.from_string(language="English", s=original_text, docid=doc_id)
    region = serif_doc.regions[0]
    serif_doc.add_new_sentences()
    current_offset = 0
    for sentence in sent_ens:
        sent_end_str = " ".join(sentence["tokens"])
        original_end_str = serif_doc.get_original_text_substring(current_offset + 0,
                                                                 current_offset + 0 + len(sent_end_str) - 1)
        assert sent_end_str == original_end_str
        serif_sent = SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, current_offset + 0,
                                                            current_offset + 0 + len(sent_end_str) - 1)[0]
        current_offset += len(sent_end_str)
    for idx, sentence_en in enumerate(sent_ens):
        serif_sentence = serif_doc.sentences[idx]
        token_sequence = serif_sentence.add_new_token_sequence()
        token_sequence.set_score(0.7)
        current_offset = serif_sentence.start_char
        for original_token_index, token in enumerate(sentence_en["tokens"]):
            new_token = token_sequence.add_new_token(current_offset, current_offset + len(token) - 1, token)
            new_token.original_token_index = original_token_index
            current_offset += len(token) + 1  # 1 for space
        for serif_token in token_sequence:
            assert serif_token.text == serif_doc.get_original_text_substring(serif_token.start_char,
                                                                             serif_token.end_char)
        parse_model_ins.add_new_parse(serif_sentence, sentence_en["parse"])
        if stanza_processor is not None:
            StanzaNERGolderEntityMentionAdder(sentence_en, stanza_processor, serif_sentence)

        event_mention_set = serif_sentence.add_new_event_mention_set()
        token_span_to_event_mentions = dict()
        for token_start_idx, token_end_idx, event_type in sentence_en["event_spans"]:
            ems = EventMentionModel.add_new_event_mention(event_mention_set, event_type,
                                                          serif_sentence.token_sequence[token_start_idx],
                                                          serif_sentence.token_sequence[token_end_idx],
                                                          model="BPAnnotated")
            em = ems[0]
            token_span_to_event_mentions.setdefault((token_start_idx, token_end_idx), set()).add(em)
    return serif_doc


class StanzaProcessor(object):
    def __init__(self):
        self.pipeline = stanza.Pipeline(lang="en",
                                        model_dir="/nfs/raid88/u10/users/hqiu_ad/data/common/stanza/1.3.1",
                                        package='default',
                                        processors='tokenize,ner',
                                        use_gpu=True, tokenize_pretokenized=True)

    def process_token_array(self, token_arr):
        text = " ".join(i.replace("\r", " ").replace("\n", " ").replace(" ", "_") for i in token_arr)
        stanza_doc = self.pipeline(text)
        assert len(stanza_doc.sentences) == 1
        stanza_sentence = stanza_doc.sentences[0]
        stanza_entities = stanza_sentence.entities
        stanza_token_start_to_stanza_token_idx = dict()
        stanza_token_end_to_stanza_token_idx = dict()
        for token_idx, token in enumerate(stanza_sentence.tokens):
            start_char = token.start_char
            end_char = token.end_char
            stanza_token_start_to_stanza_token_idx.setdefault(start_char, list()).append(token_idx)
            stanza_token_end_to_stanza_token_idx.setdefault(end_char, list()).append(token_idx)
        span_to_entity_types = dict()
        for stanza_entity in stanza_entities:
            earliest_start_token = min(stanza_token_start_to_stanza_token_idx[stanza_entity.start_char])
            latest_end_token = max(stanza_token_end_to_stanza_token_idx[stanza_entity.end_char])
            entity_type = stanza_entity.type
            span_to_entity_types.setdefault((earliest_start_token, latest_end_token), set()).add(entity_type)
        return span_to_entity_types


def main():
    # stanza_processor = StanzaProcessor()
    stanza_processor = None
    nlplingo_ner_decode_par = os.path.join(a2t_adapter_root, "nlplingo_ner_decode.json")
    # input_jsonl_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_train_sentences_all.ljson"
    # input_jsonl_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_chinese_sentences_all.ljson"
    # output_json_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_train_sentences_all_ui_nlplingo.json"
    # output_json_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_chinese_sentences_all_ui_nlplingo.json"
    input_jsonl_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_farsi_sentences_all.ljson"
    output_json_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_farsi_sentences_all_ui_nlplingo.json"
    sent_ens = list()
    random.seed(0)
    with open(input_jsonl_path) as fp:
        for l in fp:
            l = l.strip()
            sentence_en = json.loads(l)
            sent_ens.append(sentence_en)
    # random.shuffle(sent_ens)

    sentence_groups = list_spliter_by_num_of_batches(sent_ens, 40)
    output_json_docs = list()
    serif_docs = []
    # for group_id, sentence_group in enumerate(sentence_groups):
    #     doc_id = "bp_p2_gr_en_train_{}".format(group_id)
    #     serif_doc = build_serif_article_from_sentencn_en(doc_id, sentence_group, stanza_processor)
    #     serif_docs.append(serif_doc)
    for sentence in sent_ens:
        doc_id = sentence['sent_id']
        serif_doc = build_serif_article_from_sentencn_en(doc_id, [sentence], stanza_processor)
        serif_docs.append(serif_doc)

    if nlplingo_ner_decode_par is not None:
        NLPLingoNERAdder(nlplingo_ner_decode_par, serif_docs)

    for serif_doc in serif_docs:
        # serif_doc.save(os.path.join(output_serifxml_path, "{}.xml".format(serif_doc.docid)))
        serif_str = serifxml_to_string(serif_doc)
        json_doc = serif_json_seralizer(serif_doc)
        output_json_docs.append({
            "step_1_marking_extraction": json_doc,
            "a2t_extraction": json_doc,
            "step_1_serifxml": serif_str,
            "select_for_scoring": False
        })
    for output_json_doc in output_json_docs[:5]:
        output_json_doc["select_for_scoring"] = True
    with open(output_json_path, 'w') as wfp:
        json.dump(output_json_docs, wfp, indent=4, sort_keys=True, ensure_ascii=False)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
