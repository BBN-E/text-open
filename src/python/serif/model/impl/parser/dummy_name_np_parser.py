import re
from serif.model.parser_model import ParserModel


class DummyNameNPParser(ParserModel):
    def __init__(self, **kwargs):
        super(ParserModel, self).__init__(**kwargs)
        self.add_heads = False
        #self.parser = benepar.Parser(model)

    def get_tokens(self, sentence):

        names = list(sentence.name_theory)
        names_tokens = []

        # get tokens corresponding to names [(Julius, Baer), (Jeffrey, White)]
        for name in names:

            name_tokens = []
            seen_name_start_token = False

            for token in sentence.token_sequence._children:

                # single-token name
                if token == name.start_token and token == name.end_token:
                    name_tokens.append(token)
                    names_tokens.append(name_tokens)
                    break

                # start of multi-token name
                elif token == name.start_token:
                    name_tokens.append(token)
                    seen_name_start_token = True
                    continue

                # end of multi-token name
                elif token == name.end_token:
                    name_tokens.append(token)
                    names_tokens.append(name_tokens)
                    break

                # intermediate token in multi-token name
                elif seen_name_start_token:
                    name_tokens.append(token)
                    continue

                # non-name token
                else:
                    continue


        token_partitions = []

        i = 0
        while i < len(sentence.token_sequence._children):

            curr_token = sentence.token_sequence._children[i]

            is_part_of_name = False
            name_index = -1

            for j,name_tokens in enumerate(names_tokens):
                for token in name_tokens:
                    if curr_token == token:
                        is_part_of_name = True
                        name_index = j

            if is_part_of_name:
                token_partitions.append(names_tokens[name_index])
                i += len(names_tokens[name_index])
            else:
                token_partitions.append(curr_token)
                i += 1

        print(token_partitions)
        return token_partitions

    # THIS DOESN'T WORK WHEN ORDER OF NAMES IN NAME_THEORY DOESN"T CORRESPOND TO ORDER OF TOKENS IN SENTENCE
    # def get_tokens(self, sentence):
    #
    #     token_partitions = []
    #     curr_name_tokens = []
    #     seen_name_start_token = False
    #     seen_name_end_token = True
    #
    #     names_yet_to_see = list(sentence.name_theory)
    #     print(names_yet_to_see)
    #     print(list(names_yet_to_see))
    #
    #     if sentence.name_theory:
    #         print("Has Name Theory")
    #
    #     for token in sentence.token_sequence._children:
    #         print("Token ", token.text)
    #         # check if current token is part of a name
    #         if not names_yet_to_see: # if we're done processing names in current sentence
    #             token_partitions.append(token)
    #
    #         else: # if there's still a possibility of encountering a name token
    #             for name in names_yet_to_see:
    #
    #                 # single-token name
    #                 if token == name.start_token and token == name.end_token:
    #                     token_partitions.append([token])
    #                     names_yet_to_see.remove(name)
    #                     #names_yet_to_see = names_yet_to_see[1:]
    #                     break
    #
    #                 # start of multi-token name
    #                 elif token == name.start_token:
    #                     seen_name_start_token = True
    #                     seen_name_end_token = False
    #                     curr_name_tokens.append(token)
    #                     break
    #
    #                 # end of multi-token name
    #                 elif token == name.end_token:
    #                     seen_name_end_token = True
    #                     seen_name_start_token = False
    #                     curr_name_tokens.append(token)
    #                     token_partitions.append(curr_name_tokens)
    #                     curr_name_tokens = []
    #                     names_yet_to_see.remove(name)
    #                     #names_yet_to_see = names_yet_to_see[1:]
    #                     break
    #
    #                 # intermediate token in multi-token name
    #                 elif seen_name_start_token and not seen_name_end_token:
    #                     curr_name_tokens.append(token)
    #                     break
    #
    #                 # plain ol' non-name token
    #                 else:
    #                     token_partitions.append(token)
    #                     break
    #
    #
    #     print(token_partitions)
    #     return token_partitions


    def parse(self, token_partition):
        '''
        :param token_pos: reflects name sequences in sent: e.g. "Vladimir Putin sneezes ." --> [[N, N], X, X]
        :return: parse tree reflecting the name structure
        '''

        parse = []

        for chunk in token_partition:

            if isinstance(chunk, list): # we're inside a name
                NP = []
                for token in chunk:
                    text = re.sub(r"\)", r"]", token.text)
                    text = re.sub(r"\(", r"[", text)
                    NP.append("(N {})".format(text))
                NP = "(NP {})".format(" ".join(NP))
                parse.append(NP)

            else: # plain old token
                token = chunk
                text = re.sub(r"\)", r"]", token.text)
                text = re.sub(r"\(", r"[", text)
                parse.append("(X {})".format(text))

        parse = "(S {})".format(" ".join(parse))

        return parse

    def get_parse_info(self, sentence):
        token_partitions = self.get_tokens(sentence)
        tree = self.parse(token_partitions)
        print(tree)
        return tree


# if __name__ == '__main__':
#
#     import re
#     text = "(sfsfdst)"
#     text = re.sub(r"\)", r"]", text)
#     text = re.sub(r"\(", r"[", text)
#     print(text)
