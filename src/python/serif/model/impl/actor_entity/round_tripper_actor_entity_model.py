import serifxml3
from serif.model.actor_entity_model import ActorEntityModel
from serif.model.impl.round_tripper_util import find_matching_entity, find_matching_actor_mention
from serif.theory.sentence import Sentence


class RoundTripperActorEntityModel(ActorEntityModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(ActorEntityModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_actor_entities_to_document(self, document):
        ret = list()
        for actor_entity in self.serif_doc.actor_entity_set or ():
            list_of_new_actor_mentions = []
            for actor_mention in actor_entity.actor_mentions:
                sent_no = actor_mention.owner_with_type(Sentence).sent_no
                new_sentence = document.sentences[sent_no]
                new_actor_mention = find_matching_actor_mention(actor_mention, new_sentence)
                list_of_new_actor_mentions.append(new_actor_mention)
            new_entity = find_matching_entity(actor_entity.entity, document)
            ret.extend(ActorEntityModel.add_new_actor_entity(document.actor_entity_set, new_entity, actor_entity.actor_uid,
                                                             list_of_new_actor_mentions, actor_entity.confidence,
                                                             actor_entity.actor_name, name=actor_entity.name,
                                                             actor_db_name=actor_entity.actor_db_name))
        return ret
