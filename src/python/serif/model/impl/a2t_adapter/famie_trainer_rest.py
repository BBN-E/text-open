import enum
import json
import logging
import os
import time

import requests

logger = logging.getLogger(__name__)


class Task(enum.Enum):
    event_mention = enum.auto()
    event_mention_argument = enum.auto()


class RoundRobinFeatureSelector():
    NATag = "NA"

    def __init__(self, all_feature_tags, num_instances_desired):
        self.pool = {
            feature_tag: set() for feature_tag in all_feature_tags
        }
        self.pool[self.NATag] = set()
        self.valid_example_ids = set()

    def observe_datapoint(self, example_id, tags):
        self.valid_example_ids.add(example_id)
        if len(tags) < 1:
            self.pool[self.NATag].add(example_id)
        else:
            for tag in tags:
                self.pool[tag].add(example_id)

    def build_dataset(self):
        ret = []
        non_na_tags = sorted(list(set(self.pool.keys())))
        non_na_tags.remove(self.NATag)
        current_iter = 0
        while True:
            selected_tag = non_na_tags[current_iter % len(non_na_tags)]
            if len(self.pool[selected_tag]) > 0:
                selected_example_id = list(self.pool[selected_tag])[0]
                self.valid_example_ids.remove(selected_example_id)
                for tag, example_ids in self.pool.items():
                    example_ids.discard(selected_example_id)
                ret.append(selected_example_id)
            current_iter += 1
            logger.info("At iter {}".format(current_iter))
            if sum(len(i) for i in self.pool.values()) - len(self.pool[self.NATag]) == 0:
                break
        if len(self.valid_example_ids) > 0:
            ret.extend(sorted(list(self.valid_example_ids)))
        self.valid_example_ids.clear()
        return ret

class My1ExampleSelectionStrategy():
    NATag = "NA"
    def __init__(self, all_feature_tags ,num_instances_desired, min_num_of_examples_per_type):
        self.pool = {
            feature_tag: set() for feature_tag in all_feature_tags
        }
        self.pool[self.NATag] = set()
        self.example_id_to_tag_cnt = dict()
        self.num_instances_desired = num_instances_desired
        self.min_num_of_examples_per_type = min_num_of_examples_per_type

    def observe_datapoint(self, example_id, tags):
        for tag in tags:
            en = self.example_id_to_tag_cnt.setdefault(example_id, dict())
            en[tag] = en.get(tag, 0) + 1
        for tag in self.pool.keys():
            en = self.example_id_to_tag_cnt.setdefault(example_id, dict())
            if tag not in en:
                if tag != self.NATag:
                    en[tag] = 0
                elif len(tags) < 1:
                    en[tag] = 0
        if len(tags) < 1:
            self.pool[self.NATag].add(example_id)
        else:
            for tag in tags:
                self.pool[tag].add(example_id)

    def remove_example_id_references(self, example_id):
        for tag, example_ids in self.pool.items():
            example_ids.discard(example_id)
        if example_id in self.example_id_to_tag_cnt:
            del self.example_id_to_tag_cnt[example_id]

    def build_dataset(self):
        logger.info("Start building dataset for num_instances_desired: {} min_num_of_examples_per_type: {}".format(self.num_instances_desired, self.min_num_of_examples_per_type))
        selected = []
        min_num_of_examples_quota = {
            type_tag: self.min_num_of_examples_per_type for type_tag in self.pool
        }
        del min_num_of_examples_quota[self.NATag]
        num_instances_allocated = 0

        minimum_iter = 0
        min_tags = sorted(list(min_num_of_examples_quota.keys()))
        while True:
            current_tag = min_tags[minimum_iter % len(min_tags)]
            if min_num_of_examples_quota[current_tag] > 0 and len(self.pool[current_tag]) > 0:
                selected_example_id = list(self.pool[current_tag])[0]
                tag_to_cnt = self.example_id_to_tag_cnt[selected_example_id]
                num_instances_allocated += sum(tag_to_cnt.values())
                min_num_of_examples_quota[current_tag] -= self.example_id_to_tag_cnt[selected_example_id][current_tag]
                self.remove_example_id_references(selected_example_id)
                selected.append(selected_example_id)
            elif min_num_of_examples_quota[current_tag] > 0 and len(self.pool[current_tag]) < 1:
                min_num_of_examples_quota[current_tag] = 0
            minimum_iter += 1
            should_break = True
            for type_tag, quota in min_num_of_examples_quota.items():
                if quota > 0:
                    should_break = False
                    break
            if should_break:
                break
            if sum(len(i) for j in min_tags for i in self.pool[j]) < 1:
                break
        logger.info("Quota: {}".format(min_num_of_examples_quota))
        logger.info("Num instances allocated: {}".format(num_instances_allocated))
        logger.info("Pool size left: {}".format(sum(len(i) for j in min_tags for i in self.pool[j])))
        left_example_ids = sorted(list(self.example_id_to_tag_cnt.keys()))
        left_example_ids = list(filter(lambda x:"NA" not in self.example_id_to_tag_cnt[x], left_example_ids))
        while num_instances_allocated < self.num_instances_desired and sum(len(i) for j in min_tags for i in self.pool[j]) > 0:
            selected_example_id = left_example_ids.pop(0)
            tag_to_cnt = self.example_id_to_tag_cnt[selected_example_id]
            num_instances_allocated += sum(tag_to_cnt.values())
            self.remove_example_id_references(selected_example_id)
            selected.append(selected_example_id)
        logger.info("End building dataset for num_instances_desired: {} min_num_of_examples_per_type: {}. Selected: {} , left: {}".format(
            self.num_instances_desired, self.min_num_of_examples_per_type, len(selected), len(self.example_id_to_tag_cnt.keys())))
        return selected, list(self.example_id_to_tag_cnt.keys())





allowed_event_types = {
    "Communicate-Event": "Communicate-Event",
    "Weather-or-Environmental-Damage": "Damage",
    "Death-from-Crisis-Event": "Death",
    "Disease-Outbreak": "Disease-Infects-Or-Outbreak",
    "Disease-Infects": "Disease-Infects-Or-Outbreak",
    "Natural-Phenomenon-Event-or-SoA": "Natural-Phenomenon-Event-or-SoA",
    "Apply-NPI": "Provide-Aid",
    "Provide-Aid": "Provide-Aid",
    "Refugee-Movement": "Refugee-Movement"
}

granular_role_type_to_zs4ie_type = {
    "where": "location",
    # "NPI-Events",
    # "related-natural-phenomena",
    "outcome": "outcome",
    "when": "when",
    "current-location": "location",
    "origin": "location",
    # "damage",
    "killed-count": "killed",
    # "Assistance-provided",
    # "disease",
    # "human-displacement-event",
    # "major-disaster-event",
    # "event-or-SoA-at-origin",
    "total-displaced-count": "displaced",
    # "outbreak-event",
    # "group-identity",
    "destination": "location",
    "transiting-location": "location",
    "outcome-occurred": "outcome",
    "infected-count": "infected",
    # "settlement-status-event-or-SoA",
    # "human-displacement-events",
    # "responders",
    "assistance-provided": "outcome",
    # "Transitory-events",
    "who": "who",
    # "rescue-events",
    "infected-cumulative": "infected",
    "killed-cumulative": "killed",
    # "missing-count",
    # "injured-count",
    # "Assistance-needed",
    "infected-individuals": "infected",
    # "terror-event",
    # "target-physical",
    # "affected-cumulative-count",
    # "protest-against",
    # "protest-event",
    # "assistance-needed",
    "killed": "killed",
    "killed-individuals": "killed",
    # "repair",
    "outcome-hypothetical": "outcome",
    # "announce-disaster-warnings",
    # "protest-for",
    # "tested-count",
    # "tested-individuals",
    # "organizer",
    # "hospitalized-individuals",
    # "detained-count",
    # "hospitalized-count",
    # "judicial-actions",
    # "named-perp-org",
    # "recovered-count",
    # "blamed-by",
    # "weapon",
    # "vaccinated-individuals",
    # "named-organizer",
    # "charged-with",
    # "declare-emergency",
    # "rescued-count",
    # "target-human",
    # "wounded",
    # "perp-killed",
    "tested-cumulative": "infected",
    # "outcome-averted",
    # "corrupt-event",
    # "recovered-cumulative",
    # "claimed-by",
    # "named-perp",
    # "occupy",
    # "blocked-migration-count",
    # "exposed-cumulative",
    # "vaccinated-count",
    # "vaccinated-cumulative",
    # "arrested",
    # "perp-objective",
    # "exposed-individuals",
    # "perp-captured",
    # "disease-outbreak-events",
    # "exposed-count",
    # "individuals-affected",
    # "imprisoned",
}

allowed_entity_types = {
    "displaced": {"type": "entity", "label": "PER"},
    "infected": {"type": "entity", "label": "PER"},
    "killed": {"type": "entity", "label": "PER"},
    "location": {"type": "entity", "label": "LOC-GPE"},
    "outcome": {"type": "event"},
    "when": {"type": "entity", "label": "WHEN"},
    "who": {"type": "entity", "label": "PER"},
}


def map_granular_role_to_zs4ie_role(granular_role):
    if granular_role not in granular_role_type_to_zs4ie_type:
        return None, None
    else:
        return granular_role_type_to_zs4ie_type[granular_role], allowed_entity_types.get(
            granular_role_type_to_zs4ie_type[granular_role], None)


event_constraint = [
    "EventMention"
]

event_argument_constraint = {
    "displaced": [
        [
            "Refugee-Movement",
            "PER"
        ]
    ],
    "infected": [
        [
            "Disease-Infects-Or-Outbreak",
            "PER"
        ]
    ],
    "killed": [
        [
            "Death",
            "PER"
        ],
        [
            "Disease-Infects-Or-Outbreak",
            "PER"
        ],
        [
            "Refugee-Movement",
            "PER"
        ]
    ],
    "location": [
        [
            "*",
            "LOC-GPE"
        ],
        [
            "*",
            "FAC"
        ]
    ],
    "outcome": [
        [
            "Damage",
            "Death"
        ],
        [
            "Disease-Infects-Or-Outbreak",
            "Death"
        ],
        [
            "Natural-Phenomenon-Event-or-SoA",
            "Death"
        ],
        [
            "Natural-Phenomenon-Event-or-SoA",
            "Damage"
        ],
        [
            "Disease-Infects-Or-Outbreak",
            "Refugee-Movement"
        ],
        [
            "Natural-Phenomenon-Event-or-SoA",
            "Refugee-Movement"
        ]
    ],
    "when": [
        [
            "*",
            "WHEN"
        ]
    ],
    "who": [
        [
            "*",
            "PER"
        ]
    ]
}


def read_jsonl(input_ljson_path):
    sent_id_to_sentence = dict()
    with open(input_ljson_path) as fp:
        for i in fp:
            i = i.strip()
            sent = json.loads(i)
            sent_id = sent["sent_id"]
            sent_id_to_sentence[sent_id] = sent
    return sent_id_to_sentence


def assemble_sent_en(sent_en):
    serif_tokens = []
    current_offset = 0
    for token in sent_en["tokens"]:
        serif_tokens.append([current_offset, current_offset + len(token) - 1, token])
        current_offset += len(token) + 1  # 1 for space
    ret = {
        "example_id": sent_en["sent_id"],
        "text": " ".join(sent_en["tokens"]),
        "tokens": [
            {
                "text": token[2],
                "span": [token[0], token[1] + 1]
            } for token in serif_tokens
        ],
        "event_mentions": []
    }
    return ret


def event_extraction_data_split(sent_id_to_sentence, allocator_param):
    # my_selector = RoundRobinFeatureSelector(set(allowed_event_types.values()))
    my_selector = My1ExampleSelectionStrategy(allowed_event_types.values(), allocator_param[0], allocator_param[1])
    for sent_id, sent_en in sent_id_to_sentence.items():
        possible_event_types = list()
        for _, _, event_type in sent_en["event_spans"]:
            if event_type in allowed_event_types:
                possible_event_types.append(allowed_event_types[event_type])
        my_selector.observe_datapoint(sent_id, possible_event_types)
    positive_ids, neutral_ids = my_selector.build_dataset()
    positive_famie_examples = list()
    neutral_famie_examples = list()
    for positive_id in positive_ids:
        sent_en = sent_id_to_sentence[positive_id]
        famie_sent_en = assemble_sent_en(sent_en)
        for event_span in sent_en["event_spans"]:
            start_token_idx, end_token_idx, event_type = event_span
            if event_type in allowed_event_types:
                converted_event_type = allowed_event_types[event_type]
                famie_sent_en["event_mentions"].append({
                    "trigger": [start_token_idx, end_token_idx + 1, converted_event_type]
                })
        positive_famie_examples.append(famie_sent_en)

    for neutral_id in neutral_ids:
        sent_en = sent_id_to_sentence[neutral_id]
        famie_sent_en = assemble_sent_en(sent_en)
        slot_types = [set() for _ in famie_sent_en["tokens"]]
        for event_span in sent_en["event_spans"]:
            start_token_idx, end_token_idx, event_type = event_span
            for idx in range(start_token_idx, end_token_idx + 1):
                slot_types[idx].add("EventMention")
                slot_types[idx].add(event_type)
        famie_sent_en["better_demo_passthrough"] = {
            "token_to_possible_types": [list(i) for i in slot_types]
        }
        neutral_famie_examples.append(famie_sent_en)
    return positive_famie_examples, neutral_famie_examples


def event_argument_extraction_data_split(sent_id_to_sentence, allocator_param):
    my_selector = My1ExampleSelectionStrategy(allowed_entity_types.keys(), allocator_param[0], allocator_param[1])
    for sent_id, sent_en in sent_id_to_sentence.items():
        possible_arg_roles = list()
        for _, original_role, _ in sent_en["event_arg_edges"]:
            role, _ = map_granular_role_to_zs4ie_role(original_role)
            if role in allowed_entity_types:
                possible_arg_roles.append(role)
        my_selector.observe_datapoint(sent_id, possible_arg_roles)
    positive_ids, neutral_ids = my_selector.build_dataset()
    positive_famie_examples = list()
    neutral_famie_examples = list()
    for positive_id in positive_ids:
        sent_en = sent_id_to_sentence[positive_id]
        famie_sent_en = assemble_sent_en(sent_en)
        left_span_to_arguments = dict()
        for left_span, original_role, right_span in sent_en["event_arg_edges"]:
            left_token_start, left_token_end, left_type = left_span
            role, _ = map_granular_role_to_zs4ie_role(original_role)
            if role in allowed_entity_types:
                right_token_start, right_token_end, right_type, right_span_type = right_span
                left_span_to_arguments.setdefault((left_token_start, left_token_end + 1, left_type), list()).append(
                    [right_token_start, right_token_end + 1, role]
                )
        for left_span, right_spans in left_span_to_arguments.items():
            famie_sent_en["event_mentions"].append({
                "trigger": [left_span[0], left_span[1], allowed_event_types.get(left_span[2], "Event")],
                "arguments": right_spans
            })
        positive_famie_examples.append(famie_sent_en)
    for neutral_id in neutral_ids:
        sent_en = sent_id_to_sentence[neutral_id]
        famie_sent_en = assemble_sent_en(sent_en)
        slot_types = [set() for _ in famie_sent_en["tokens"]]
        sent_bio = ["O" for _ in famie_sent_en["tokens"]]
        for event_span in sent_en["event_spans"]:
            start_token_idx, end_token_idx, event_type = event_span
            resolved_event_type = allowed_event_types.get(event_type, "Event")
            for idx in range(start_token_idx, end_token_idx + 1):
                slot_types[idx].add("EventMention")
                slot_types[idx].add(resolved_event_type)
                for idx in range(start_token_idx, end_token_idx + 1):
                    if idx == start_token_idx:
                        sent_bio[idx] = "B-{}".format(resolved_event_type)
                    else:
                        sent_bio[idx] = "I-{}".format(resolved_event_type)
        for start_token_idx, end_token_idx, entity_type, original_role in sent_en["entity_spans"]:
            role, _ = map_granular_role_to_zs4ie_role(original_role)
            if role in allowed_entity_types:
                right_type_mapping_en = allowed_entity_types[role]
                if right_type_mapping_en["type"] == "entity":
                    right_type = right_type_mapping_en["label"]
                    for idx in range(start_token_idx, end_token_idx + 1):
                        slot_types[idx].add("Mention")
                        slot_types[idx].add(right_type)
        famie_sent_en["better_demo_passthrough"] = {
            "token_to_possible_types": [list(i) for i in slot_types]
        }
        famie_sent_en["labels"] = sent_bio
        neutral_famie_examples.append(famie_sent_en)
    return positive_famie_examples, neutral_famie_examples


def famie_doc_render(famie_docs, id_to_type):
    for doc in famie_docs['docs']:
        doc_id = doc['example_id']
        famie_tokens = doc["tokens"]
        converted_tokens = list()
        start_char_to_token_idx = dict()
        end_char_to_token_idx = dict()
        for famie_token in famie_tokens:
            converted_tokens.append({
                "text": famie_token["text"],
                "idx": famie_token["id"] - 1
            })
            start_char_to_token_idx[famie_token["span"][0]] = famie_token["id"] - 1
            end_char_to_token_idx[famie_token["span"][1]] = famie_token["id"] - 1
        logger.info("Sentence: {}".format(" ".join(i["text"] for i in converted_tokens)))
        logger.info(json.dumps(doc))
        for span in doc["label"]:
            start_token_idx = start_char_to_token_idx[span["start"]]
            end_token_idx = end_char_to_token_idx[span["end"]]
            logger.info(
                "\t{} is {}".format(" ".join(i["text"] for i in converted_tokens[start_token_idx:end_token_idx + 1]),
                                    id_to_type[span["entityId"]]))


def add_metadata_builder(project_name, project_task_type):
    def adder(famie_sent_en):
        famie_sent_en["project_name"] = project_name
        famie_sent_en["project_task_type"] = project_task_type
        return famie_sent_en

    return adder


def training_data_prep(sent_id_to_sentence, allocator_param, famie_project_name, task):
    if task is Task.event_mention:
        positive_examples, neutral_examples = event_extraction_data_split(sent_id_to_sentence, allocator_param)
        neutral_examples = list(map(add_metadata_builder(famie_project_name, "unconditional"), neutral_examples))
        ontology_types = set(allowed_event_types.values())
        constraints = event_constraint
    elif task is Task.event_mention_argument:
        positive_examples, neutral_examples = event_argument_extraction_data_split(sent_id_to_sentence, allocator_param)
        neutral_examples = list(map(add_metadata_builder(famie_project_name, "conditional"), neutral_examples))
        ontology_types = set(allowed_entity_types.keys())
        constraints = event_argument_constraint
    else:
        raise NotImplementedError(task)
    return positive_examples, neutral_examples, ontology_types, constraints


def online_corpus_builder(sent_id_to_sentence, allocator_param, famie_project_name, famie_endpoint, task):
    positive_examples, neutral_examples, ontology_types, constraints = training_data_prep(sent_id_to_sentence,
                                                                                          allocator_param,
                                                                                          famie_project_name, task)
    with requests.Session() as session:
        r = session.delete(famie_endpoint + "/api/delete-project/{}".format(famie_project_name))
        logger.info(r.text)
        r = session.post(famie_endpoint + "/api/upload", files={
            "file": "\n".join(json.dumps(i) for i in neutral_examples),
            "project_name": (None, famie_project_name),
            "project_type": (None, "ner"),
            "file_type": (None, "documents"),
            "column_names": (None, json.dumps({"text": "text"})),
            "upload_id": (None, "SOMETHING RANDOM")
        })
        logger.info(r.text)
        r = session.post(famie_endpoint + "/api/better_demo/upload_ontology", files={
            "project_name": (None, famie_project_name),
            "ontology_string": json.dumps(constraints)
        })
        logger.info(r.text)
        r = session.post(famie_endpoint + "/api/classnames", files={
            "file": "\n".join(sorted(list(ontology_types))),
            "column_name": (None, "label"),
            "project_name": (None, famie_project_name),
        })
        logger.info(r.text)
        r = session.post(famie_endpoint + "/api/upload-labeled-data", files={
            "project_name": (None, famie_project_name),
            "column_name": (None, "RANDOM"),
            "file": "\n".join(json.dumps(i) for i in positive_examples)
        })
        logger.info(r.text)


def progress_checker(famie_endpoint, check_interval):
    logger.info("Start waiting {} model training".format(famie_endpoint))
    iteration = 0
    with requests.Session() as session:
        while True:
            r = session.get(famie_endpoint + "/api/better_demo/training_log")
            resp = r.json()
            log_lines = list(i.strip() for i in resp["log"].splitlines())
            log_lines = list(filter(lambda x: len(x) > 0, log_lines))
            if len(log_lines) >= 2:
                if "Saving trained main model ..." in log_lines[-2] and '-' * 50 in log_lines[-1]:
                    break
            iteration += 1
            if iteration % 10 == 0:
                logger.info("Waiting {} , current iter {} , log tail {}".format(famie_endpoint, iteration,
                                                                                log_lines[-1] if len(
                                                                                    log_lines) > 0 else ""))
            time.sleep(check_interval)
    logger.info("End waiting {} model training".format(famie_endpoint))


def task_trainer(sent_id_to_sentence, famie_endpoint, famie_project_name, task, num_positive, check_interval):
    online_corpus_builder(sent_id_to_sentence, num_positive, famie_project_name, famie_endpoint, task)
    r = requests.post(famie_endpoint + "/api/start-iteration", files={
        "project_name": (None, famie_project_name),
        "update_id": (None, "SOMETHING RANDOM")
    })
    logger.info(r.text)
    r = requests.get(famie_endpoint + "/api/get-docs", params={
        "from": 0,
        "to": 10000,
        "project_name": famie_project_name,
        "session_id": "SOMETHING RANDOM",
        "label": "none"
    })
    logger.info(r.text)
    progress_checker(famie_endpoint, check_interval)


def task_decoder(famie_endpoint, decode_json_path, famie_project_name_prefix, event_project_name,
                 event_argument_project_name, output_dir, active_lang):
    os.makedirs(output_dir, exist_ok=True)
    with open(decode_json_path) as fp:
        decode_json_str = fp.read()
    r = requests.post(famie_endpoint + "/api/better_demo/decoding", files={
        "event_extraction_project_name": (None, event_project_name),
        "event_argument_extraction_project_name": (None, event_argument_project_name),
        "input_json_doc": decode_json_str,
        "active_lang": (None, active_lang)
    })
    with open(os.path.join(output_dir, "{}decoding.json".format(famie_project_name_prefix)), 'w') as wfp:
        json.dump(r.json(), wfp, indent=4, sort_keys=True, ensure_ascii=False)


def split_trainer_and_decoder(input_ljson_path, famie_project_name_prefix, famie_endpoint, num_positive, check_interval,
                              decode_json_path, output_dir, decode_active_lang):
    sent_id_to_sentence = read_jsonl(input_ljson_path)
    event_project_name = "{}{}".format(famie_project_name_prefix, "event_mention")
    task_trainer(sent_id_to_sentence, famie_endpoint, event_project_name, Task.event_mention, num_positive,
                 check_interval)
    event_argument_project_name = "{}{}".format(famie_project_name_prefix, "event_mention_argument")
    task_trainer(sent_id_to_sentence, famie_endpoint, event_argument_project_name, Task.event_mention_argument,
                 num_positive, check_interval)
    task_decoder(famie_endpoint, decode_json_path, famie_project_name_prefix, event_project_name,
                 event_argument_project_name, output_dir, decode_active_lang)


def print_all_arg_role(input_ljson_path):
    role_to_cnt = dict()
    sent_id_to_sent = read_jsonl(input_ljson_path)
    for sent_id, sent_en in sent_id_to_sent.items():
        for _, role, _ in sent_en["event_arg_edges"]:
            role_to_cnt[role] = role_to_cnt.get(role, 0) + 1
    for role, cnt in sorted(role_to_cnt.items(), key=lambda x: x[1], reverse=True):
        print("\"{}\" : \"{}\",".format(role, role))


def online_model_training_decoding(allocator_param, input_ljson_path, famie_endpoint, check_interval,
                                   decode_json_path, output_dir, active_lang):
    for num_positive_instance, min_num_of_example_per_class in allocator_param:
        famie_project_name_prefix = "{}_example_".format(num_positive_instance)
        split_trainer_and_decoder(input_ljson_path, famie_project_name_prefix, famie_endpoint, (num_positive_instance, min_num_of_example_per_class), check_interval, decode_json_path, output_dir, active_lang)


def offline_model_training_data_prep(allocator_param, input_ljson_path, event_mention_famie_project_name,
                                     event_mention_arg_famie_project_name, output_dir):
    sent_id_to_sent = read_jsonl(input_ljson_path)
    os.makedirs(output_dir, exist_ok=True)
    positive_examples, neutral_examples, ontology_types, constraints = training_data_prep(sent_id_to_sent,
                                                                                          allocator_param,
                                                                                          event_mention_famie_project_name,
                                                                                          Task.event_mention)
    with open(os.path.join(output_dir, "{}.json".format(event_mention_famie_project_name)), 'w') as wfp:
        json.dump({
            "positive_examples": positive_examples,
            "neutral_examples": neutral_examples,
            "ontology_types": sorted(list(ontology_types)),
            "constraints": constraints,
            "project_name": event_mention_famie_project_name
        }, wfp, ensure_ascii=False)
    positive_examples, neutral_examples, ontology_types, constraints = training_data_prep(sent_id_to_sent,
                                                                                          allocator_param,
                                                                                          event_mention_arg_famie_project_name,
                                                                                          Task.event_mention_argument)
    with open(os.path.join(output_dir, "{}.json".format(event_mention_arg_famie_project_name)), 'w') as wfp:
        json.dump({
            "positive_examples": positive_examples,
            "neutral_examples": neutral_examples,
            "ontology_types": sorted(list(ontology_types)),
            "constraints": constraints,
            "project_name": event_mention_arg_famie_project_name
        }, wfp, ensure_ascii=False)


if __name__ == "__main__":
    log_format = '[%(asctime)s] {P%(process)d:%(module)s:%(lineno)d} %(levelname)s - %(message)s'
    logging.basicConfig(level=logging.INFO, format=log_format)
    input_ljson_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_train_sentences_all.ljson"
    # decode_json_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_devtest_sentences_all_ui_nlplingo.json"
    # decode_json_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_analysis_sentences_all_ui_nlplingo.json"


    # output_dir = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/decode/"
    famie_endpoint = "http://bet-rtx8000-100:5011"
    # num_positive_examples = [(250, 10), (300, 10), (350, 10), (400, 10), (450, 10)]
    # num_positive_examples = [(400, 10), (450, 10)]
    # num_positive_examples = [(250, 10), (300, 10), (350, 10)]

    # online_model_training_decoding(num_positive_examples, input_ljson_path, famie_endpoint, 5, decode_json_path, output_dir, "english")

    # Generate UI data
    # output_dir = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/golden_training_famie"
    # offline_model_training_data_prep((300, 10), input_ljson_path, "BETTER_event_mention", "BETTER_event_mention_argument", output_dir)

    # Chinese
    output_dir = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/farsi_decoding_current"
    decode_json_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_farsi_sentences_all_ui_nlplingo.json"
    num_positive_examples = [(250, 10), (300, 10), (350, 10), (400, 10), (450, 10)]
    for num_positive_example in num_positive_examples:
        famie_project_name_prefix = "{}_example_".format(num_positive_example[0])
        event_project_name = "{}{}".format(famie_project_name_prefix, "event_mention")
        event_argument_project_name = "{}{}".format(famie_project_name_prefix, "event_mention_argument")
        task_decoder(famie_endpoint, decode_json_path, famie_project_name_prefix, event_project_name,
                     event_argument_project_name, output_dir, "persian")
