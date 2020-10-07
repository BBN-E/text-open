from serif.theory.actor_mention import ActorMention
from serif.theory.entity import Entity
from serif.theory.serif_theory import SerifTheory
from serif.xmlio import _ReferenceAttribute, _SimpleAttribute, _ReferenceListAttribute, _TextOfElement


class ActorEntity(SerifTheory):
    entity = _ReferenceAttribute('entity_id', cls=Entity)
    actor_uid = _SimpleAttribute(int)
    actor_mentions = _ReferenceListAttribute('actor_mention_ids', cls=ActorMention)
    confidence = _SimpleAttribute(float)
    name = _TextOfElement(strip=True)
    actor_name = _SimpleAttribute()
