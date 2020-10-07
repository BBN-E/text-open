
class NameFinder(object):
    
    # return a list of start_token, end_token pairs
    @staticmethod
    def find_drug_name_tokens(sentence):
        drug_tokens = []
        for token in sentence.token_sequence:
            if ((token.text.lower() == "epinephrine" or
                 token.text.lower() == "norepinephrine" or
                 token.text.lower() == "amphetamine" or
                 token.text.lower() == "benzedrine")):
                drug_tokens.append((token, token))
        return drug_tokens

    @staticmethod
    def find_discovery_tokens(sentence):
        targets = ['discovered', 'identified', 'isolated', 'developed']
        discovery_tokens = []
        for token in sentence.token_sequence:
            if any(token.text.lower() == t for t in targets):
                discovery_tokens.append((token, token))
        return discovery_tokens
