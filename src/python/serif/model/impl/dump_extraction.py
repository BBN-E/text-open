import os, json
import logging
import traceback
import serifxml3
from serif.model.base_model import BaseModel



logger = logging.getLogger(__name__)

def log_type_to_cnt(prefix,cnt_dict):
    for t,cnt in cnt_dict.items():
        logger.info("{}\t{}\t{}".format(prefix,t,cnt))

class DumpExtraction(BaseModel):

    def __init__(self,**kwargs):
        super(DumpExtraction, self).__init__(**kwargs)

    def process(self, serif_doc):
        try:
            event_mention_in_eer = set()

            event_type_to_cnt_in_eer = dict()
            event_types_to_cnt_in_eer = dict()
            factor_types_to_cnt_in_eer = dict()
            event_type_to_cnt_not_in_eer = dict()
            event_types_to_cnt_not_in_eer = dict()
            factor_types_to_cnt_not_in_eer = dict()

            eer_type_to_cnt = dict()
            arg_role_cnt = dict()
            eer_cnt = 0
            event_cnt = 0
            event_has_args = 0



            for serif_eerm in serif_doc.event_event_relation_mention_set or []:
                serif_em_arg1 = None
                serif_em_arg2 = None
                relation_type = serif_eerm.relation_type
                confidence = serif_eerm.confidence
                for arg in serif_eerm.event_mention_relation_arguments:
                    if arg.role == "arg1":
                        serif_em_arg1 = arg.event_mention
                    if arg.role == "arg2":
                        serif_em_arg2 = arg.event_mention
                if serif_em_arg1 is not None and serif_em_arg2 is not None:
                    event_mention_in_eer.add(serif_em_arg1)
                    event_mention_in_eer.add(serif_em_arg2)
                eer_type_to_cnt[relation_type] = eer_type_to_cnt.get(relation_type,0) + 1
                eer_cnt += 1
            for sent_idx, sentence in enumerate(serif_doc.sentences):
                sentence_theory = sentence.sentence_theory
                for event_mention in sentence_theory.event_mention_set:
                    event_cnt += 1
                    if event_mention in event_mention_in_eer:
                        event_type_to_cnt_in_eer[event_mention.event_type] = event_type_to_cnt_in_eer.get(event_mention.event_type, 0) + 1
                        for event_type in event_mention.event_types:
                            event_types_to_cnt_in_eer[event_type.event_type] = event_types_to_cnt_in_eer.get(event_type.event_type, 0) + 1
                        for event_type in event_mention.factor_types:
                            factor_types_to_cnt_in_eer[event_type.event_type] = factor_types_to_cnt_in_eer.get(event_type.event_type, 0) + 1
                    else:
                        event_type_to_cnt_not_in_eer[event_mention.event_type] = event_type_to_cnt_not_in_eer.get(event_mention.event_type, 0) + 1
                        for event_type in event_mention.event_types:
                            event_types_to_cnt_not_in_eer[event_type.event_type] = event_types_to_cnt_not_in_eer.get(event_type.event_type, 0) + 1
                        for event_type in event_mention.factor_types:
                            factor_types_to_cnt_not_in_eer[event_type.event_type] = factor_types_to_cnt_not_in_eer.get(event_type.event_type, 0) + 1
                    if len(event_mention.arguments):
                        event_has_args += 1
                    for argument in event_mention.arguments:
                        arg_role_cnt[argument.role] = arg_role_cnt.get(argument.role, 0) + 1

            logger.info("docid: {}\tnum_evts: {}\tnum_evts_in_eer: {}\tnum_evts_has_1_arg: {}\tnum_eers: {}".format(serif_doc.docid,event_cnt,len(event_mention_in_eer),event_has_args,eer_cnt))
            log_type_to_cnt("docid: {}\tin_eer_evt".format(serif_doc.docid),event_type_to_cnt_in_eer)
            log_type_to_cnt("docid: {}\tin_eer_evts".format(serif_doc.docid),event_types_to_cnt_in_eer)
            log_type_to_cnt("docid: {}\tin_eer_facs".format(serif_doc.docid),factor_types_to_cnt_in_eer)
            log_type_to_cnt("docid: {}\tno_in_eer_evt".format(serif_doc.docid),event_type_to_cnt_not_in_eer)
            log_type_to_cnt("docid: {}\tno_in_eer_evts".format(serif_doc.docid),event_types_to_cnt_not_in_eer)
            log_type_to_cnt("docid: {}\tno_in_eer_facs".format(serif_doc.docid),factor_types_to_cnt_not_in_eer)
            log_type_to_cnt("docid: {}\targ".format(serif_doc.docid),arg_role_cnt)
            log_type_to_cnt("docid: {}\teer".format(serif_doc.docid),eer_type_to_cnt)
        except Exception:
            logger.warning(traceback.format_exc())
