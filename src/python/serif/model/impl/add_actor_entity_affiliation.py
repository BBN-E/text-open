from serif.model.document_model import DocumentModel
from serif.util.wikidata_affiliation import get_wikidata_affiliation


class ActorEntityAffiliationModel(DocumentModel):

    def __init__(self, query_cache_path=None, **kwargs):
        super(ActorEntityAffiliationModel, self).__init__(**kwargs)

        self.query_cache_path = query_cache_path

    def load_model(self):

        if self.query_cache_path is not None:
            with open(self.query_cache_path, "r") as f:
                self.query_cache = json.load(f)
        else:
            self.query_cache = {}

    def process_document(self, serif_doc):

        for actor_entity in serif_doc.actor_entity_set:
            if actor_entity.actor_type != "Q5":
                continue

            qnode = actor_entity.actor_db_name

            if qnode in self.query_cache:
                if self.query_cache[qnode] is not None:
                    actor_affiliation = self.query_cache[qnode]
                else:
                    actor_affiliation = "UNDET"
            else:
                actor_affiliation, actor_affiliation_label = get_wikidata_affiliation(qnode)
                self.query_cache[qnode] = actor_affiliation

            actor_entity.actor_affiliation = actor_affiliation

        return serif_doc
