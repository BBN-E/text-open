from serif.model.name_model import NameModel
class DogAndBreadNameModel(NameModel):
    def __init__(self,**kwargs):
        super(DogAndBreadNameModel,self).__init__(**kwargs)

    def get_name_info(self, sentence):
        drug_triples = []
        for token in sentence.token_sequence:
            if token.text.lower() == "dog":
                drug_triples.append(("DOG", token, token))
            if token.text.lower() == "bread":
                drug_triples.append(("FOOD", token, token))
        return drug_triples
