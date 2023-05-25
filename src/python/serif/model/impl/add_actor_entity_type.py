from serif.model.document_model import DocumentModel
import json
import requests

WIKIDATA_SPARQL_URL = "https://query.wikidata.org/sparql"


class ActorEntityTypeModel(DocumentModel):

    def __init__(self, query_cache_path='/nfs/raid66/u15/aida/docs/ontologies/wikidata/type_qnodes.json', **kwargs):
        super(ActorEntityTypeModel, self).__init__(**kwargs)

        self.query_cache_path = query_cache_path

    def load_model(self):
        with open(self.query_cache_path, "r") as f:
            self.query_cache = json.load(f)

    def query_class_from_qnode(self, qnode):
        if qnode in self.query_cache:
            return self.query_cache[qnode][0]

        query_string = """
        SELECT ?prop_label ?prop_item WHERE {{
          VALUES (?qnode) {{
            (wd:{})
          }}
          ?qnode wdt:P31 ?prop_item.
          ?wd wikibase:directClaim wdt:P31.
          ?wd rdfs:label ?prop_label.
          FILTER((LANG(?prop_label)) = "en")
        }}"""

        query_string = query_string.format(qnode)
        res = requests.get(WIKIDATA_SPARQL_URL, params={"query": query_string, "format": "json"})
        if res.status_code != 200:
            type_qnode = 'UNDEF'
        else:
            res_json = res.json()
            try:
                type_qnode = res_json['results']['bindings'][0]['prop_item']['value'].rsplit('/', 1)[-1]
                # print("{}\t{}".format(qnode, type_qnode))
                self.query_cache[qnode] = [type_qnode]
            except IndexError:
                type_qnode = "UNDEF"

        return type_qnode

    def process_document(self, serif_doc):

        for actor_entity in serif_doc.actor_entity_set:
            actor_entity.actor_type = self.query_class_from_qnode(actor_entity.actor_db_name)

        return serif_doc
