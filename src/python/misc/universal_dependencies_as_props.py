# To run:
#
# cp -r /d4m/home/bmin/stanfordnlp_resources into your home directory e.g. /d4m/home/azamania
# cp /nfs/raid87/u11/users/azamania/serifxml_api_save/rundir/output/sample_doc.txt.xml to home directory
# ssh azamania@magpie-p100-101
#
# Run with a command line like (except point to your git checkout of text-open)
#
# /usr/local/bin/singularity exec -B /nfs/raid66/u14/users/azamania/git/text-open/src/python:/tmp --nv /nfs/mercury-07/u26/bmin/repo/py3_stanfordnlp.img python3.7 /tmp/misc/universal_dependencies_as_props.py sample_doc.txt.xml sample_doc.dependencies.txt.xml
#
# Obviously, we need to reorganize a lot if we were to use this in a real system.
#

import sys, os

script_dir = os.path.dirname(os.path.realpath(__file__))
sys.path.append(os.path.join(script_dir, ".."))
import serifxml3
import stanfordnlp

def get_covering_preterm(synnode, index, token_to_index):
    #print ("Looking for " + str(index))
    #print ("start, end: " + str(token_to_index[synnode.start_token]) + " " + str(token_to_index[synnode.end_token]))
    if synnode.is_preterminal and index == token_to_index[synnode.start_token]:
        return synnode
    
    if index < token_to_index[synnode.start_token] or index > token_to_index[synnode.end_token]:
        return None

    for child in synnode:
        node = get_covering_preterm(child, index, token_to_index)
        if node:
            return node

    return None

def get_token_to_index_dict(sentence):
    d = dict()
    index = 0
    for token in sentence.token_sequence:
        d[token] = index
        index += 1
    return d

if len(sys.argv) != 3:
    print("Usage: " + sys.argv[0] + " input-serifxml-file output-serifxmlfile")
    sys.exit(1)

nlp = stanfordnlp.Pipeline(processors='tokenize,pos,depparse', lang='en', tokenize_pretokenized=True)

input_file, output_file = sys.argv[1:]
serif_doc = serifxml3.Document(input_file)
for sentence in serif_doc.sentences:
    parse = sentence.parse
    token_to_index = get_token_to_index_dict(sentence)
    
    # Get tokenized sentence to send through stanfordnlp universal dependency parser
    tokenized_sentence_text = ""
    serif_length = len(sentence.token_sequence)
    for token in sentence.token_sequence:
        if len(tokenized_sentence_text) != 0:
            tokenized_sentence_text += " "
        tokenized_sentence_text += token.text
    
    stanford_sentence = nlp(tokenized_sentence_text).sentences[0]
    stanford_length = len(stanford_sentence.dependencies)
    
    dep_set = sentence.add_new_dependency_set()

    if stanford_length != serif_length:
        print("Different sentence lengths, skipping...")
        continue

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
        
        if relation == "root":
            continue

        # Make prop out of governor, if it doesn't exist already
        if governor-1 not in index_to_prop:
            synnode = get_covering_preterm(parse.root, governor-1, token_to_index)
            if not synnode:
                print("Could not get covering preterm")
                sys.exit(1)
            governor_prop = dep_set.add_new_proposition("dependency", synnode)
            index_to_prop[governor-1] = governor_prop
        governor_prop = index_to_prop[governor-1]

        if is_leaf[index]:
            # Add to governor prop as syn node argument
            governor_prop.add_new_synnode_argument(relation, get_covering_preterm(parse.root, index, token_to_index))
        else:
            # Add to governor prop as prop argument, may need to create prop here
            index_prop = None
            if index in index_to_prop:
                index_prop = index_to_prop[index]
            else:
                synnode = get_covering_preterm(parse.root, index, token_to_index)
                if not synnode:
                    print("Could not get covering preterm")
                    sys.exit(1)
                index_prop = dep_set.add_new_proposition("dependency", synnode)
                index_to_prop[index] = index_prop
            governor_prop.add_new_proposition_argument(relation, index_prop)

serif_doc.save(output_file)

#doc = nlp("Barack Obama was born in Hawaii .")
#sent = doc.sentences[0]
#for deps in sent.dependencies:
#    relation = deps[1]
#    governor = deps[2].governor
#    token = deps[2].text
#    print(token + " " + relation + " " + str(governor))
#sent.print_dependencies()

