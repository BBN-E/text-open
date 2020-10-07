from serif.model.name_model import NameModel


class DrugNameModel(NameModel):
    def __init__(self,**kwargs):
        super(DrugNameModel,self).__init__(**kwargs)
    def get_name_info(self, sentence):
        drug_triples = []
        for token in sentence.token_sequence:
            if ((token.text.lower() == "epinephrine" or
                 token.text.lower() == "norepinephrine" or
                 token.text.lower() == "amphetamine" or
                 token.text.lower() == "benzedrine")):
                drug_triples.append(("DRUG", token, token))
        return drug_triples
