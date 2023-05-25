import json


def main(old_json_path, new_json_path):
    with open(old_json_path) as fp:
        j = json.load(fp)
    # for task in {"entity_mention","entity_mention_relation","event_mention","event_mention_argument"}:
    #     for ontology_type, ontology_en in j[task]["ontology"].items():
    #         new_templates = []
    #         for template in ontology_en["templates"]:
    #             new_templates.append({"template":template,"threshold":0.5})
    #         ontology_en["templates"] = new_templates
    for ontology_type, ontology_en in j["event_mention"]["ontology"].items():
        ontology_en["input_constraints"] = []
        ontology_en["input_constraints"].append({
            "args": {
                "disallowed_words": [],
            },
            "name": "DisallowedWordEventFilter",
        })
    with open(new_json_path, 'w') as wfp:
        json.dump(j, wfp, indent=4, sort_keys=True, ensure_ascii=False)


if __name__ == "__main__":
    in_json_path = "/nfs/raid88/u10/users/hqiu_ad/repos/text-open/src/python/serif/model/impl/a2t_adapter/default_config.json"
    out_json_path = "/nfs/raid88/u10/users/hqiu_ad/repos/text-open/src/python/serif/model/impl/a2t_adapter/default_config.json"
    main(in_json_path, out_json_path)
