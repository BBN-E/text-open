from serif.theory.enumerated_type import MentionType
from serif.theory.mention import Mention
from serif.theory.parse import Parse
from serif.theory.serif_sequence_theory import SerifSequenceTheory
from serif.xmlio import _SimpleAttribute, _ReferenceAttribute, _ChildTheoryElementList


class MentionSet(SerifSequenceTheory):
    name_score = _SimpleAttribute(float)
    desc_score = _SimpleAttribute(float)
    parse = _ReferenceAttribute('parse_id', cls=Parse)
    _children = _ChildTheoryElementList('Mention')

    def add_mention(self, mention):
        self._children.append(mention)

    def add_new_mention(self, syn_node, mention_type, entity_type):
        mention = self.construct_mention(syn_node, mention_type, entity_type)
        self.add_mention(mention)
        return mention

    def construct_mention(self, syn_node, mention_type, entity_type):
        mention = Mention(owner=self)
        mention.syn_node = syn_node
        mention.entity_type = entity_type
        umt = mention_type.upper()
        if umt == "NONE":
            mention.mention_type = MentionType.none
        elif umt == "NAME":
            mention.mention_type = MentionType.name
        elif umt == "PRON":
            mention.mention_type = MentionType.pron
        elif umt == "DESC":
            mention.mention_type = MentionType.desc
        elif umt == "PART":
            mention.mention_type = MentionType.part
        elif umt == "APPO":
            mention.mention_type = MentionType.appo
        elif umt == "LIST":
            mention.mention_type = MentionType.list
        elif umt == "NEST":
            mention.mention_type = MentionType.nest
        else:
            mention.mention_type = MentionType.none
        mention.document.generate_id(mention)
        return mention
