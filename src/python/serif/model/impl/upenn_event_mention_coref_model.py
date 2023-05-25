# Code ported from /nfs/raid88/u10/users/bmin-ad/Repo/UPennCoref/predict.py and clustering_no_pickle.py
# main function. Code from main distributed between constructor and process().

from serif.model.document_model import DocumentModel

from predict import *
import clustering_no_pickle as cnp

from misc.upenn_event_coref_utilities import *

import os
import random
import torch
import numpy as np


from transformers.tokenization_roberta import RobertaTokenizer

class UPennEventMentionCorefModel(DocumentModel):
    SEED = 42
    MAX_SEQUENCE_LENGTH = 128
    EVAL_BATCH_SIZE = 80

    def __init__(self, model_path, threshold, **kwargs):
        super(UPennEventMentionCorefModel, self).__init__(**kwargs)

        self.threshold = float(threshold)

        self.n_gpu = torch.cuda.device_count()

        random.seed(UPennEventMentionCorefModel.SEED)
        np.random.seed(UPennEventMentionCorefModel.SEED)
        torch.manual_seed(UPennEventMentionCorefModel.SEED)

        if self.n_gpu > 0:
            torch.cuda.manual_seed_all(UPennEventMentionCorefModel.SEED)

        self.processor = RteProcessor()
        self.output_mode = "classification"

        self.label_list = [0, 1]
        num_labels = len(self.label_list)

        self.model = RobertaForSequenceClassification(num_labels)
        self.tokenizer = RobertaTokenizer.from_pretrained(pretrain_model_dir, do_lower_case=True) # pretrain_model_dir imported from predict.py

        self.device = torch.device("cuda")
        self.model.to(self.device)

        self.model.load_state_dict(torch.load(model_path))
        self.model.to(self.device)

        self.max_test_acc = 0.0
        
    def get_test_examples_from_serif_doc(self, serif_doc, id_to_event_mention):
        docid = serif_doc.docid
        line_count = 0
        examples = []
        all_event_mentions = []

        for sentence in serif_doc.sentences:
            if sentence.event_mention_set is None:
                continue
            for event_mention in sentence.event_mention_set:
                all_event_mentions.append(event_mention)

        for em1 in all_event_mentions:
            em1_id = docid + "_" + em1.id
            id_to_event_mention[em1_id] = em1

            for em2 in all_event_mentions:
                if is_not_coref_pair(em1, em2):
                    continue
            
                
                em2_id = docid + "_" + em2.id

                em1_sentence_tokens = get_sentence_tokens(serif_doc, em1)
                em2_sentence_tokens = get_sentence_tokens(serif_doc, em2)

                guid = "test-" + str(line_count)
                label = 0

                examples.append(
                    InputExample(guid=guid, 
                                 text_a=em1_sentence_tokens, span_a_left=int(em1.semantic_phrase_start), span_a_right=int(em1.semantic_phrase_end), 
                                 text_b=em2_sentence_tokens, span_b_left=int(em2.semantic_phrase_start), span_b_right=int(em2.semantic_phrase_end),
                                 label=label, pair_id=em1_id + "&&" + em2_id))

                line_count += 1

        return examples
                    
    def create_clusters(self, threshold):
        '''
        Runs the inference procedure for both event and entity models calculates the B-cubed
        score of their predictions.
        '''
    
        cnp.clusters_count = 1
        topics_counter = 0
        epoch = 0 #
        all_event_mentions = []
        all_entity_mentions = []
    
        clusters = {}
        with torch.no_grad():
            for topic_id, topic in enumerate(cnp.topic_event_list.keys()):
    
                topics_counter += 1
    
                event_mentions = list(cnp.topic_event_list[topic])
                all_event_mentions.extend(event_mentions)
    
                topic_event_clusters = cnp.init_cd(event_mentions, is_event=True)
    
                cluster_pairs, _ = cnp.generate_cluster_pairs(topic_event_clusters, is_train=False)
                cnp.merge_cluster(topic_event_clusters, cluster_pairs, epoch, topics_counter, 0, threshold, True)
                cnp.set_coref_chain_to_mentions(topic_event_clusters, is_event=True, is_gold=True, intersect_with_gold=True)
                    
                for mention in all_event_mentions:
                    if cnp.coref_chain[mention] not in clusters:
                        clusters[cnp.coref_chain[mention]] = []
                    clusters[cnp.coref_chain[mention]].append(mention)
 
        return clusters

    def process_document(self, serif_doc):

        # Code taken from predict.py 
        id_to_event_mention = dict()
        test_examples = self.get_test_examples_from_serif_doc(serif_doc, id_to_event_mention)
        
        test_features = convert_examples_to_features(
            test_examples, self.label_list, UPennEventMentionCorefModel.MAX_SEQUENCE_LENGTH, self.tokenizer, self.output_mode,
            cls_token_at_end=False,  # bool(args.model_type in ['xlnet']),            # xlnet has a cls token at the end
            cls_token=self.tokenizer.cls_token,
            cls_token_segment_id=0,  # 2 if args.model_type in ['xlnet'] else 0,
            sep_token=self.tokenizer.sep_token,
            sep_token_extra=True,
            # bool(args.model_type in ['roberta']),           # roberta uses an extra separator b/w pairs of sentences, cf. github.com/pytorch/fairseq/commit/1684e166e3da03f5b600dbb7855cb98ddfcd0805
            pad_on_left=False,  # bool(args.model_type in ['xlnet']),                 # pad on the left for xlnet
            pad_token=self.tokenizer.convert_tokens_to_ids([self.tokenizer.pad_token])[0],
            pad_token_segment_id=0)  # 4 if args.model_type in ['xlnet'] else 0,)

        eval_all_pair_ids = [f.pair_id for f in test_features]

        eval_data, eval_sampler, test_dataloader = feature2vector(test_features, UPennEventMentionCorefModel.EVAL_BATCH_SIZE)

        logger.info("***** Running test *****")
        logger.info("  Num examples = %d", len(test_features))

        eval_loss = 0
        nb_eval_steps = 0
        preds = []
        gold_label_ids = []
        # print('Evaluating...')
        for input_ids, input_mask, segment_ids, span_a_mask, span_b_mask, label_ids in test_dataloader:

            input_ids = input_ids.to(self.device)
            input_mask = input_mask.to(self.device)
            segment_ids = segment_ids.to(self.device)
            span_a_mask = span_a_mask.to(self.device)
            span_b_mask = span_b_mask.to(self.device)

            label_ids = label_ids.to(self.device)
            gold_label_ids += list(label_ids.detach().cpu().numpy())

            with torch.no_grad():
                logits = self.model(input_ids, input_mask, span_a_mask, span_b_mask)
            if len(preds) == 0:
                preds.append(logits.detach().cpu().numpy())
            else:
                preds[0] = np.append(preds[0], logits.detach().cpu().numpy(), axis=0)

        preds = preds[0]
        pred_probs = softmax(preds, axis=1)
        score_for_print = list(pred_probs[:, 0])
        assert len(eval_all_pair_ids) == len(score_for_print)
        pred_label_ids = list(np.argmax(pred_probs, axis=1))

        gold_label_ids = gold_label_ids
        assert len(pred_label_ids) == len(gold_label_ids)
        hit_co = 0
        for k in range(len(pred_label_ids)):
            if pred_label_ids[k] == gold_label_ids[k]:
                hit_co += 1
        test_acc = hit_co / len(gold_label_ids)

        overlap = 0
        for k in range(len(pred_label_ids)):
            if pred_label_ids[k] == gold_label_ids[k] and gold_label_ids[k] == 1:
                overlap += 1
        recall = overlap / (1e-6 + sum(gold_label_ids))
        precision = overlap / (1e-6 + sum(pred_label_ids))
        f1 = 2 * recall * precision / (1e-6 + recall + precision)

        # this is test
        if f1 > self.max_test_acc:
            self.max_test_acc = f1

        coref_scores = []
        
        for id, score in enumerate(score_for_print):
            pair_idd = eval_all_pair_ids[id].split('&&')
            coref_scores.append((pair_idd[0], pair_idd[1], float(score)))

        # Code taken from clustering_no_pickle.py
        # entailment_score and topic_event_list in clustering_no_pickle.py
        cnp.topic_event_list = dict()
        for mention1, mention2, score in coref_scores:
            if mention1 not in cnp.entailment_score:
                cnp.entailment_score[mention1] = {}
            if mention2 not in cnp.entailment_score:
                cnp.entailment_score[mention2] = {}

            cnp.entailment_score[mention1][mention2] = float(score)
            cnp.entailment_score[mention2][mention1] = float(score)

            topic1 = mention1.split('_')[0] + "_" + id_to_event_mention[mention1].event_type
            if topic1 not in cnp.topic_event_list:
                cnp.topic_event_list[topic1] = set()
            cnp.topic_event_list[topic1].add(mention1)
            topic2 = mention2.split('_')[0] + "_" + id_to_event_mention[mention2].event_type
            if topic2 not in cnp.topic_event_list:
                cnp.topic_event_list[topic2] = set()
            cnp.topic_event_list[topic2].add(mention2)

        clusters = self.create_clusters(self.threshold)
        cluster_id = 0
        for cluster in clusters.values():
            for emid in cluster:
                id_to_event_mention[emid].cluster_id=cluster_id
            cluster_id += 1
        
        return serif_doc
