from serif.model.name_model import NameModel


class DogAndBreadNameModel(NameModel):
    def __init__(self, **kwargs):
        super(DogAndBreadNameModel, self).__init__(**kwargs)

    def add_names_to_sentence(self, sentence):
        ret = []
        for token in sentence.token_sequence:
            if token.text.lower() == "dog":
                ret.extend(self.add_or_update_name(sentence.name_theory, "DOG", token, token))
            if token.text.lower() == "bread":
                ret.extend(self.add_or_update_name(sentence.name_theory, "FOOD", token, token))
        return ret
