from serif.model.mention_model import MentionModel

class NounPhraseMentionModel(MentionModel):
    """Creates a Mention for each NP"""

    def __init__(self, **kwargs):
        super(NounPhraseMentionModel, self).__init__(**kwargs)

    def get_mention_info(self, sentence):
        tuples = []
        nodes = sentence.parse.get_nodes_matching_tags(["NP"])
        for node in nodes:
            tuples.append(
                ('UNDET', 'DESC', node))
        return tuples
