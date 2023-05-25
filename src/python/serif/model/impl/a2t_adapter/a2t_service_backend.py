import datetime
import io
import json
import os
import sys

import yaml

current_script_path = __file__
project_root = os.path.realpath(
    os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

import flask
import flask_cors
import logging

logger = logging.getLogger(__name__)
from serif.theory.document import Document
from serif.driver.pipeline_service_base import PySerifPipeline
from serif.model.impl.a2t_adapter.A2TDriver import A2TDriver
from serif.model.impl.a2t_adapter.special_text_parser import convert_marked_text_into_plain_text_and_markings, \
    add_spans_into_serifxml
from serif.model.impl.a2t_adapter.utils import modify_serifxml_from_unary_markings
from serif.model.impl.a2t_adapter.json_doc import serif_json_seralizer, serifxml_to_string


def generate_mention_key(doc_id, sent_idx, mention_entry):
    return (
        doc_id, sent_idx, mention_entry["start_token_idx"], mention_entry["end_token_idx"],
        mention_entry["entity_type"])


def generate_entity_relation_key(doc_id, left_mention_key, right_mention_key, relation_entry):
    return (doc_id, left_mention_key, right_mention_key, relation_entry["rel_type"])


def generate_event_mention_key(doc_id, sent_idx, event_mention_entry):
    return (doc_id, sent_idx, event_mention_entry["start_token_idx"],
            event_mention_entry["end_token_idx"], event_mention_entry["event_type"])


def generate_event_argument_key(doc_id, event_mention_key, mention_key, event_arg_entry):
    return (doc_id, event_mention_key, mention_key, event_arg_entry["role"], event_arg_entry["serif_type"])


def build_json_id_to_json_entries(json_doc):
    mention_id_to_mention = dict()
    mention_id_to_sentence = dict()
    event_mention_id_to_event_mention = dict()
    event_mention_id_to_sentence = dict()
    for sentence in json_doc["sentences"]:
        for mention in sentence["mentions"]:
            mention_id_to_mention[mention["mention_id"]] = mention
            mention_id_to_sentence[mention["mention_id"]] = sentence
        for event_mention in sentence["event_mentions"]:
            event_mention_id_to_event_mention[event_mention["event_mention_id"]] = event_mention
            event_mention_id_to_sentence[event_mention["event_mention_id"]] = sentence
    return mention_id_to_mention, mention_id_to_sentence, event_mention_id_to_event_mention, event_mention_id_to_sentence


def build_customer_k_to_objects(json_doc, mention_id_to_mention, mention_id_to_sentence,
                                event_mention_id_to_event_mention, event_mention_id_to_sentence):
    mention_k_to_mentions_annos_j = dict()
    entity_relation_k_to_entity_relations_annos_j = dict()
    event_mention_k_to_event_mentions_annos_j = dict()
    event_arg_k_to_event_args_annos_j = dict()
    for sentence in json_doc["sentences"]:
        for mention in sentence["mentions"]:
            mention_key = generate_mention_key(json_doc["doc_id"], sentence["sent_no"], mention)
            mention_k_to_mentions_annos_j.setdefault(mention_key, list()).append(mention)
        for event_mention in sentence["event_mentions"]:
            event_mention_key = generate_event_mention_key(json_doc["doc_id"], sentence["sent_no"], event_mention)
            event_mention_k_to_event_mentions_annos_j.setdefault(event_mention_key, list()).append(event_mention)
    for sentence in json_doc["sentences"]:
        for event_mention in sentence["event_mentions"]:
            event_mention_key = generate_event_mention_key(json_doc["doc_id"], sentence["sent_no"], event_mention)
            for event_arg in event_mention["event_args"]:
                # right_mention_key = generate_mention_key(json_doc["doc_id"], right_sentence["sent_no"], right_mention)
                if event_arg["serif_type"] == "Mention":
                    right_mention_key = generate_mention_key(json_doc["doc_id"],
                                                             mention_id_to_sentence[event_arg["ref"]]["sent_no"],
                                                             mention_id_to_mention[event_arg["ref"]])
                elif event_arg["serif_type"] == "EventMention":
                    right_mention_key = generate_event_mention_key(json_doc["doc_id"],
                                                                   event_mention_id_to_sentence[event_arg["ref"]][
                                                                       "sent_no"],
                                                                   event_mention_id_to_event_mention[event_arg["ref"]])
                else:
                    raise ValueError("Unknown {}".format(event_arg["serif_type"]))
                event_arg_key = generate_event_argument_key(json_doc["doc_id"], event_mention_key, right_mention_key,
                                                            event_arg)
                event_arg_k_to_event_args_annos_j.setdefault(event_arg_key, list()).append(event_arg)
    for entity_relation in json_doc["entity_relations"]:
        left_mention = mention_id_to_mention[entity_relation["left_mention_id"]]
        left_sentence = mention_id_to_sentence[entity_relation["left_mention_id"]]
        right_mention = mention_id_to_mention[entity_relation["right_mention_id"]]
        right_sentence = mention_id_to_sentence[entity_relation["right_mention_id"]]
        left_mention_key = generate_mention_key(json_doc["doc_id"], left_sentence["sent_no"], left_mention)
        right_mention_key = generate_mention_key(json_doc["doc_id"], right_sentence["sent_no"], right_mention)
        entity_relation_key = generate_entity_relation_key(json_doc["doc_id"], left_mention_key, right_mention_key,
                                                           entity_relation)
        entity_relation_k_to_entity_relations_annos_j.setdefault(entity_relation_key, list()).append(entity_relation)
    return mention_k_to_mentions_annos_j, entity_relation_k_to_entity_relations_annos_j, event_mention_k_to_event_mentions_annos_j, event_arg_k_to_event_args_annos_j


def resolve_annotation(annotation_json_doc, extraction_json_doc):
    extraction_json_doc["annotation"] = annotation_json_doc["annotation"]
    # Use UI data for overriding annotation
    mention_id_to_mention_anno, mention_id_to_sentence_anno, event_mention_id_to_event_mention_anno, event_mention_id_to_sentence_anno = build_json_id_to_json_entries(
        annotation_json_doc)
    mention_k_to_mentions_annos_j, entity_relation_k_to_entity_relations_annos_j, event_mention_k_to_event_mentions_annos_j, event_arg_k_to_event_args_annos_j = build_customer_k_to_objects(
        annotation_json_doc, mention_id_to_mention_anno, mention_id_to_sentence_anno,
        event_mention_id_to_event_mention_anno, event_mention_id_to_sentence_anno)
    for mention_k, mentions in mention_k_to_mentions_annos_j.items():
        for mention in mentions:
            if mention["is_frozen"]:
                string_mention_k = "{}#{}".format("Mention", "#".join(str(i) for i in mention_k))
                extraction_json_doc["annotation"][string_mention_k] = mention["is_good"]
    for entity_relation_k, entity_relations in entity_relation_k_to_entity_relations_annos_j.items():
        for entity_relation in entity_relations:
            if entity_relation["is_frozen"]:
                string_entity_relation_k = "{}#{}".format("EntityRelation", "#".join(str(i) for i in entity_relation_k))
                extraction_json_doc["annotation"][string_entity_relation_k] = entity_relation["is_good"]
    for event_mention_k, event_mentions in event_mention_k_to_event_mentions_annos_j.items():
        for event_mention in event_mentions:
            if event_mention["is_frozen"]:
                string_event_mention_k = "{}#{}".format("EventMention", "#".join(str(i) for i in event_mention_k))
                extraction_json_doc["annotation"][string_event_mention_k] = event_mention["is_good"]
    for event_arg_k, event_args in event_arg_k_to_event_args_annos_j.items():
        for event_arg in event_args:
            if event_arg["is_frozen"]:
                string_event_arg_k = "{}#{}".format("EventArg", "#".join(str(i) for i in event_arg_k))
                extraction_json_doc["annotation"][string_event_arg_k] = event_arg["is_good"]

    # Use the entries to repopulate UI display plus generate statistics
    mention_id_to_mention_extract, mention_id_to_sentence_extract, event_mention_id_to_event_mention_extract, event_mention_id_to_sentence_extract = build_json_id_to_json_entries(
        extraction_json_doc)
    mention_k_to_mentions_extracts_j, entity_relation_k_to_entity_relations_extracts_j, event_mention_k_to_event_mentions_extracts_j, event_arg_k_to_event_args_extracts_j = build_customer_k_to_objects(
        extraction_json_doc, mention_id_to_mention_extract, mention_id_to_sentence_extract,
        event_mention_id_to_event_mention_extract, event_mention_id_to_sentence_extract)
    for mention_k, mentions in mention_k_to_mentions_extracts_j.items():
        string_mention_k = "{}#{}".format("Mention", "#".join(str(i) for i in mention_k))
        if string_mention_k in extraction_json_doc["annotation"]:
            for mention in mentions:
                mention["is_frozen"] = True
                mention["is_good"] = extraction_json_doc["annotation"][string_mention_k]
    for entity_relation_k, entity_relations in entity_relation_k_to_entity_relations_extracts_j.items():
        string_entity_relation_k = "{}#{}".format("EntityRelation", "#".join(str(i) for i in entity_relation_k))
        if string_entity_relation_k in extraction_json_doc["annotation"]:
            for mention in entity_relations:
                mention["is_frozen"] = True
                mention["is_good"] = extraction_json_doc["annotation"][string_entity_relation_k]
    for event_mention_k, event_mentions in event_mention_k_to_event_mentions_extracts_j.items():
        string_event_mention_k = "{}#{}".format("EventMention", "#".join(str(i) for i in event_mention_k))
        if string_event_mention_k in extraction_json_doc["annotation"]:
            for mention in event_mentions:
                mention["is_frozen"] = True
                mention["is_good"] = extraction_json_doc["annotation"][string_event_mention_k]
    for event_arg_k, event_args in event_arg_k_to_event_args_extracts_j.items():
        string_event_arg_k = "{}#{}".format("EventArg", "#".join(str(i) for i in event_arg_k))
        if string_event_arg_k in extraction_json_doc["annotation"]:
            for mention in event_args:
                mention["is_frozen"] = True
                mention["is_good"] = extraction_json_doc["annotation"][string_event_arg_k]


def create_app():
    logging.basicConfig(level=logging.getLevelName(os.environ.get('LOGLEVEL', 'INFO').upper()),
                        format='[%(asctime)s] {P%(process)d:%(module)s:%(lineno)d} %(levelname)s - %(message)s')

    flask_app = flask.Flask(__name__, static_folder=os.path.join(project_root, "serif/model/impl/a2t_adapter/statics"),
                            static_url_path='')
    flask_cors.CORS(flask_app)

    # default_text = "Billy Mays, the bearded, boisterous pitchman who, as the undisputed king of TV yell and sell, became an unlikely pop culture icon, died at his home in Tampa, Fla, on Sunday."
    # default_marked_text = "[[John Smith|Mention|PERSON]], a great man, [[died|EventMention|Death]] in [[Florida|Mention|LOCATION]]."
    default_original_text = "John Smith, a great man, died in Florida."
    default_original_text = "Billy Mays, the bearded, boisterous pitchman who, as the undisputed king of TV yell and sell, became an unlikely pop culture icon, died at his home in Tampa, Fla, on Sunday."
    default_extraction = dict()
    default_serifxml = ""
    default_doc_collection_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_train_sentences_all_ui_nlplingo_8.json"
    default_doc_collection = []

    if os.path.exists(default_doc_collection_path):
        with open(default_doc_collection_path) as fp:
            default_doc_collection = json.load(fp)

    @flask_app.after_request
    def add_header(r):
        """
        Add headers to both force latest IE rendering engine or Chrome Frame,
        and also to cache the rendered page for 10 minutes.
        """
        r.headers["Cache-Control"] = "public, no-store, no-cache, must-revalidate, post-check=0, pre-check=0, max-age=0"
        r.headers['Last-Modified'] = datetime.datetime.now()
        r.headers["Pragma"] = "no-cache"
        r.headers["Expires"] = "-1"
        r.headers['Cache-Control'] = 'public, max-age=0'
        return r

    # PySerif basic
    pyserif_basic_config_path = os.path.join(project_root, "config/config_a2t_basic_nlp.yml")
    with open(pyserif_basic_config_path) as fp:
        pyserif_basic_config = yaml.full_load(fp)
    pyserif_nlp_basic_pipeline = PySerifPipeline.from_config_dict(pyserif_basic_config)
    # Pyserif basic end

    # A2TModel
    a2t_default_config_path = os.path.join(project_root, "serif/model/impl/a2t_adapter/default_config.json")
    with open(a2t_default_config_path) as fp:
        a2t_default_config_dict = json.load(fp)
    a2t_driver = A2TDriver()

    @flask_app.before_first_request
    def flask_warmup():
        nonlocal a2t_driver
        nonlocal pyserif_nlp_basic_pipeline
        nonlocal default_original_text
        nonlocal default_extraction
        nonlocal default_serifxml
        logger.info("Loading PySerif basic models")
        pyserif_nlp_basic_pipeline.load_models()
        logger.info("Loading A2T models")
        a2t_driver.load_model()
        logger.info("Produce default extraction")
        a2t_driver.parse_template(a2t_default_config_dict)
        serif_docs = pyserif_nlp_basic_pipeline.process_txts({"doc_1": default_original_text}, lang="english")
        a2t_driver.process_document(serif_docs[0])
        final_serif_doc = serif_docs[0]

        default_serifxml = serifxml_to_string(final_serif_doc)

    @flask_app.route("/process_text", methods=["POST"])
    def process_text():
        original_text = flask.request.json.get("original_text")
        a2t_template_dict = flask.request.json.get("a2t_template", None)
        if a2t_template_dict is None:
            a2t_template_dict = a2t_default_config_dict
        a2t_driver.parse_template(a2t_template_dict)
        serif_docs = pyserif_nlp_basic_pipeline.process_txts({"doc_1": original_text}, lang="english")
        a2t_driver.process_document(serif_docs[0])
        return flask.jsonify(
            {"extraction": serif_json_seralizer(serif_docs[0]), "serif_doc": serifxml_to_string(serif_docs[0])}), 200

    @flask_app.route("/process_marked_text", methods=["POST"])
    def process_marked_text():
        marked_text = flask.request.json.get("marked_text")
        a2t_template_dict = flask.request.json.get("a2t_template", None)
        if a2t_template_dict is None:
            a2t_template_dict = a2t_default_config_dict
        a2t_driver.parse_template(a2t_template_dict)
        original_text, spans = convert_marked_text_into_plain_text_and_markings(marked_text)
        serif_docs = pyserif_nlp_basic_pipeline.process_txts({"doc_1": original_text}, lang="english")
        add_spans_into_serifxml(serif_docs[0], spans)
        a2t_driver.process_document(serif_docs[0])
        return flask.jsonify(
            {"extraction": serif_json_seralizer(serif_docs[0]), "serif_doc": serifxml_to_string(serif_docs[0])}), 200

    @flask_app.route("/process_serifxml", methods=["POST"])
    def process_serifxml():
        original_serifxml_f = flask.request.files.get("original_serifxml")
        original_a2t_template_dict = flask.request.files.get("a2t_template", None)
        if original_a2t_template_dict is not None:
            a2t_template_dict = json.loads(original_a2t_template_dict.read().decode("utf-8"))
        else:
            a2t_template_dict = a2t_default_config_dict
        serif_doc = Document(original_serifxml_f.read().decode("utf-8"))
        a2t_driver.parse_template(a2t_template_dict)
        a2t_driver.process_document(serif_doc)
        return flask.jsonify(
            {"extraction": serif_json_seralizer(serif_doc), "serif_doc": serifxml_to_string(serif_doc)}), 200

    @flask_app.route("/default_a2t_template", methods=["GET"])
    def get_default_a2t_template():
        return flask.jsonify(
            {"default_a2t_template": a2t_default_config_dict, "default_original_text": default_original_text,
             "default_extraction": default_extraction, "default_serif_doc": default_serifxml,"default_doc_collection": default_doc_collection}), 200

    @flask_app.route("/replace_default_a2t_template", methods=["POST"])
    def replace_default_a2t_template():
        nonlocal a2t_default_config_dict
        a2t_template_dict = flask.request.json.get("a2t_template", None)
        if a2t_template_dict != None:
            a2t_default_config_dict = a2t_template_dict
        return flask.jsonify({"status": "OK"}), 200

    @flask_app.route("/process_raw_text_basic_nlp", methods=["POST"])
    def process_raw_text_basic_nlp():
        original_text = flask.request.json.get("original_text")
        serif_docs = pyserif_nlp_basic_pipeline.process_txts({"doc_1": original_text}, lang="english")
        return flask.jsonify(
            {"extraction": serif_json_seralizer(serif_docs[0]), "serif_doc": serifxml_to_string(serif_docs[0])}), 200

    @flask_app.route("/process_markup_sentence_throgh_a2t", methods=["POST"])
    def process_markup_sentence_through_a2t():
        original_doc_dict = flask.request.json.get("original_doc_dict")
        original_serif_doc = flask.request.json.get("original_serif_doc")
        serif_doc = Document(original_serif_doc)
        modify_serifxml_from_unary_markings(serif_doc, original_doc_dict)
        a2t_template_dict = flask.request.json.get("a2t_template", None)
        if a2t_template_dict is None:
            a2t_template_dict = a2t_default_config_dict
        a2t_driver.parse_template(a2t_template_dict)
        a2t_driver.process_document(serif_doc)
        return flask.jsonify(
            {"extraction": serif_json_seralizer(serif_doc), "serif_doc": serifxml_to_string(serif_doc)}), 200

    @flask_app.route("/heuristic_te", methods=["GET"])
    def heuristic_te():
        pass

    @flask_app.route("/rescore_a2t_template", methods=["POST"])
    def rescore_using_latest_template():
        a2t_template_dict = flask.request.json.get("a2t_template", None)
        if a2t_template_dict is None:
            a2t_template_dict = a2t_default_config_dict
        adjudicated_extraction = flask.request.json.get("a2t_extraction", dict())
        unary_marking_extraction = flask.request.json.get("step_1_marking_extraction")
        step_1_serifxml = flask.request.json.get("step_1_serifxml")
        serif_doc = Document(step_1_serifxml)
        modify_serifxml_from_unary_markings(serif_doc, unary_marking_extraction)
        a2t_driver.parse_template(a2t_template_dict)
        a2t_driver.process_document(serif_doc)
        new_extraction = serif_json_seralizer(serif_doc)
        resolve_annotation(adjudicated_extraction, new_extraction)
        return flask.jsonify(
            {"extraction": new_extraction, "serif_doc": serifxml_to_string(serif_doc)}), 200

    @flask_app.route("/ping")
    def ping():
        return flask.jsonify({"msg":"pong"}), 200

    return flask_app


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=5007, required=False)
    args = parser.parse_args()
    flask_app = create_app()
    flask_app.run(host='::', port=args.port)
