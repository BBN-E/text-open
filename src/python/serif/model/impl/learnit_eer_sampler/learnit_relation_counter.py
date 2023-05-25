import dill
import os, json
import logging
import numpy as np
import collections
import serifxml3
from serif.model.corpus_model import CorpusModel
import time
import pickle

logger = logging.getLogger(__name__)

class LearnItRelationCounter(CorpusModel):

    def __init__(self, task, **kwargs):
        super(LearnItRelationCounter, self).__init__(**kwargs)
        self.task = task
        self.output_path = kwargs['argparse'].output_directory

    def process_documents(self, serif_doc_list):
        # Read in LearnIt EER's and write them into a list, paired with the docid
        start = time.time()
        logging.info('Start of counting relation EERs')

        relation_count_dict = {}

        for serif_doc_idx, serif_doc in enumerate(serif_doc_list):
            eerm_set = set()
            if self.task == 'event':
                eer_set = serif_doc.event_event_relation_mention_set
                if eer_set is None:
                    eerm_set = \
                        serif_doc.add_new_event_event_relation_mention_set()
                else:
                    for serif_eerm in eer_set:
                        if serif_eerm.model == 'LearnIt':
                            eerm_set.add(serif_eerm)
            elif self.task == 'entity':
                for sentence in serif_doc.sentences:
                    for serif_relation_mention in sentence.rel_mention_set:
                        eerm_set.add(serif_relation_mention)

            # add LearnIt relation mentions into a relation count data structure
            for serif_eerm in eerm_set or []:
                if self.task == 'event':
                    relation = serif_eerm.relation_type
                elif self.task == 'entity':
                    relation = serif_eerm.type

                if relation not in relation_count_dict:
                    relation_count_dict[relation] = 0

                relation_count_dict[relation] += 1

        logging.info('End of sampling EERs')
        end = time.time()
        logging.info('Dumping EERs took %s seconds', end - start)

        os.makedirs(self.output_path, exist_ok=True)
        count_file = self.output_path + '/' + 'relations.count'
        with open(count_file, 'wb') as f:
            pickle.dump(relation_count_dict, f)