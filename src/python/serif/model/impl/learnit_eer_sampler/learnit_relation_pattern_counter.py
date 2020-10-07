import os
import logging
from serif.model.base_model import BaseModel
import time
import pickle

logger = logging.getLogger(__name__)

class LearnItRelationPatternCounter(BaseModel):

    def __init__(self, task, **kwargs):
        super(LearnItRelationPatternCounter, self).__init__(**kwargs)
        self.task = task
        self.output_path = kwargs['argparse'].output_directory

    def process(self, serif_doc):
        pass

    def process_barrier(self, serif_doc_list):
        # Read in LearnIt EER's
        # and assign the corresponding relation, pattern a docid.
        # This table is later used for sampling.
        start = time.time()
        logging.info('Start of reading EERs')

        relation_pattern_instance_dict = {}
        for serif_doc_idx, serif_doc in enumerate(serif_doc_list):
            docid = serif_doc.docid

            eerm_set = set()
            if self.task == 'event':
                eer_set = serif_doc.event_event_relation_mention_set
                if eer_set is None:
                    eerm_set = set()
                else:
                    for serif_eerm in eer_set:
                        if serif_eerm.model == 'LearnIt':
                            eerm_set.add(serif_eerm)

            elif self.task == 'entity':
                for sentence in serif_doc.sentences:
                    for serif_relation_mention in sentence.rel_mention_set:
                        eerm_set.add(serif_relation_mention)
            else:
                raise Exception('Only event and entity relations are supported.')

            # add LearnIt event-event relation mentions into a data structure
            for serif_eerm in eerm_set or []:
                    if self.task == 'event':
                        relation = serif_eerm.relation_type
                    elif self.task == 'entity':
                        relation = serif_eerm.type

                    if relation == 'Negative_incomplete':
                        continue

                    pattern = serif_eerm.pattern
                    if self.task == 'entity':
                        if pattern is None:
                            continue

                    if relation not in relation_pattern_instance_dict:
                        relation_pattern_instance_dict[relation] = dict()

                    if pattern not in relation_pattern_instance_dict[relation]:
                        relation_pattern_instance_dict[relation][pattern] = []
                    relation_pattern_instance_dict[relation][pattern].append(docid)

        logging.info('Relation_pattern_set: %s', relation_pattern_instance_dict)
        logging.info('End of reading EERs')
        end = time.time()
        logging.info('Dumping EERs took %s seconds', end - start)

        os.makedirs(self.output_path, exist_ok=True)
        count_file = self.output_path + '/' + 'relation_pattern.count'
        with open(count_file, 'wb') as f:
            pickle.dump(relation_pattern_instance_dict, f)