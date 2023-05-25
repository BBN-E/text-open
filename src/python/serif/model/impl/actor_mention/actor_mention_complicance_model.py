

from serif.model.actor_mention_model import ActorMentionModel

class ActorMentionCompliance(ActorMentionModel):
    def __init__(self, **kwargs):
        super(ActorMentionCompliance, self).__init__(**kwargs)

    def add_actor_mentions_to_sentence(self, sentence):
        # backup previous results
        previous = list(sentence.actor_mention_set._children)
        sentence.actor_mention_set._children.clear()
        ret = list()
        for actor_mention in previous:
            ret.extend(ActorMentionModel.add_new_actor_mention(
                sentence.actor_mention_set,
                actor_mention.mention,
                actor_mention.actor_db_name,
                actor_mention.actor_uid,
                actor_mention.actor_name,
                actor_mention.source_note,
                sentence_theory=sentence.sentence_theory,
                actor_code=actor_mention.actor_code,
                actor_pattern_uid=actor_mention.actor_pattern_uid,
                is_acronym=actor_mention.is_acronym,
                requires_context=actor_mention.requires_context,
                pattern_confidence_score=actor_mention.pattern_confidence_score,
                importance_score=actor_mention.importance_score,
                paired_actor_uid=actor_mention.paired_actor_uid,
                paired_actor_code=actor_mention.paired_actor_code,
                paired_actor_pattern_uid=actor_mention.paired_actor_pattern_uid,
                paired_actor_name=actor_mention.paired_actor_name,
                paired_agent_uid=actor_mention.paired_agent_uid,
                paired_agent_code=actor_mention.paired_agent_code,
                paired_agent_pattern_uid=actor_mention.paired_agent_pattern_uid,
                paired_agent_name=actor_mention.paired_agent_name,
                actor_agent_pattern=actor_mention.actor_agent_pattern,
                geo_country=actor_mention.geo_country,
                geo_latitude=actor_mention.geo_latitude,
                geo_longitude=actor_mention.geo_longitude,
                geo_uid=actor_mention.geo_uid,
                geo_text=actor_mention.geo_text,
                country_id=actor_mention.country_id,
                iso_code=actor_mention.iso_code,
                country_info_actor_id=actor_mention.country_info_actor_id,
                country_info_actor_code=actor_mention.country_info_actor_code,
                pattern_match_score=actor_mention.pattern_match_score,
                association_score=actor_mention.association_score,
                edit_distance_score=actor_mention.edit_distance_score,
                georesolution_score=actor_mention.georesolution_score,
                confidence=actor_mention.confidence,
                name=actor_mention.name
            ))
        return ret