import logging
from serif.model.document_model import DocumentModel
import time
import pickle
import random

logger = logging.getLogger(__name__)

HUMAN_LABEL = 'HumanLabeled'
class LearnItLabeledSampler(DocumentModel):

    def __init__(self, task, docid_to_relation_pattern_map, docid_to_na_map, **kwargs):
        super(LearnItLabeledSampler, self).__init__(**kwargs)
        self.task = task

        with open(docid_to_relation_pattern_map, 'rb') as f:
            self.docid_to_relation_pattern_map = pickle.load(f)

        with open(docid_to_na_map, 'rb') as f:
            self.docid_to_na_map = pickle.load(f)

        self.output_path = kwargs['argparse'].output_directory

    def process_document(self, serif_doc):
        start = time.time()
        logging.info('Start of sampling labeled LearnIt EERs')

        relation_pattern_instance_dict = {}
        docid = serif_doc.docid

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
                    eerm_set.add((serif_relation_mention, sentence))
        else:
            raise Exception('Only event and entity is supported.')

        # add LearnIt relations into a relation pattern to docid data structure
        final_learnit_relations = []
        negative_incomplete_list = []

        for serif_eerm in eerm_set or []:
            if self.task == 'event':
                eerm = serif_eerm
            elif self.task == 'entity':
                eerm = serif_eerm[0]

            if self.task == 'event':
                relation = eerm.relation_type
            elif self.task == 'entity':
                relation = eerm.type

            if relation == 'Negative_incomplete':
                negative_incomplete_list.append(serif_eerm)
                continue

            pattern = eerm.pattern
            if self.task == 'entity':
                if pattern is None:
                    continue

            if relation not in relation_pattern_instance_dict:
                relation_pattern_instance_dict[relation] = dict()

            if pattern not in relation_pattern_instance_dict[relation]:
                relation_pattern_instance_dict[relation][pattern] = []
            relation_pattern_instance_dict[relation][pattern].append(serif_eerm)


        for relation in relation_pattern_instance_dict:
            for pattern in relation_pattern_instance_dict[relation]:

                # always add human labeled examples
                if HUMAN_LABEL in pattern:
                    final_learnit_relations.extend(relation_pattern_instance_dict[relation][pattern])
                else:
                    if docid in self.docid_to_relation_pattern_map:
                        relation_to_pattern_count = self.docid_to_relation_pattern_map[docid]
                        if relation not in relation_to_pattern_count or pattern not in relation_to_pattern_count[relation]:
                            continue
                        else:
                            final_learnit_relations.extend(random.sample(relation_pattern_instance_dict[relation][pattern],
                                                                           relation_to_pattern_count[relation][pattern]))


        random.shuffle(negative_incomplete_list)

        if docid in self.docid_to_na_map:
            final_learnit_relations.extend(negative_incomplete_list[0:self.docid_to_na_map[docid]])

        # Write the final EER's into the appropriate document's EER set
        if self.task == 'event':
            eerm_set = serif_doc.add_new_event_event_relation_mention_set()  # kill the eerm_set

            for serif_eerm in final_learnit_relations:
                eerm_set.add_event_event_relation_mention(serif_eerm)
        elif self.task == 'entity':
            sentence_relation_mention_dict = dict()
            for sentence in serif_doc.sentences: # kill the relation mention set for each sentence
                rel_mention_set = sentence.add_new_relation_mention_set()
                sentence_relation_mention_dict[sentence] = rel_mention_set
            for serif_eerm in final_learnit_relations:
                sentence = serif_eerm[1]
                rel_mention_set = sentence_relation_mention_dict[sentence]
                serif_eerm[0].id = serif_eerm[0].document.generate_id(serif_eerm[0])
                rel_mention_set.add_relation_mention(serif_eerm[0])

        logging.info('End of sampling EERs')
        end = time.time()
        logging.info('Sampling EERs took %s seconds', end - start)

    def process_barrier(self, serif_doc_list):
        pass