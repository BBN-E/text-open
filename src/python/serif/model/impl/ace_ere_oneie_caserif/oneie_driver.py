import json
import logging
import os

from serif import Document
from serif.model.document_model import DocumentModel
from serif.model.event_mention_model import EventMentionModel
from serif.model.impl.parser.all_token_flatten_parser import AllTokenFlattenParser
from serif.model.ingester import Ingester
from serif.model.mention_model import MentionModel
from serif.model.name_model import NameModel
from serif.model.relation_mention_model import RelationMentionModel
from serif.model.sentence_splitter_model import SentenceSplitterModel
from serif.model.tokenizer_model import TokenizerModel
from serif.theory.enumerated_type import Tense, Modality
from serif.theory.original_text import get_patterns_for_illegal_xml_unicode_char

logger = logging.getLogger(__name__)


def XMLStringCleaner(original_str):
    illegal_re = get_patterns_for_illegal_xml_unicode_char()
    clean_text = " ".join(illegal_re.split(original_str))
    if clean_text != original_str:
        logger.warning("Cannot save original string into XML, will replace it with space")
        logger.debug(original_str)
        logger.debug(clean_text)
    return clean_text


class DuckNameModel(NameModel):
    def add_names_to_sentence(self, serif_sentence):
        raise NotImplementedError()

    def maintain_deduplication_set_for_sentence(self, serif_sentence):
        self.name_hash.clear()
        for n in serif_sentence.name_theory:
            self.name_hash.setdefault((n.entity_type, n.start_token, n.end_token), list()).append(n)


class DuckMentionModel(MentionModel):
    def add_mentions_to_sentence(self, sentence):
        raise NotImplementedError()

    def maintain_deduplication_set_for_sentence(self, serif_sentence):
        self.existing_mentions_by_span.clear()
        self.existing_mentions_by_type_and_span.clear()
        for m in serif_sentence.mention_set:
            tokens = m.tokens
            span_key = tokens[0], tokens[-1]
            full_key = m.entity_type, m.mention_type == "NAME", tokens[0], tokens[-1]
            self.existing_mentions_by_span[span_key].append(m)
            self.existing_mentions_by_type_and_span[full_key].append(m)


class OneIEIngester(Ingester):
    def __init__(self, **kwargs):
        self.lang = kwargs["lang"]
        super(OneIEIngester, self).__init__(**kwargs)

    def ingest(self, jsonl_list_path):
        doc_id_to_sentences = dict()
        serif_docs = list()
        with open(jsonl_list_path) as fp:
            for i in fp:
                j = json.loads(i)
                doc_id = j["doc_id"]
                sent_id = j["sent_id"]
                sent_id_int = int(sent_id.split("-")[-1])  # 0 based
                j["sent_id_int"] = sent_id_int
                for idx in range(len(j["tokens"])):
                    j["tokens"][idx] = XMLStringCleaner(j["tokens"][idx]).replace(" ", "_")
                # use tokens for original sentence text to avoid discrepancies
                j["sentence"] = " ".join(j["tokens"])
                # if "sentence" not in j:
                #     j["sentence"] = " ".join(j["tokens"])
                # else:
                #     j["sentence"] = XMLStringCleaner(j["sentence"])
                doc_id_to_sentences.setdefault(doc_id, list()).append(j)
        for doc_id in doc_id_to_sentences.keys():
            sentences = sorted(doc_id_to_sentences[doc_id], key=lambda x: x["sent_id_int"])
            all_text = "".join(sentence["sentence"] for sentence in sentences)

            # Two Checks here. 1 Sent id is not duplicated. 2. There's no missing sentences
            seen_sent_ids = set()
            for sentence in sentences:
                if sentence["sent_id_int"] in seen_sent_ids:
                    logger.warning("{} Seen duplicated id {} sentences".format(doc_id, sentence["sent_id_int"]))
                seen_sent_ids.add(sentence["sent_id_int"])
            max_sent_id = max(seen_sent_ids)
            for idx in range(max_sent_id):
                if idx not in seen_sent_ids:
                    logger.warning("{} missing sentence {}".format(doc_id, idx))

            serif_doc = Document.from_string(language=self.lang, s=all_text, docid=doc_id)
            region = serif_doc.regions[0]
            serif_doc.add_new_sentences()
            current_pos_in_region = 0
            for idx, sentence in enumerate(sentences):
                assert region.text[current_pos_in_region:current_pos_in_region + len(sentence["sentence"])] == sentence[
                    "sentence"]
                serif_sent = SentenceSplitterModel.add_new_sentence(serif_doc.sentences, region, current_pos_in_region,
                                                                    current_pos_in_region + len(
                                                                        sentence["sentence"]) - 1)[0]
                if hasattr(serif_sent, "aux") is False:
                    serif_sent.aux = dict()
                serif_sent.aux["oneie_sentence"] = sentence
                current_pos_in_region += len(sentence["sentence"])
            serif_docs.append(serif_doc)
        return serif_docs


class OneIEDriver(DocumentModel):
    def __init__(self, **kwargs):
        super(OneIEDriver, self).__init__(**kwargs)
        # Init name_model for deduplication
        self.name_model = DuckNameModel()
        # Init mention_model for deduplication
        self.mention_model = DuckMentionModel()
        self.all_token_flatter_parser = AllTokenFlattenParser("unknown", "X")

    def process_document(self, serif_doc):
        for serif_sentence in serif_doc.sentences:
            oneie_sentence = serif_sentence.aux["oneie_sentence"]
            # Tokenizer
            token_sequence = serif_sentence.add_new_token_sequence()
            token_sequence.set_score(0.7)
            last_token_end = serif_sentence.start_char
            for oneie_token in oneie_sentence["tokens"]:
                while serif_doc.get_original_text_substring(last_token_end, last_token_end).isspace():
                    last_token_end += 1
                token_start = last_token_end
                for token_char, c in enumerate(oneie_token):
                    assert serif_doc.get_original_text_substring(last_token_end, last_token_end) == c, "{} {}".format(c,
                                                                                                                      serif_sentence.text)
                    last_token_end += 1
                token_end = last_token_end
                serif_token = TokenizerModel.add_new_token(
                    token_sequence,
                    oneie_token,
                    token_start,
                    token_end - 1)
            # Use dummy parsing model
            self.all_token_flatter_parser.add_parse_to_sentence(serif_sentence)
            # Add mention
            oneie_elem_id_to_serif_obj = dict()
            if serif_sentence.name_theory is None:
                serif_sentence.add_new_name_theory()
            if serif_sentence.mention_set is None:
                serif_sentence.add_new_mention_set()
            for oneie_mention in oneie_sentence["entity_mentions"]:
                entity_type = oneie_mention["entity_type"]
                mention_type = oneie_mention["mention_type"]
                if mention_type == "NOM":
                    mention_type = "DESC"
                elif mention_type == "NAM":
                    mention_type = "NAME"
                elif mention_type == "PRO":
                    mention_type = "PRON"
                else:
                    raise NotImplementedError("Cannot inteprate mention type {}".format(mention_type))
                serif_start_token = serif_sentence.token_sequence[oneie_mention["start"]]
                serif_end_token = serif_sentence.token_sequence[oneie_mention["end"] - 1]
                if mention_type == "NAME":
                    self.name_model.add_or_update_name(serif_sentence.name_theory, entity_type, serif_start_token,
                                                       serif_end_token)
                added_mentions = self.mention_model.add_or_update_mention(serif_sentence.mention_set, entity_type,
                                                                          mention_type, serif_start_token,
                                                                          serif_end_token,
                                                                          model="OneIEDriver",
                                                                          entity_subtype=oneie_mention[
                                                                              "entity_subtype"] if "entity_subtype" in oneie_mention else "UNDET")
                assert len(added_mentions) == 1
                oneie_elem_id_to_serif_obj[oneie_mention["id"]] = added_mentions[0]
            # Add entity relation
            rel_mention_set = serif_sentence.add_new_relation_mention_set()
            for oneie_entity_relation in oneie_sentence["relation_mentions"]:
                left_arg_d = None
                right_arg_d = None
                for argument in oneie_entity_relation["arguments"]:
                    if argument["role"] == "Arg-1":
                        left_arg_d = argument
                    elif argument["role"] == "Arg-2":
                        right_arg_d = argument
                    else:
                        raise ValueError("Cannot handle {}".format(argument["role"]))
                assert left_arg_d is not None and right_arg_d is not None
                left_mention = oneie_elem_id_to_serif_obj[left_arg_d["entity_id"]]
                right_mention = oneie_elem_id_to_serif_obj[right_arg_d["entity_id"]]
                added_relation_mentions = RelationMentionModel.add_new_relation_mention(rel_mention_set,
                                                                                        oneie_entity_relation[
                                                                                            "relation_subtype"],
                                                                                        left_mention, right_mention,
                                                                                        Tense.Unspecified,
                                                                                        Modality.Other,
                                                                                        model="OneIEDriver")
                assert len(added_relation_mentions) == 1
                oneie_elem_id_to_serif_obj[oneie_entity_relation["id"]] = added_relation_mentions[0]
            # Add event
            event_mention_set = serif_sentence.add_new_event_mention_set()
            for oneie_event_mention in oneie_sentence["event_mentions"]:
                trigger_start_token = serif_sentence.token_sequence[oneie_event_mention["trigger"]["start"]]
                trigger_end_token = serif_sentence.token_sequence[oneie_event_mention["trigger"]["end"] - 1]
                event_type = oneie_event_mention["event_type"]
                added_event_mentions = EventMentionModel.add_new_event_mention(event_mention_set, event_type,
                                                                               trigger_start_token, trigger_end_token,
                                                                               model="OneIEDriver")
                assert len(added_event_mentions) == 1
                for argument in oneie_event_mention["arguments"]:
                    arg_role = argument["role"]
                    entity_mention = oneie_elem_id_to_serif_obj[argument["entity_id"]]
                    EventMentionModel.add_new_event_mention_argument(added_event_mentions[0], arg_role, entity_mention,
                                                                     1.0, model="OneIEDriver")
                oneie_elem_id_to_serif_obj[oneie_event_mention["id"]] = added_event_mentions[0]
            # It may be possible to recover entity coreference info from dataset. But that part is not implemented


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--input_jsonl")
    parser.add_argument("--language")
    parser.add_argument("--output_dir")
    args = parser.parse_args()
    ingester = OneIEIngester(lang=args.language)
    driver = OneIEDriver()
    seen_doc_ids = set()

    serif_docs = ingester.ingest(args.input_jsonl)
    for serif_doc in serif_docs:
        if serif_doc.docid in seen_doc_ids:
            raise ValueError("Duplicated {}".format(serif_doc.docid))
        seen_doc_ids.add(serif_doc.docid)
        driver.process_document(serif_doc)
        serif_doc.save(os.path.join(args.output_dir, "{}.xml".format(serif_doc.docid)))
