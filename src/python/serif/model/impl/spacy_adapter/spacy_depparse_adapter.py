import logging

from serif.model.dependency_model import DependencyModel

logger = logging.getLogger(__name__)


class SpacyDependencyModel(DependencyModel):
    def __init__(self, **kwargs):
        super(SpacyDependencyModel, self).__init__(**kwargs)
        self.add_heads = True

    def add_dependencies_to_sentence(self, sentence):

        parse = sentence.parse
        if parse is None or parse.root is None:
            logger.warning("Skipping sentence {} {} due to no parse tree detected".format(sentence.document.docid,
                                                                                          sentence.sent_no))
            return

        if sentence.dependency_set is None:
            sentence.add_new_dependency_set(sentence.mention_set)
        dep_set = sentence.dependency_set

        spacy_sentence = sentence.aux['spacy_sentence']
        assert len(sentence.token_sequence) == len(spacy_sentence)

        token_to_index = self.get_token_to_index_dict(sentence)

        # recursively create proposition sets
        self.add_children(parse.root, spacy_sentence.root, spacy_sentence, token_to_index, dep_set, parse.root)

    def add_children(self, governor_synnode, governor_spacy_node, spacy_sentence, token_to_index, dep_set, root_synnode):
        if len(list(governor_spacy_node.children)) <= 0:
            return governor_synnode

        governor_prop = dep_set.add_new_proposition("dependency", governor_synnode)

        for spacy_child_node in governor_spacy_node.children:
            index = spacy_child_node.i - spacy_sentence.start
            child_syn_node = self.get_covering_preterm(root_synnode, index, token_to_index)
            assert child_syn_node is not None
            argument = self.add_children(child_syn_node, spacy_child_node,
                                         spacy_sentence, token_to_index, dep_set, root_synnode)
            self.add_new_argument(governor_prop, spacy_child_node.dep_, argument)
        return governor_prop

    def get_token_to_index_dict(self, sentence):
        d = dict()
        index = 0
        for token in sentence.token_sequence:
            d[token] = index
            index += 1
        return d

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


def spacy_depparse_adder(serif_sentence):
    spacy_depparse_model = SpacyDependencyModel()
    if "spacy_sentence" not in serif_sentence.aux:
        logger.warning("Cannot find spacy_sentence for {}, skipping!!".format(serif_sentence.text))
        return

    spacy_depparse_model.add_dependencies_to_sentence(serif_sentence)
