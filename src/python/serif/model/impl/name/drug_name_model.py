from serif.model.name_model import NameModel


class DrugNameModel(NameModel):
    def __init__(self, **kwargs):
        super(DrugNameModel, self).__init__(**kwargs)

    def add_names_to_sentence(self, sentence):
        ret = []
        for token in sentence.token_sequence:
            if ((token.text.lower() == "epinephrine" or
                 token.text.lower() == "norepinephrine" or
                 token.text.lower() == "amphetamine" or
                 token.text.lower() == "benzedrine")):
                ret.extend(self.add_or_update_name(sentence.name_theory, "DRUG", token, token))
        return ret
