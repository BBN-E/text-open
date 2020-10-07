from serif.model.name_model import NameModel
import os

class AIDANameModel(NameModel):
    ''' Model for parsing training corpus into SerifXML with name annotations
    '''
    def __init__(self, mapping_file, **kwargs):
        ''':param mapping_file: mapping word docs to their corresponding BIO label docs'''

        super(AIDANameModel,self).__init__(**kwargs)

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


    def get_name_info(self, sentence):
        '''
        :param sentence: tokenized sentence as list
        :param BIO_tags: corresponding BIO tokens (same length)
        :return:
        '''

        # get BIO tags corresponding to sentence in given doc
        print(sentence.document.docid)
        print(self.bio_dict.keys())
        #BIO_tags = self.bio_dict[serif_doc_name][sent_index_in_doc]
        BIO_tags = self.bio_dict[sentence.document.docid][sentence.sent_no]

        type_start_end = []
        start_idx = 0
        end_idx = 0

        print("len sent", len(sentence.token_sequence))
        print(sentence.token_sequence)
        print("len bio", len(BIO_tags))
        print(BIO_tags)
        assert len(sentence.token_sequence) == len(BIO_tags)

        for i,token in enumerate(sentence.token_sequence):
            label = BIO_tags[i]

            if label.upper() == 'O': # not a name
                continue

            else:

                bi, type = label.split('-')

                # start of name span (sentence-internal)
                if bi.upper() == 'B' or \
                   (i > 0 and bi.upper() == 'I' and BIO_tags[i-1].upper() == 'O') or \
                   (i > 0 and bi.upper() == 'I' and BIO_tags[i-1].split('-')[1] != type): # beginning of name span
                    start_idx = i

                # end of namespan (end of sentence)
                if i == len(sentence.token_sequence)-1:
                    end_idx = i
                # end of namespan otherwise
                elif BIO_tags[i+1].upper().startswith('O') or \
                     BIO_tags[i+1].upper().startswith('B') or \
                     BIO_tags[i+1].upper().split('-')[1] != type: # end of namespan
                    end_idx = i
                else: # name-internal token (neither start nor end)
                    continue

                type_start_end.append((type, sentence.token_sequence[start_idx], sentence.token_sequence[end_idx]))

        return type_start_end