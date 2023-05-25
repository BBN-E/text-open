import serifxml3
import logging
import enum
from serif.model.actor_entity_model import ActorEntityModel
from serif.model.impl.round_tripper_util import find_matching_entity, find_matching_actor_mention
from serif.theory.sentence import Sentence
from collections import defaultdict, Counter

logger = logging.getLogger(__name__)


class ActorResolverMethod(enum.Enum):
    combine = enum.auto()
    multiple = enum.auto()
    majority_wins = enum.auto()


class SimpleActorEntityModel(ActorEntityModel):
    '''
    Assumes that entity coreference has already been run, which means every mention is governed by (at least one) entity;
    if actor mentions for a given entity exist, creates an actor entity to mirror the entity
    '''

    def __init__(self, method="multiple", **kwargs):
        super(ActorEntityModel, self).__init__(**kwargs)
        self.method = ActorResolverMethod[method]

    def add_actor_entities_to_document(self, serif_doc):
        ret = list()
        logger.info("adding actor entities to document {}".format(serif_doc.docid))
        mention_to_actor_mentions_map = self.build_mention_to_actor_mentions_map(serif_doc)

        for entity in serif_doc.entity_set:
            logger.info("processing Entity {}".format(entity.id))

            actor_mentions = []
            for m in entity.mentions:
                if m in mention_to_actor_mentions_map:
                    actor_mentions.extend(mention_to_actor_mentions_map[m])

            if len(actor_mentions) > 0:
                actor_entity_infos = self.resolve_actor_mentions_attributes(actor_mentions)
                for info in actor_entity_infos:
                    logger.info("creating ActorEntity with link \"{}\" for Entity {}".format(info["actor_entity_name"],
                                                                                             entity.id))
                    ret.extend(
                        ActorEntityModel.add_new_actor_entity(actor_entity_set=serif_doc.actor_entity_set,
                                                              entity=entity,
                                                              actor_uid=info["actor_entity_uid"],
                                                              actor_mentions=actor_mentions,
                                                              confidence=info["actor_entity_confidence"],
                                                              actor_name=info["actor_entity_name"],
                                                              # -----------
                                                              name=None,
                                                              actor_db_name=info.get("actor_entity_db_name", None),
                                                              source_note=info.get("actor_entity_source_note", None)))

            else:
                logger.info("no actor mentions found for mentions governed by Entity {}, skipping".format(entity.id))

        return ret

    def build_mention_to_actor_mentions_map(self, serif_doc):
        mention_to_actor_mentions_map = defaultdict(list)
        for s in serif_doc.sentences:
            for am in s.actor_mention_set:
                mention_to_actor_mentions_map[am.mention].append(am)
        return mention_to_actor_mentions_map

    def resolve_actor_mentions_attributes(self, actor_mentions):

        if self.method == ActorResolverMethod.combine:
            ret = [{"actor_entity_uid": -1,  # TODO figure out how to combine (uid must be int)
                    "actor_entity_confidence": sum(
                        [am.confidence if am.confidence is not None else 0.5 for am in actor_mentions])
                                               / len(actor_mentions),
                    "actor_entity_name": "|".join([str(am.actor_name) for am in actor_mentions])}]
            return ret

        elif self.method == ActorResolverMethod.multiple:  # merge only the actor mentions with identical links

            actor_id_to_avg_confidence = defaultdict(list)
            for actor_mention in actor_mentions:
                actor_entity_uid = actor_mention.actor_uid
                actor_entity_confidence = actor_mention.confidence if actor_mention.confidence is not None else 0.5
                actor_entity_name = actor_mention.actor_name
                actor_entity_db_name = actor_mention.actor_db_name
                actor_entity_source_note = actor_mention.source_note

                actor_id_to_avg_confidence[ \
                    (actor_entity_uid, actor_entity_name, actor_entity_db_name, actor_entity_source_note)].append(
                    actor_entity_confidence)

            ret = [{"actor_entity_uid": uid,
                    "actor_entity_confidence": sum(confs) / len(confs),
                    "actor_entity_name": name,
                    "actor_entity_db_name": db_name,
                    "actor_entity_source_note": source_note}
                   for (uid, name, db_name, source_note), confs in actor_id_to_avg_confidence.items()]

            return ret

        elif self.method == ActorResolverMethod.majority_wins:  # majority_wins

            db_name_to_actor_mention = {}

            actor_db_name_counts = Counter()
            for actor_mention in actor_mentions:
                actor_db_name = actor_mention.actor_db_name
                actor_db_name_counts[actor_db_name] += 1
                db_name_to_actor_mention[actor_db_name] = actor_mention

            (most_common_db_name, frequency) = actor_db_name_counts.most_common(1)[0]
            representative_actor_mention = db_name_to_actor_mention[most_common_db_name]

            total_count = sum(actor_db_name_counts.values())
            confidence = frequency / total_count

            ret = [{"actor_entity_uid": representative_actor_mention.actor_uid,
                    "actor_entity_confidence": confidence,
                    "actor_entity_name": representative_actor_mention.actor_name,
                    "actor_entity_db_name": representative_actor_mention.actor_db_name,
                    "actor_entity_source_note": representative_actor_mention.source_note}]

            return ret

        else:
            raise NotImplementedError
