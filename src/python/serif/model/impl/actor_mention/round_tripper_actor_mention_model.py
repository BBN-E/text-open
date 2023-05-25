import serifxml3

from serif.model.actor_mention_model import ActorMentionModel

from serif.model.impl.round_tripper_util import find_matching_mention


class RoundTripperActorMentionModel(ActorMentionModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperActorMentionModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_actor_mentions_to_sentence(self, sentence):
        ret = list()
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]
        for actor_mention in serif_doc_sentence.actor_mention_set or ():
            new_mention = find_matching_mention(actor_mention.mention, sentence)
            ret.extend(ActorMentionModel.add_new_actor_mention(
                sentence.actor_mention_set,
                new_mention,
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

