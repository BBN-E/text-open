# In order to use this model
# download Miniconda Linux 64-bit https://docs.conda.io/en/latest/miniconda.html

# Run bash Miniconda3-latest-Linux-x86_64.sh
# Install to location with a lot of space
# when it asks: "Do you wish the installer to initialize Miniconda3 by running conda init? [yes|no]", say no

# source miniconda3/bin/activate
# conda create -n p3-cpu-torch
# conda activate p3-cpu-torch
# conda install python=3 pytorch-cpu
# pip install stanfordnlp

# Also may need to uncomment StanfordNLPDependencyParser line in sample_implementations.py

# Then you can run pipeline.py with the python command which points to a miniconda version of python with all the requirements

from serif.model.dependency_model import DependencyModel

import stanfordnlp
import sys

class StanfordNLPDependencyParser(DependencyModel):
    
    def __init__(self, lang, models_dir, **kwargs):
        super(StanfordNLPDependencyParser,self).__init__(**kwargs)
        if "use_data_from_stanford_adapter" in kwargs:
            self.nlp = None
        else:
            # For lang,models_dir, refer to https://stanfordnlp.github.io/stanfordnlp/models.html
            self.nlp = stanfordnlp.Pipeline(processors='tokenize,pos,depparse', lang=lang, tokenize_pretokenized=True, models_dir=models_dir, use_gpu=True)

    def add_dependencies_to_sentence(self, sentence):
        stanford_sentence = None
        if self.nlp is None:
            try: 
                stanford_sentence = sentence.stanford_sentence
            except AttributeError:
                print("If parameter use_data_from_stanford_adapter is used, the StanfordNLPAdapter model must have been run")
                sys.exit(1)
            self.apply_dependencies(sentence, stanford_sentence) 
        else:
            # Get tokenized sentence to send through stanfordnlp universal dependency parser
            tokenized_sentence_text = ""
            serif_length = len(sentence.token_sequence)
            if serif_length < 1:
                return
            for token in sentence.token_sequence:
                if len(tokenized_sentence_text) != 0:
                    tokenized_sentence_text += " "
                tokenized_sentence_text += token.text
            stanford_sentence = self.nlp(tokenized_sentence_text).sentences[0]
            self.apply_dependencies(sentence, stanford_sentence)

    def apply_dependencies(self, serif_sentence, stanford_sentence):
        dep_set = serif_sentence.add_new_dependency_set(serif_sentence.mention_set)

        parse = serif_sentence.parse
        token_to_index = self.get_token_to_index_dict(serif_sentence)
        stanford_length = len(stanford_sentence.dependencies)
        serif_length = len(serif_sentence.token_sequence)

        if stanford_length != serif_length:
            print("Different sentence lengths, skipping...")
            return
    
        # Go through dependencies, figure out which one are leaves --
        # leaves have nothing dependent on them
        is_leaf = dict()
        index = -1 # index into the sentence, 0-indexed
        for dep in stanford_sentence.dependencies:
            index += 1
            governor = dep[2].governor # index into the sentence, 1-indexed
            if index not in is_leaf:
                is_leaf[index] = True
            is_leaf[governor-1] = False
    
        # Create propositions from dependencies
        index_to_prop = dict()
        index = -1 # index into the sentence, 0-indexed
        for dep in stanford_sentence.dependencies:
            index += 1
            relation = dep[1]
            governor = dep[2].governor # index into the sentence, 1-indexed
            token = dep[2].text
            
            # This is the top node, typically, the relation is "root"
            # but sometimes it's <PAD>
            if governor == 0: 
                continue

            # Make prop out of governor, if it doesn't exist already
            if governor-1 not in index_to_prop:
                synnode = self.get_covering_preterm(parse.root, governor-1, token_to_index)
                if not synnode:
                    print("Could not get covering preterm")
                    sys.exit(1)
                governor_prop = dep_set.add_new_proposition("dependency", synnode)
                index_to_prop[governor-1] = governor_prop
            governor_prop = index_to_prop[governor-1]
    
            if is_leaf[index]:
                # Add to governor prop as syn node argument
                governor_prop.add_new_synnode_argument(relation, self.get_covering_preterm(parse.root, index, token_to_index))
            else:
                # Add to governor prop as prop argument, may need to create prop here
                index_prop = None
                if index in index_to_prop:
                    index_prop = index_to_prop[index]
                else:
                    synnode = self.get_covering_preterm(parse.root, index, token_to_index)
                    if not synnode:
                        print("Could not get covering preterm")
                        sys.exit(1)
                    index_prop = dep_set.add_new_proposition("dependency", synnode)
                    index_to_prop[index] = index_prop
                governor_prop.add_new_proposition_argument(relation, index_prop)

    # Helper functions
    def get_covering_preterm(self, synnode, index, token_to_index):
        if synnode.is_preterminal and index == token_to_index[synnode.start_token]:
            return synnode
    
        if index < token_to_index[synnode.start_token] or index > token_to_index[synnode.end_token]:
            return None

        for child in synnode:
            node = self.get_covering_preterm(child, index, token_to_index)
            if node:
                return node

        return None

    def get_token_to_index_dict(self, sentence):
        d = dict()
        index = 0
        for token in sentence.token_sequence:
            d[token] = index
            index += 1
        return d
