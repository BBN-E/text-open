from serif.model.event_mention_model import EventMentionModel


# Modified from DummyEventMentionModel
class EatEventMentionModel(EventMentionModel):
    def __init__(self,**kwargs):
        super(EatEventMentionModel,self).__init__(**kwargs)

    def get_event_mention_info(self, sentence):
        # Create an EventMention whenever there is an FOOD
        # mentioned in the same sentence as a DOG
        tuples = []
        event_type = 'EAT'
        food_role = 'participant_food'
        dog_role = 'participant_dog'

        foods = [m for m in sentence.mention_set if m.entity_type == 'FOOD']
        dogs = [m for m in sentence.mention_set if m.entity_type == 'DOG']

        for food_mention in foods:
            if len(dogs) == 0:
                continue
            for dog_mention in dogs:
                food_argument_spec = (food_role, food_mention, 1.0)
                dog_argument_spec = (dog_role, dog_mention, 1.0)
                arg_specs = [food_argument_spec, dog_argument_spec]
                anchor_node = food_mention.syn_node.head
                event_mention_info = \
                    (event_type, anchor_node, 0.75, arg_specs)
                tuples.append(event_mention_info)
        return tuples
