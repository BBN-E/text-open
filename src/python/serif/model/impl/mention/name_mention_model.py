from serif.model.mention_model import MentionModel

def get_np_covering_tokens(sentence, start_token, end_token):
    return sentence.parse.get_covering_syn_node(
        start_token, end_token, ['NP'])

def get_covering_np(sentence, name):
    return get_np_covering_tokens(sentence, name.start_token, name.end_token)

class NameMentionModel(MentionModel):
    """Makes Mentions for existing Names"""

    def __init__(self,**kwargs):
        super(NameMentionModel,self).__init__(**kwargs)

    def get_mention_info(self, sentence):
        tuples = []
        for name in sentence.name_theory:
            mention_type = "NAME"
            #if name.entity_type == "FOOD":
            #    mention_type = "DESC"
            syn_node = get_covering_np(sentence, name)
            if syn_node:
                tuples.append(
                    (name.entity_type, mention_type, syn_node))
            print("tuples",tuples)
        return tuples
