from serif.model.impl.dependency_parser.stanza_nlp_dependency_parser import StanzaDependencyParser

stanza_dependency_parser_ins = StanzaDependencyParser("unknown", "NON_EXISTED_PATH",
                                                      use_data_from_stanford_adapter=True)
stanza_dependency_parser_ins.load_model()


def stanza_depparse_adder(serif_sentence):
    stanza_dependency_parser_ins.add_dependencies_to_sentence(serif_sentence)
