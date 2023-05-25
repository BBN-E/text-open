from serif.model.event_mention_model import EventMentionModel


# Modified from DummyEventMentionModel
class EatEventMentionModel(EventMentionModel):
    def __init__(self, **kwargs):
        super(EatEventMentionModel, self).__init__(**kwargs)

    def add_event_mentions_to_sentence(self, sentence):
        # Create an EventMention whenever there is an FOOD
        # mentioned in the same sentence as a DOG
        event_mentions = []
        event_type = 'EAT'
        food_role = 'participant_food'
        dog_role = 'participant_dog'

        foods = [m for m in sentence.mention_set if m.entity_type == 'FOOD']
        dogs = [m for m in sentence.mention_set if m.entity_type == 'DOG']

        for food_mention in foods:
            if len(dogs) == 0:
                continue
            for dog_mention in dogs:
                anchor_node = food_mention.syn_node.head
                new_event_mentions = EventMentionModel.add_new_event_mention(sentence.event_mention_set, event_type,
                                                                             anchor_node.start_token,
                                                                             anchor_node.end_token,
                                                                             score=0.75)
                for em in new_event_mentions:
                    event_mentions.append(em)
                    EventMentionModel.add_new_event_mention_argument(em, food_role, food_mention, 1.0)
                    EventMentionModel.add_new_event_mention_argument(em, dog_role, dog_mention, 1.0)
        return event_mentions
