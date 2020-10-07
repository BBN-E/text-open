from serif.model.name_model import NameModel


class InstitutionNameModel(NameModel):
    def __init__(self,**kwargs):
        super(InstitutionNameModel,self).__init__(**kwargs)

    def get_name_info(self, sentence):
        name_triples = []
        entity_type = 'ORG_institution'
        for i in range(len(sentence.token_sequence)):
            is_jhu = (
                    sentence.token_sequence[i].text.lower() == "johns" and
                    len(sentence.token_sequence) >= i + 3 and
                    sentence.token_sequence[i + 1].text.lower() == "hopkins" and
                    sentence.token_sequence[i + 2].text.lower() == "university")
            is_ama = (
                    sentence.token_sequence[i].text.lower() == "american" and
                    len(sentence.token_sequence) >= i + 3 and
                    sentence.token_sequence[i + 1].text.lower() == "medical" and
                    sentence.token_sequence[i + 2].text.lower() == "association")
            if is_jhu or is_ama:
                name_triples.append((entity_type,
                                     sentence.token_sequence[i],
                                     sentence.token_sequence[i + 2]))
        return name_triples
