from serif.model.entity_model import EntityModel


class SimpleEntityModel(EntityModel):
    def __init__(self,**kwargs):
        super(SimpleEntityModel,self).__init__(**kwargs)
    def get_entity_info(self, serif_doc):
        # Put any Mention with a type in its own Entity
        tuples = []
        for sentence in serif_doc.sentences:
            for mention in sentence.mention_set:
                if (mention.entity_type == 'OTH' or
                        mention.entity_type == 'UNDET'):
                    continue
                entity_subtype = 'UNDET'
                entity_info = (mention.entity_type, entity_subtype, [mention], True) # True dummy for is_generic attribute
                tuples.append(entity_info)
        return tuples
