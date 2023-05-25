import logging
from serif.model.dependency_model import DependencyModel

import trankit
import os,sys

logger = logging.getLogger(__name__)


class TrankitDependencyParser(DependencyModel):

    def __init__(self, lang, dir, **kwargs):
        super(TrankitDependencyParser, self).__init__(**kwargs)
        self.gpu = len(os.environ.get("CUDA_VISIBLE_DEVICES", "")) > 0
        if "use_data_from_trankit_adapter" in kwargs:
            self.nlp = None
        else:
            self.nlp = trankit.Pipeline(lang=lang, gpu=self.gpu, cache_dir=dir)

    def add_dependencies_to_sentence(self, sentence):
        trankit_sentence = None
        if self.nlp is None:
            try:
                trankit_sentence = sentence.trankit_sentence
            except AttributeError as e:
                logger.critical(
                    "If parameter use_data_from_trankit_adapter is used, the TrankitAdapter model must have been run")
                raise e
            self.apply_dependencies(sentence, trankit_sentence)
        else:
            # Get tokenized sentence to send through trankit universal dependency parser
            tokenized_sentence_text = ""
            serif_length = len(sentence.token_sequence)
            if serif_length < 1:
                return
            for token in sentence.token_sequence:
                if len(tokenized_sentence_text) != 0:
                    tokenized_sentence_text += " "
                tokenized_sentence_text += token.text
            trankit_sentence = self.nlp(tokenized_sentence_text).sentences[0]
            self.apply_dependencies(sentence, trankit_sentence)

    def apply_dependencies(self, serif_sentence, trankit_sentence):
        dep_set = serif_sentence.dependency_set

        parse = serif_sentence.parse
        if parse is None or parse.root is None:
            logger.warning("Skipping sentence {} {} due to no parse tree detected".format(serif_sentence.document.docid,
                                                                                          serif_sentence.sent_no))
            return
        token_to_index = self.get_token_to_index_dict(serif_sentence)
        trankit_sentence_dependencies = [{'head': t['head'], 'deprel': t['deprel'], 'text':t['text']} for t in trankit_sentence['tokens']]
        trankit_length = len(trankit_sentence_dependencies)
        serif_length = len(serif_sentence.token_sequence)

        if trankit_length != serif_length:
            logger.warning("Different sentence lengths, skipping {} {}".format(serif_sentence.document.docid,
                                                                               serif_sentence.sent_no))
            return

        # Go through dependencies, figure out which one are leaves --
        # leaves have nothing dependent on them
        is_leaf = dict()
        index = -1  # index into the sentence, 0-indexed
        for dep in trankit_sentence_dependencies:
            index += 1
            governor = dep['head']  # index into the sentence, 1-indexed
            if index not in is_leaf:
                is_leaf[index] = True
            is_leaf[governor - 1] = False

        # Create propositions from dependencies
        index_to_prop = dict()
        index = -1  # index into the sentence, 0-indexed
        for dep in trankit_sentence_dependencies:
            index += 1
            relation = dep['deprel']
            governor = dep['head']  # index into the sentence, 1-indexed
            token = dep['text']

            # This is the top node, typically, the relation is "root"
            # but sometimes it's <PAD>
            if governor == 0:
                continue

            # Make prop out of governor, if it doesn't exist already
            if governor - 1 not in index_to_prop:
                synnode = self.get_covering_preterm(parse.root, governor - 1, token_to_index)
                if not synnode:
                    logger.critical("Could not get covering preterm")
                    raise ValueError("Could not get covering preterm")
                governor_prop = dep_set.add_new_proposition("dependency", synnode)
                index_to_prop[governor - 1] = governor_prop
            governor_prop = index_to_prop[governor - 1]

            if is_leaf[index]:
                # Add to governor prop as syn node argument
                governor_prop.add_new_synnode_argument(relation,
                                                       self.get_covering_preterm(parse.root, index, token_to_index))
            else:
                # Add to governor prop as prop argument, may need to create prop here
                index_prop = None
                if index in index_to_prop:
                    index_prop = index_to_prop[index]
                else:
                    synnode = self.get_covering_preterm(parse.root, index, token_to_index)
                    if not synnode:
                        print("Could not get covering preterm")
                        raise ValueError("Could not get covering preterm")
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
