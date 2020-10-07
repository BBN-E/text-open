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

sent = ['Dog', 'eats', 'bread', '.']
parser = nltk.ChartParser(groucho_grammar)
for tree in parser.parse(sent):
    print(tree)