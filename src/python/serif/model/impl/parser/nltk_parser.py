from serif.model.parser_model import ParserModel


class NLTKParser(ParserModel):
    def __init__(self, **kwargs):
        super(NLTKParser, self).__init__(**kwargs)

    def get_tokens(self, sentence):
        token_texts = []
        for token in sentence.token_sequence._children:
            token_texts.append(token.text)

        return token_texts

    def add_parse_to_sentence(self, sentence):

        token_texts = self.get_tokens(sentence)

        import nltk
        groucho_grammar = nltk.CFG.fromstring("""
         S -> NP VP PERIOD
         NP -> NNP | NN
         VP -> VBZ NP
         NNP -> 'Dog'
         VBZ -> 'eats'
         NN -> 'bread'
         PERIOD -> '.'
         """)

        parser = nltk.ChartParser(groucho_grammar)

        trees = parser.parse(token_texts)
        for tree in trees:
            treebank_string = str(tree).replace("PERIOD", ".")
            return self.add_new_parse(sentence, treebank_string)
        return []
