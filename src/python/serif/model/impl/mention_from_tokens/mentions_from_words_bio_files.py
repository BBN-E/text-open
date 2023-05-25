import os

from serif.model.mention_model import MentionModel


class MentionsFromWordsBIOFiles(MentionModel):
    ''' Model for parsing training corpus into SerifXML with name annotations
    '''
    def __init__(self, mapping_file, **kwargs):
        ''':param mapping_file: mapping word docs to their corresponding BIO label docs'''

        super(MentionsFromWordsBIOFiles,self).__init__(**kwargs)

        self.words2tags = self.load_words2tags_dict(mapping_file)
        self.bio_dict = self.load_bio_dict(self.words2tags)

        self.external_tag_file = True # to permit the model to accept BIO tags file as argument

    def load_words2tags_dict(self, mapping_file):
        '''
        :param mapping_file: tab-separated file of  "doc.words  doc.tags"  line for each doc to be processed
        :return: {"doc.words":"doc.tags" for each doc}
        '''

        words2tags = dict()

        with open(mapping_file, 'r') as f:
            for l in f.readlines():
                print(l)
                words_file = l.strip().split()[0]
                tags_file = l.strip().split()[1]
                words2tags[os.path.basename(words_file)] = tags_file

        return words2tags

    def load_bio_dict(self, mapping_dict):
        '''
        :param mapping_dict: created by self.load_mapping_dict
        :return: {doc.words":[["B","I","O"],
                              ["I", "I"]
                              ...],         for each doc }
        '''
        bio_dict = dict()

        for words_file,tags_file in mapping_dict.items():
            bio_sents = self.preprocess_bio_file(tags_file)
            bio_dict[os.path.basename(words_file)] = bio_sents

        return bio_dict

    def preprocess_bio_file(self, bio_file):
        '''processes supplementary .tags bio file into sents to provide as labelling info to add_names_to_sentence'''
        bio_sents = [s.strip().split() for s in open(bio_file).readlines()]
        return bio_sents

    def add_mentions_to_sentence(self, sentence):
        new_mentions = []

        # get BIO tags corresponding to sentence in given doc
        BIO_tags = self.bio_dict[sentence.document.docid][sentence.sent_no]

        type_start_end = []
        start_idx = None
        end_idx = None

        assert len(sentence.token_sequence) == len(BIO_tags)

        for i,token in enumerate(sentence.token_sequence):

            label = BIO_tags[i]

            if label.upper() == 'O':  # not a name
                start_idx = None
                end_idx = None
                continue

            else:  # name annotation

                bi, type = label.split('-')

                # start of name span (sentence-internal)
                if bi.upper() == 'B': # beginning of name span
                    start_idx = i
                    end_idx = None

                # end of namespan (end of sentence)
                if start_idx is not None and i == len(sentence.token_sequence)-1:
                    end_idx = i
                # end of namespan otherwise
                elif (start_idx is not None and BIO_tags[i+1].upper().startswith('O')) or \
                     (start_idx is not None and BIO_tags[i+1].upper().startswith('B')): # end of namespan
                    end_idx = i
                else:  # name-internal token (neither start nor end)
                    continue

                if start_idx is not None and end_idx is not None:
                    print(sentence.token_sequence[start_idx].text, sentence.token_sequence[end_idx].text)
                    new_mentions.extend(self.add_or_update_mention(sentence.mention_set, type, "UNDET",
                                                                   sentence.token_sequence[start_idx],
                                                                   sentence.token_sequence[end_idx],
                                                                   loose_synnode_constraint=True))

                start_idx = None
                end_idx = None

        return new_mentions
