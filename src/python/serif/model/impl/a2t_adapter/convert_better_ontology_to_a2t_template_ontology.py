import json

BETTER_BASIC_EVENT_TYPES = {
    "Aid-Needs",
    "Apply-NPI",
    "Bribery",
    "Business-Event-or-SoA",
    "Change-of-Govt",
    "Close-Schools",
    "Communicate-Event",
    "Conduct-Diplomatic-Talks",
    "Conduct-Medical-Research",
    "Conduct-Meeting",
    "Conduct-Protest",
    "Conduct-Violent-Protest",
    "Coordinated-Comm",
    "Corruption",
    "Coup",
    "Cull-Livestock",
    "Death-from-Crisis-Event",
    "Declare-Emergency",
    "Disease-Exposes",
    "Disease-Infects",
    "Disease-Kills",
    "Disease-Outbreak",
    "Disease-Recovery",
    "Economic-Event-or-SoA",
    "Environmental-Event-or-SoA",
    "Evacuate",
    "Expel",
    "Financial-Crime",
    "Fiscal-or-Monetary-Action",
    "Hospitalize",
    "Illegal-Entry",
    "Impose-Quarantine",
    "Judicial-Acquit",
    "Judicial-Convict",
    "Judicial-Indict",
    "Judicial-Other",
    "Judicial-Plead",
    "Judicial-Prosecute",
    "Judicial-Seize",
    "Judicial-Sentence",
    "Kidnapping",
    "Law-Enforcement-Arrest",
    "Law-Enforcement-Investigate",
    "Law-Enforcement-Other",
    "Leave-Job",
    "Lift-Quarantine",
    "Loosen-Business-Restrictions",
    "Loosen-Travel-Restrictions",
    "Migrant-Detain",
    "Migrant-Relocation",
    "Migrant-Smuggling",
    "Migration-Blocked",
    "Military-Attack",
    "Military-Other",
    "Missing-from-Crisis-Event",
    "Monitor-Disease",
    "Natural-Phenomenon-Event-or-SoA",
    "Open-Schools",
    "Organize-Protest",
    "Other-Crime",
    "Other-Government-Action",
    "Political-Election-Event",
    "Political-Event-or-SoA",
    "Political-Other",
    "Provide-Aid",
    "Refugee-Movement",
    "Repair",
    "Require-PPE",
    "Rescue",
    "Restrict-Business",
    "Restrict-Travel",
    "Test-Patient",
    "Treat-Patient",
    "Vaccinate",
    "Violence",
    "Violence-Attack",
    "Violence-Bombing",
    "Violence-Damage",
    "Violence-Kill",
    "Violence-Other",
    "Violence-Set-Fire",
    "Violence-Wound",
    "War-Event-or-SoA",
    "Weather-or-Environmental-Damage",
    "Wounding-from-Crisis-Event"
}


def main():
    basic_json_barebone = {
        "stages_to_run": ["event_mention"],
        "entity_mention":{
            "input_constraints":[],
            "ontology":{}
        },
        "entity_mention_relation":{
            "input_constraints": [],
            "ontology": {}
        },
        "event_mention": {
            "input_constraints": [],
            "ontology": {}
        },
        "event_mention_argument": {
            "input_constraints": [],
            "ontology": {}
        }
    }
    for better_event_type in BETTER_BASIC_EVENT_TYPES:
        basic_json_barebone["event_mention"]["ontology"][better_event_type] = {
            "templates":[],
            "use_global_input_constraints": True
        }
    with open("/home/hqiu/tmp/zs4ie_better_event_empty.json",'w') as wfp:
        json.dump(basic_json_barebone, wfp, indent=4, sort_keys=True, ensure_ascii=False)

if __name__ == "__main__":
    main()