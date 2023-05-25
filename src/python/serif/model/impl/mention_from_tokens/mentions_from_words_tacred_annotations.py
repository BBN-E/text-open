import os

from serif.model.mention_model import MentionModel


class MentionsFromWordsTACREDAnnotations(MentionModel):
    ''' Model for parsing TACRED corpus into Serifxml by first reading entity mentions as names and then converting them to entities
    '''
    def __init__(self, mapping_file, **kwargs):
        ''':param mapping_file: mapping word docs to their corresponding annotations docs'''

        super(MentionsFromWordsTACREDAnnotations,self).__init__(**kwargs)

        self.words2anno = self.load_words2anno_dict(mapping_file)
        self.anno_dict = self.load_anno_dict(self.words2anno)

        self.external_tag_file = True # to permit the model to accept annotations file as argument

    def load_words2anno_dict(self, mapping_file):
        '''
        :param mapping_file: tab-separated file of  "doc[.words] doc.annotations"  line for each doc to be processed
               TACRED relation annotations consist of 7 fields per line: "subj_start","subj_end","subj_type", "obj_start","obj_end","obj_type", "relation"
        :return: {"doc":"doc.annotations" for each doc}
        '''

        words2anno = dict()

        with open(mapping_file, 'r') as f:
            for l in f.readlines():
                words_file = l.strip().split()[0]
                tags_file = l.strip().split()[1]
                words2anno[os.path.basename(words_file)] = tags_file

        return words2anno

    def load_anno_dict(self, mapping_dict):
        '''
        :param mapping_dict: created by self.load_mapping_dict
        :return: {train_doc=":[["subj_start","subj_end","subj_type", "obj_start","obj_end","obj_type", "relation"],
                               ["subj_start","subj_end","subj_type", "obj_start","obj_end","obj_type", "relation"],
                                                                                                              ...]],     for each train, dev, test }
        '''
        anno_dict = dict()

        for words_file,anno_file in mapping_dict.items():
            anno_sents = self.preprocess_anno_file(anno_file)
            anno_dict[os.path.basename(words_file)] = anno_sents

        return anno_dict

    def preprocess_anno_file(self, anno_file):
        '''processes supplementary .annotations file into sents to provide as labelling info to entity indices, types and relations to doc'''
        anno_sents = [s.strip().split() for s in open(anno_file).readlines()]
        return anno_sents

    def add_mentions_to_sentence(self, sentence):
        new_mentions = []

        # get annotations corresponding to sentence in given doc
        annotations = self.anno_dict[sentence.document.docid][sentence.sent_no]
        [subj_start, subj_end, subj_type, obj_start, obj_end, obj_type, relation] = annotations

        new_mentions.extend(self.add_or_update_mention(sentence.mention_set, subj_type, "UNDET",
                                                       sentence.token_sequence[int(subj_start)],
                                                       sentence.token_sequence[int(subj_end)],
                                                       loose_synnode_constraint=True))

        new_mentions.extend(self.add_or_update_mention(sentence.mention_set, obj_type, "UNDET",
                                                       sentence.token_sequence[int(obj_start)],
                                                       sentence.token_sequence[int(obj_end)],
                                                       loose_synnode_constraint=True))

        return new_mentions
