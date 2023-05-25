import serifxml3

from serif.model.document_model import DocumentModel

from serif.model.impl.round_tripper_util import find_matching_mention, find_matching_syn_node


# Propositions don't fit well into the usual pyserif infrastructure
# where we override a function that returns a list of tuples. This 
# is because Propositions can point to other Propositions, and at 
# the time we're creating the tuples, we haven't made the Propositions
# we want to point to. 
# 
# However, we can still access the lower-level serifxml API to create
# Proposition objects, and that's what this model does. 
#
class RoundTripperPropositionModel(DocumentModel):
    def __init__(self, input_serifxml_file, **kwargs):
        super(RoundTripperPropositionModel, self).__init__(**kwargs)
        self.serif_doc = serifxml3.Document(input_serifxml_file)

    @staticmethod
    def make_new_proposition_from_old(new_proposition_set, proposition, new_sentence):
        new_head_node = None
        if proposition.head is not None:
            new_head_node = find_matching_syn_node(
                proposition.head, new_sentence.parse)
        new_proposition = new_proposition_set.add_new_proposition(
            proposition.pred_type.value, new_head_node)

        if proposition.particle is not None:
            new_proposition.particle =\
                find_matching_syn_node(proposition.particle, new_sentence.parse)
        if proposition.adverb is not None:
            new_proposition.adverb =\
                find_matching_syn_node(proposition.adverb, new_sentence.parse)
        if proposition.negation is not None:
            new_proposition.negation =\
                find_matching_syn_node(proposition.negation, new_sentence.parse)
        if proposition.modal is not None:
            new_proposition.modal =\
                find_matching_syn_node(proposition.modal, new_sentence.parse)
        new_proposition.statuses = proposition.statuses
        return new_proposition

    # Overrides DocumentModel.process_document
    def process_document(self, document):
        for sentence in self.serif_doc.sentences:
            sent_no = sentence.sent_no
            new_sentence = document.sentences[sent_no]

            new_proposition_set = new_sentence.add_new_proposition_set(
                sentence.mention_set)
            if sentence.proposition_set is None:
                continue
                
            old_prop_to_new_prop = dict()
            for proposition in sentence.proposition_set:
                # Make new proposition matching old proposition
                # but don't include arguments yet
                new_proposition = RoundTripperPropositionModel.make_new_proposition_from_old(
                    new_proposition_set, proposition, new_sentence)
                old_prop_to_new_prop[proposition] = new_proposition

            # Do a second pass to create arguments, now that all props have been created
            for proposition in sentence.proposition_set:
                new_proposition = old_prop_to_new_prop[proposition]

                # Add arguments to new_proposition
                for argument in proposition.arguments:
                    # Mention argument
                    if argument.mention is not None:
                        new_mention = find_matching_mention(
                            argument.mention, new_sentence)
                        new_proposition.add_new_mention_argument(
                            argument.role, new_mention)
                        
                    # Proposition argument
                    elif argument.proposition is not None:
                        new_arg_prop = old_prop_to_new_prop[argument.proposition]
                        new_proposition.add_new_proposition_argument(
                            argument.role, new_arg_prop)

                    # SynNode argument
                    elif argument.syn_node is not None:
                        new_syn_node = find_matching_syn_node(
                            argument.syn_node, new_sentence.parse)
                        new_proposition.add_new_synnode_argument(
                            argument.role, new_syn_node)
                        
        return document
