import serifxml3

from serif.model.dependency_model import DependencyModel
from serif.model.impl.round_tripper_util import find_matching_syn_node, find_matching_mention


class RoundTripperDependencyModel(DependencyModel):

    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperDependencyModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    def add_dependencies_to_sentence(self, sentence):
        # Get matching sentence from self.serif_doc
        serif_doc_sentence = self.serif_doc.sentences[sentence.sent_no]

        added_dependencies = []
        old_to_new_dependency_mapping = dict()
        for dependency in serif_doc_sentence.dependency_set or ():
            new_head_node = find_matching_syn_node(dependency.head, sentence.parse) if dependency.head else None
            new_particle = find_matching_syn_node(dependency.particle, sentence.parse) if dependency.particle else None
            new_adverb = find_matching_syn_node(dependency.adverb, sentence.parse) if dependency.adverb else None
            new_negation = find_matching_syn_node(dependency.negation, sentence.parse) if dependency.negation else None
            new_modal = find_matching_syn_node(dependency.modal, sentence.parse) if dependency.modal else None
            new_props = DependencyModel.add_new_proposition(sentence.dependency_set, dependency.pred_type.value,
                                                            new_head_node, particle=new_particle, adverb=new_adverb,
                                                            negation=new_negation, modal=new_modal,
                                                            statuses=dependency.statuses)
            old_to_new_dependency_mapping[dependency.id] = new_props
            added_dependencies.extend(new_props)

        # Do a second pass to create arguments, now that all props have been created
        for dependency in serif_doc_sentence.dependency_set or ():
            new_dependencies = old_to_new_dependency_mapping[dependency.id]
            for argument in dependency.arguments:
                # Mention argument
                if argument.mention is not None:
                    new_mention = find_matching_mention(
                        argument.mention, sentence)
                    for new_dependency in new_dependencies:
                        new_dependency.add_new_mention_argument(
                            argument.role, new_mention)

                # Proposition argument
                elif argument.proposition is not None:
                    new_arg_prop = old_to_new_dependency_mapping[argument.proposition.id][0]
                    for new_dependency in new_dependencies:
                        new_dependency.add_new_proposition_argument(
                            argument.role, new_arg_prop)

                # SynNode argument
                elif argument.syn_node is not None:
                    new_syn_node = find_matching_syn_node(
                        argument.syn_node, sentence.parse)
                    for new_dependency in new_dependencies:
                        new_dependency.add_new_synnode_argument(
                            argument.role, new_syn_node)

        return added_dependencies
