import collections
import json
import logging
import os

import numpy as np
import torch
from torch.utils.data import TensorDataset, DataLoader
from tqdm import tqdm
from transformers import AutoTokenizer, AutoModelForSequenceClassification

current_script_path = __file__
project_root = os.path.realpath(
    os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir, os.path.pardir, os.path.pardir))

from serif.model.document_model import DocumentModel
from serif.model.event_mention_model import EventMentionModel
from serif.model.impl.a2t_adapter.a2t_example_filters import class_name_to_class as a2t_filter_class_name_to_class
from serif.model.relation_mention_model import RelationMentionModel
from serif.theory.enumerated_type import Tense, Modality
from serif.theory.event_mention import EventMention
from serif.theory.mention import Mention
from serif.model.impl.a2t_adapter.utils import modify_or_add_event_mention, modify_or_add_mention

logger = logging.getLogger(__name__)


def get_valid_slide_window_intervals(min_val, max_val, focus_val, slide_window_size):
    ret = set()
    for min_p in range(max(min_val, focus_val - slide_window_size + 1), focus_val + 1):
        max_p = min_p + slide_window_size - 1
        if max_p <= max_val:
            ret.add((min_p, max_p))
    if len(ret) < 1 and min_val <= focus_val <= max_val:
        ret.add((min_val, max_val))
    return ret


class GranularSpecialEventArgGoldenGenerator(object):
    def generate(self, serif_doc):
        ret = list()
        for granular_event in serif_doc.event_set or ():
            sent_to_ems = dict()
            sent_to_ms = dict()
            m_to_event_arg_role = dict()
            for event_arg in granular_event.arguments:
                if event_arg.entity is not None:
                    for mention in event_arg.entity.mentions:
                        sent_to_ms.setdefault(mention.sentence.sent_no, set()).add(mention)
                        m_to_event_arg_role.setdefault(mention, set()).add(event_arg.role)
                if event_arg.event_mention is not None:
                    sent_to_ems.setdefault(event_arg.event_mention.sentence.sent_no, set()).add(event_arg.event_mention)
            for sent_id, ems in sent_to_ems.items():
                for em in ems:
                    for m in sent_to_ms.get(sent_id, ()):
                        for arg_role in m_to_event_arg_role.get(m, ()):
                            ret.append((em, m,
                                        serif_doc.get_original_text_substring(em.sentence.start_char,
                                                                              em.sentence.end_char),
                                        arg_role))
        return ret


def graunlar_eventarg_golden_example_matcher(generated_examples, golden_examples):
    event_mention_to_linkable_mentions = dict()
    event_mention_mention_to_examples = dict()
    for generated_example in generated_examples:
        event_mention_to_linkable_mentions.setdefault(generated_example[0], set()).add(generated_example[1])
        event_mention_mention_to_examples.setdefault((generated_example[0], generated_example[1]), set()).add(
            generated_example)

    ret = list()
    for golden_example in golden_examples:
        event_mention = golden_example[0]
        mention = golden_example[1]
        potential_mentions = event_mention_to_linkable_mentions.get(event_mention, ())
        other_example_list = list()
        for potential_mention in potential_mentions:
            if potential_mention is not mention:
                other_example_list.extend(event_mention_mention_to_examples.get((event_mention, potential_mention), ()))
        ret.append([golden_example, other_example_list])
    return ret


def unary_slot_id_generator(slot):
    if isinstance(slot, Mention):
        return "{}#{}#{}".format(slot.sentence.sent_no, slot.tokens[0].index(), slot.tokens[-1].index())
    elif isinstance(slot, EventMention):
        return "{}#{}#{}".format(slot.sentence.sent_no, slot.start_token.index(), slot.end_token.index())
    raise NotImplementedError(type(slot).__name__)


def binary_slot_id_generator(left_slot, right_slot):
    return "{}#{}".format(unary_slot_id_generator(left_slot), unary_slot_id_generator(right_slot))


def a2t_example_id_generator(example):
    if len(example) == 2:
        return unary_slot_id_generator(example[0])
    elif len(example) == 3:
        return binary_slot_id_generator(example[0], example[1])
    else:
        raise ValueError(len(example))


class GenericUnaryExampleGenerator(object):
    def __init__(self, allowed_elem_types, slide_window_size=2):
        self.allowed_elem_types = set(allowed_elem_types)
        self.slide_window_size = slide_window_size

    def generate(self, serif_doc):
        ret = set()
        for sentence in serif_doc.sentences:
            focus_theory_set = set()
            if "Mention" in self.allowed_elem_types:
                focus_theory_set.update(sentence.mention_set or ())
            if "EventMention" in self.allowed_elem_types:
                focus_theory_set.update(sentence.event_mention_set or ())
            for mention in focus_theory_set:
                for start_slide_window, end_slide_window in get_valid_slide_window_intervals(0, len(
                        serif_doc.sentences) - 1, sentence.sent_no, self.slide_window_size):
                    original_passage = serif_doc.get_original_text_substring(
                        serif_doc.sentences[start_slide_window].start_char,
                        serif_doc.sentences[end_slide_window].end_char)
                    ret.add((mention, original_passage))
        return ret


class GenericBinaryExampleGenerator(object):
    def __init__(self, left_elem_allow_types, right_elem_allow_types, slide_window_size=1):
        self.left_elem_allow_types = set(left_elem_allow_types)
        self.right_elem_allow_types = set(right_elem_allow_types)
        self.slide_window_size = slide_window_size

    def generate(self, serif_doc):
        ret = set()
        all_possible_lefts = set()
        all_possible_rights = set()
        for serif_sentence in serif_doc.sentences:
            left_focus_set = set()
            if "Mention" in self.left_elem_allow_types:
                left_focus_set.update(serif_sentence.mention_set or ())
            if "EventMention" in self.left_elem_allow_types:
                left_focus_set.update(serif_sentence.event_mention_set or ())
            all_possible_lefts.update(left_focus_set)
            right_focus_set = set()
            if "Mention" in self.right_elem_allow_types:
                right_focus_set.update(serif_sentence.mention_set or ())
            if "EventMention" in self.right_elem_allow_types:
                right_focus_set.update(serif_sentence.event_mention_set or ())
            all_possible_rights.update(right_focus_set)
        for left_elem in all_possible_lefts:
            for right_elem in all_possible_rights:
                if left_elem == right_elem:
                    continue
                left_sent = left_elem.sentence
                right_sent = right_elem.sentence
                left_windows = get_valid_slide_window_intervals(0, len(serif_doc.sentences) - 1, left_sent.sent_no,
                                                                self.slide_window_size)
                right_windows = get_valid_slide_window_intervals(0, len(serif_doc.sentences) - 1, right_sent.sent_no,
                                                                 self.slide_window_size)
                windows_intersect = set(left_windows)
                windows_intersect = windows_intersect.intersection(set(right_windows))
                for start_slide_window, end_slide_window in windows_intersect:
                    ret.add((left_elem, right_elem, serif_doc.get_original_text_substring(
                        serif_doc.sentences[start_slide_window].start_char,
                        serif_doc.sentences[end_slide_window].end_char)))
        return ret


class PreExistedEventArgumentPopulator(object):
    def __init__(self):
        pass

    def generate(self, serif_doc):
        ret = set()
        for sentence in serif_doc.sentences:
            for event_mention in sentence.event_mention_set or ():
                for event_mention_arg in event_mention.arguments:
                    arg = event_mention_arg.mention
                    if arg is not None:
                        ret.add((event_mention, arg, serif_doc.get_original_text_substring(
                            sentence.start_char,
                            sentence.end_char)))
        return ret


def ner_write_back(example_label_confidence_tuple):
    filtered_example_label_confidence_tuple = list()
    should_blockout_endpoints = set()
    for mention, label, confidence, debug_info in example_label_confidence_tuple:
        for sentence_mention in mention.sentence.mention_set:
            if sentence_mention.model == "Ask2Transformers":
                if sentence_mention.confidence > 0.999:
                    should_blockout_endpoints.add(mention)
    for mention, label, confidence, debug_info in example_label_confidence_tuple:
        if mention not in should_blockout_endpoints:
            filtered_example_label_confidence_tuple.append((mention, label, confidence, debug_info))
    for mention, label, confidence, debug_info in filtered_example_label_confidence_tuple:
        serif_sentence = mention.sentence
        modify_or_add_mention(serif_sentence, mention.start_token, mention.end_token, label, confidence, debug_info)


def entity_mention_entity_mention_relation_write_back(example_label_confidence_tuple):
    for left_mention, right_mention, label, connfidence, debug_info in example_label_confidence_tuple:
        serif_doc = left_mention.document
        if serif_doc.rel_mention_set is None:
            serif_doc.add_new_rel_mention_set()
        rel_mention_set = serif_doc.rel_mention_set
        RelationMentionModel.add_new_relation_mention(rel_mention_set, label, left_mention, right_mention,
                                                      Tense.Unspecified, Modality.Other, score=connfidence,
                                                      pattern=json.dumps(list(debug_info)), model="Ask2Transformers")


def event_mention_write_back(example_label_confidence_tuple):
    filtered_example_label_confidence_tuple = list()
    should_blockout_endpoints = set()
    for event_mention, label, confidence, debug_info in example_label_confidence_tuple:
        for sentence_event_mention in event_mention.sentence.event_mention_set:
            if sentence_event_mention.model == "Ask2Transformers":
                if sentence_event_mention.score > 0.999:
                    should_blockout_endpoints.add(sentence_event_mention)
    for event_mention, label, confidence, debug_info in example_label_confidence_tuple:
        if event_mention not in should_blockout_endpoints:
            filtered_example_label_confidence_tuple.append((event_mention, label, confidence, debug_info))
    for event_mention, label, confidence, debug_info in filtered_example_label_confidence_tuple:
        serif_sentence = event_mention.sentence
        modify_or_add_event_mention(serif_sentence, event_mention.start_token, event_mention.end_token, label,
                                    confidence, debug_info)


def event_mention_arg_write_back_attach_old_em(example_label_confidence_tuple):
    for left_event_mention, right_serif_theory, label, confidence, debug_info in example_label_confidence_tuple:
        EventMentionModel.add_new_event_mention_argument(left_event_mention, label, right_serif_theory, confidence,
                                                         model="Ask2Transformers", pattern=json.dumps(list(debug_info)))


def event_mention_arg_write_back_with_new_em(example_label_confidence_tuple):
    # If EventMention level have multiple grounding already, it will generate too many pairs that meaningless for UI and downstream
    represent_event_mention_id_to_event_mention = dict()
    outputted_pairs = set()
    for left_event_mention_old, right_serif_theory, label, confidence, debug_info in example_label_confidence_tuple:
        represent_event_id = unary_slot_id_generator(left_event_mention_old)
        if represent_event_id not in represent_event_mention_id_to_event_mention:
            serif_sentence = left_event_mention_old.sentence
            new_event_mention = EventMentionModel.add_new_event_mention(serif_sentence.event_mention_set, "UNDET",
                                                                        left_event_mention_old.start_token,
                                                                        left_event_mention_old.end_token,
                                                                        score=left_event_mention_old.score if left_event_mention_old.score is not None else 0.0,
                                                                        model="UIFiller", pattern_id=json.dumps([]))[0]
            represent_event_mention_id_to_event_mention[represent_event_id] = new_event_mention
        else:
            new_event_mention = represent_event_mention_id_to_event_mention[represent_event_id]
            new_event_mention.score = max(new_event_mention.score,
                                          left_event_mention_old.score if left_event_mention_old.score is not None else 0.0)
        arg_id = unary_slot_id_generator(right_serif_theory)
        if (represent_event_id, arg_id) in outputted_pairs:
            continue
        else:
            EventMentionModel.add_new_event_mention_argument(new_event_mention, label, right_serif_theory, confidence,
                                                             model="Ask2Transformers",
                                                             pattern=json.dumps(list(debug_info)))
            outputted_pairs.add((represent_event_id, arg_id))


def init_input_constraints(input_constraints_list):
    ret = list()
    for input_constraint in input_constraints_list:
        filter_ins = a2t_filter_class_name_to_class[input_constraint["name"]](**input_constraint["args"])
        ret.append(filter_ins)
    return ret


def get_original_text(serif_elem):
    if isinstance(serif_elem, Mention):
        return serif_elem.text
    elif isinstance(serif_elem, EventMention):
        return serif_elem.text
    else:
        raise NotImplementedError(type(serif_elem).__name__)


class A2TPipeline(object):
    def __init__(self):
        self.example_generator = None
        self.global_input_constraints = []
        self.ontology = None
        self.serializer = None
        self.golden_example_generator = None
        self.match_heuristic_examples = None

    def parse_ontology(self, ontology_dict_root):
        self.ontology = dict()
        # self.ontology["O"] = {
        #     "templates": [],
        #     "input_constraints": self.global_input_constraints
        # }
        for ontology_type, ontology_properties in ontology_dict_root.items():
            templates = list(ontology_properties["templates"])
            use_global_input_constraints = ontology_properties["use_global_input_constraints"]
            local_input_constraints = ontology_properties.get("input_constraints", list())
            self.ontology[ontology_type] = {
                "templates": templates,
                "input_constraints": (list(
                    self.global_input_constraints) if use_global_input_constraints is True else list()) + init_input_constraints(
                    local_input_constraints)
            }

    def a2t_predict(self, example_to_ontology_name, model, tokenizer):
        # Modified from inference.predict
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        template_mapping = dict()
        ontology_template_to_threshold = {}
        for ontology_name, ontology_config in self.ontology.items():
            template_mapping[ontology_name] = []
            for template_entry in ontology_config["templates"]:
                template_mapping[ontology_name].append(template_entry["template"])
                ontology_template_to_threshold[(ontology_name, template_entry["template"])] = template_entry[
                    "threshold"]
        _template_mapping_reverse = collections.defaultdict(list)
        for ontology_name, templates in template_mapping.items():
            for v in templates:
                _template_mapping_reverse[v].append(ontology_name)
        _labels = list(_template_mapping_reverse.keys())

        _target_labels = list(self.ontology.keys())
        if "O" not in self.ontology.keys():
            _target_labels = ["O"] + _target_labels
        _new_labels2id = {t: i for i, t in enumerate(_labels)}

        _mapping = collections.defaultdict(list)
        for ontology_name, templates in template_mapping.items():
            _mapping[ontology_name].extend([_new_labels2id[v] for v in templates])

        def idx2label(idx):
            return _target_labels[idx]

        _idx2label = np.vectorize(idx2label)

        represent_example_id_to_examples = dict()  # Say we have multiple grounding on same span, TE is not type sensitive so we want to deduplicate them

        for example, ontology_names in example_to_ontology_name.items():
            example_id = a2t_example_id_generator(example)
            represent_example_id_to_examples.setdefault(example_id, list()).append(example)
        represent_example_to_valid_templates = dict()
        for example, ontology_names in example_to_ontology_name.items():
            example_id = a2t_example_id_generator(example)
            represent_example = represent_example_id_to_examples[example_id][0]
            for ontology_name in ontology_names:
                represent_example_to_valid_templates.setdefault(represent_example, set()).update(
                    template_mapping[ontology_name])
        represent_example_in_order = list()
        for represent_example_id, examples in represent_example_id_to_examples.items():
            selected_example = examples[0]
            represent_example_in_order.append(selected_example)
        logger.info("Shrink example space from {} to {}".format(len(example_to_ontology_name.keys()),
                                                                len(represent_example_in_order)))

        hypotheses = list()
        # valid_example = np.zeros((len(example_in_order), len(_target_labels)))
        for x, represent_example in enumerate(represent_example_in_order):
            # for ontology_idx, ontology_name in enumerate(_target_labels):
            # if ontology_name in example_to_ontology_name[example] or ontology_name == "O":
            #     valid_example[x, ontology_idx] = 1.0
            for y, label_template in enumerate(_labels):
                escaped_label_template = label_template.replace("{X}", "{0}").replace("{Y}", "{1}")
                formatted_question = ""
                if len(represent_example) == 2:
                    formatted_question = escaped_label_template.format(get_original_text(represent_example[0]))
                elif len(represent_example) == 3:
                    formatted_question = escaped_label_template.format(get_original_text(represent_example[0]),
                                                                       get_original_text(represent_example[1]))
                else:
                    raise ValueError()
                hypotheses.append("{} {} {}".format(represent_example[-1], tokenizer.sep_token, formatted_question))
        batch_size = 128
        ent_position = -1
        for ontology_name, templates in model.config.label2id.items():
            if ontology_name.lower() == 'entailment':
                ent_position = templates
        if ent_position == -1:
            raise ValueError("Entailment label position not found on model configuration.")
        hypotheses = tokenizer(hypotheses, return_tensors='pt', padding=True).input_ids
        dataset = TensorDataset(hypotheses)
        data_loader = DataLoader(
            dataset,
            batch_size=batch_size
        )

        outputs = []
        with torch.no_grad():
            for (data,) in tqdm(data_loader, total=(len(dataset) // batch_size) + (len(dataset) % batch_size != 0)):
                data = data.to(device)
                output = model(data)[0].detach().cpu().numpy()
                outputs.append(output)

        outputs = np.vstack(outputs)
        outputs = np.exp(outputs) / np.exp(outputs).sum(-1, keepdims=True)
        outputs = outputs[..., ent_position].reshape(len(represent_example_in_order), -1)

        represent_example_template_probs = list()

        for x, template_scores in enumerate(outputs):
            represent_example_template_probs.append([])
            current_en = represent_example_template_probs[-1]
            for y, score in enumerate(template_scores):
                current_en.append((_labels[y], float(score)))

        example_to_type_to_infos = dict()
        result = list()
        for represent_example, predictions in zip(represent_example_in_order, represent_example_template_probs):
            if len(represent_example) == 3:
                id_left = unary_slot_id_generator(represent_example[0])
                id_right = unary_slot_id_generator(represent_example[1])
                if id_left == id_right:
                    continue
            template_type_score_list = list()
            for template, score in predictions:
                if template not in represent_example_to_valid_templates[represent_example]:
                    continue
                for pred_label in _template_mapping_reverse[template]:
                    if pred_label == "O":
                        continue
                    if score < ontology_template_to_threshold[(pred_label, template)]:
                        continue
                    template_type_score_list.append((template, pred_label, score))
            if len(template_type_score_list) < 1:
                continue
            selected_type = max(template_type_score_list, key=lambda x: x[2])[1]
            for template, pred_label, score in template_type_score_list:
                if pred_label != selected_type:
                    continue
                represent_example_id = a2t_example_id_generator(represent_example)
                for example in represent_example_id_to_examples[represent_example_id]:
                    if pred_label not in example_to_ontology_name[example]:
                        continue
                    entry = example_to_type_to_infos.setdefault(example, dict()).setdefault(pred_label, dict())
                    entry[template] = max(entry.get(template, 0.0), score)
        for example, type_to_infos in example_to_type_to_infos.items():
            for ontology_type, infos in type_to_infos.items():
                debug_infos = []
                for template, score in infos.items():
                    debug_infos.append((ontology_type, template, score))
                debug_infos = sorted(debug_infos, key=lambda x: x[2], reverse=True)
                score = debug_infos[0][2]
                if len(example) == 2:
                    slot_0 = example[0]
                    result.append((slot_0, ontology_type, score, debug_infos))
                elif len(example) == 3:
                    slot_0 = example[0]
                    slot_1 = example[1]
                    result.append((slot_0, slot_1, ontology_type, score, debug_infos))
        return result

    def generate_decode_examples_shared(self, serif_doc):
        examples = self.example_generator.generate(serif_doc)
        logger.info("Generated {} examples".format(len(examples)))
        ontology_name_to_examples = dict()
        example_to_ontology_names = dict()
        for ontology_name, model_config in self.ontology.items():
            filtered_examples = set(examples)
            for input_constraint_filter in model_config["input_constraints"]:
                filtered_examples = set(filter(input_constraint_filter.filter, filtered_examples))
            ontology_name_to_examples[ontology_name] = filtered_examples
            logger.info("Under {} we have {} examples".format(ontology_name, len(filtered_examples)))
            for example in filtered_examples:
                example_to_ontology_names.setdefault(example, set()).add(ontology_name)
        # Filter O only examples
        filtered_example_to_ontology_names = dict()
        for example, ontology_names in example_to_ontology_names.items():
            if len(ontology_names.difference({"O"})) > 0:
                filtered_example_to_ontology_names[example] = ontology_names
        return filtered_example_to_ontology_names

    def decode(self, serif_doc, model, tokenizer):
        if len(self.ontology) < 1:
            return
        filtered_example_to_ontology_names = self.generate_decode_examples_shared(serif_doc)
        if len(filtered_example_to_ontology_names) > 0:
            elem_predict_confidence_tuple = self.a2t_predict(filtered_example_to_ontology_names, model, tokenizer)
            self.serializer(elem_predict_confidence_tuple)

    def generate_golden_examples_and_candidates(self, serif_doc):
        filtered_example_to_ontology_names = self.generate_decode_examples_shared(serif_doc)
        golden_examples = self.golden_example_generator.generate(serif_doc)
        return self.match_heuristic_examples(set(filtered_example_to_ontology_names.keys()), golden_examples)


class A2TDriver(DocumentModel):
    def __init__(self, **kwargs):
        super(A2TDriver, self).__init__(**kwargs)
        self.current_model_name = "ynie/roberta-large-snli_mnli_fever_anli_R1_R2_R3-nli"
        self.current_model = None
        self.current_tokenizer = None

    def load_model(self):
        """Load the NLI model from HuggingFace given a pretrained name or path.

        Args:
            pretrained_model (str, optional): Pretrained model name or path. Defaults to "microsoft/deberta-v2-xlarge-mnli".

        """
        logger.info("Loading model {}".format(self.current_model_name))
        model = AutoModelForSequenceClassification.from_pretrained(self.current_model_name)
        tokenizer = AutoTokenizer.from_pretrained(self.current_model_name)
        if torch.cuda.is_available():
            device = torch.device("cuda")
            model.to(device).half().eval()
        logger.info("Finished loading model {}".format(self.current_model_name))
        self.current_model = model
        self.current_tokenizer = tokenizer

    def unload_model(self):
        if self.current_model is not None:
            del self.current_model
        self.current_model = None
        if self.current_tokenizer is not None:
            del self.current_tokenizer
        self.current_tokenizer = None
        torch.cuda.empty_cache()

    def parse_template(self, template_dict):
        self.template_dict = template_dict
        self.ner_pipeline = None
        self.entity_relation_pipeline = None
        self.event_mention_pipeline = None
        self.event_mention_arg_pipeline = None
        # NER
        if "entity_mention" in template_dict["stages_to_run"]:
            self.ner_pipeline = A2TPipeline()
            self.ner_pipeline.example_generator = GenericUnaryExampleGenerator({"Mention"})
            self.ner_pipeline.global_input_constraints = list()
            self.ner_pipeline.global_input_constraints.extend(
                init_input_constraints(template_dict["entity_mention"]["input_constraints"]))
            self.ner_pipeline.parse_ontology(template_dict["entity_mention"]["ontology"])
            self.ner_pipeline.serializer = ner_write_back
        # entity_mention_entity_mention_relation
        if "entity_mention_relation" in template_dict["stages_to_run"]:
            self.entity_relation_pipeline = A2TPipeline()
            self.entity_relation_pipeline.example_generator = GenericBinaryExampleGenerator({"Mention"}, {"Mention"})
            self.entity_relation_pipeline.global_input_constraints = list()
            self.entity_relation_pipeline.global_input_constraints.extend(
                init_input_constraints(template_dict["entity_mention_relation"]["input_constraints"]))
            self.entity_relation_pipeline.parse_ontology(template_dict["entity_mention_relation"]["ontology"])
            self.entity_relation_pipeline.serializer = entity_mention_entity_mention_relation_write_back
        # event_mention
        if "event_mention" in template_dict["stages_to_run"]:
            self.event_mention_pipeline = A2TPipeline()
            self.event_mention_pipeline.example_generator = GenericUnaryExampleGenerator({"EventMention"})
            self.event_mention_pipeline.global_input_constraints = list()
            self.event_mention_pipeline.global_input_constraints.extend(
                init_input_constraints(template_dict["event_mention"]["input_constraints"]))
            self.event_mention_pipeline.parse_ontology(template_dict["event_mention"]["ontology"])
            self.event_mention_pipeline.serializer = event_mention_write_back

        # event_mention_args
        if "event_mention_argument" in template_dict["stages_to_run"]:
            self.event_mention_arg_pipeline = A2TPipeline()
            self.event_mention_arg_pipeline.example_generator = GenericBinaryExampleGenerator({"EventMention"},
                                                                                              {"Mention",
                                                                                               "EventMention"})
            self.event_mention_arg_pipeline.global_input_constraints = list()
            self.event_mention_arg_pipeline.global_input_constraints.extend(
                init_input_constraints(template_dict["event_mention_argument"]["input_constraints"]))
            self.event_mention_arg_pipeline.parse_ontology(template_dict["event_mention_argument"]["ontology"])
            self.event_mention_arg_pipeline.serializer = event_mention_arg_write_back_with_new_em
            self.event_mention_arg_pipeline.match_heuristic_examples = graunlar_eventarg_golden_example_matcher
            self.event_mention_arg_pipeline.golden_example_generator = GranularSpecialEventArgGoldenGenerator()

    def process_document(self, serif_doc):
        # NER
        if self.ner_pipeline is not None:
            self.ner_pipeline.decode(serif_doc, self.current_model, self.current_tokenizer)
        # entity_mention_entity_mention_relation
        if self.entity_relation_pipeline is not None:
            self.entity_relation_pipeline.decode(serif_doc, self.current_model, self.current_tokenizer)
        # event_mention
        if self.event_mention_pipeline is not None:
            self.event_mention_pipeline.decode(serif_doc, self.current_model, self.current_tokenizer)
        # event_mention_args
        if self.event_mention_arg_pipeline is not None:
            self.event_mention_arg_pipeline.decode(serif_doc, self.current_model, self.current_tokenizer)

    def prepopulate_annotation_sentences(self, serif_docs):
        ret = list()
        for serif_doc in serif_docs:
            ret.extend(self.event_mention_arg_pipeline.generate_golden_examples_and_candidates(serif_doc))
        return ret


def stanza_a2t_joint_test():
    logging.basicConfig(level=logging.getLevelName(os.environ.get('LOGLEVEL', 'INFO').upper()),
                        format='[%(asctime)s] {P%(process)d:%(module)s:%(lineno)d} %(levelname)s - %(message)s')
    from serif.driver.pipeline_service_base import PySerifPipeline
    import yaml
    config_path = os.path.join(project_root, "config/config_a2t_basic_nlp.yml")
    output_path = "/home/hqiu/tmp/"
    input_txt = dict()
    # with open("/nfs/raid88/u10/users/hqiu_ad/repos/text-open/src/python/test/sample_doc.txt") as fp:
    #     for idx, i in enumerate(fp):
    #         i = i.strip()
    #         if len(i) > 1:
    #             input_txt["doc_{}".format(idx)] = i
    input_txt["doc_1"] = "John Smith, an executive at XYZ Corp., died in Florida on Sunday."
    with open(config_path) as fp:
        config_dict = yaml.full_load(fp)
    pyserif_nlp_basic_pipeline = PySerifPipeline.from_config_dict(config_dict)
    pyserif_nlp_basic_pipeline.load_models()
    serif_docs = pyserif_nlp_basic_pipeline.process_txts(input_txt, lang="English")
    with open(os.path.join(project_root, "serif/model/impl/a2t_adapter/default_config_local_test.json")) as fp:
        a2t_config = json.load(fp)
    a2t_driver = A2TDriver()
    a2t_driver.load_model()
    a2t_driver.parse_template(a2t_config)
    for serif_doc in serif_docs:
        a2t_driver.process_document(serif_doc)
        serif_doc.save(os.path.join(output_path, "{}.xml".format(serif_doc.docid)))


if __name__ == "__main__":
    stanza_a2t_joint_test()
