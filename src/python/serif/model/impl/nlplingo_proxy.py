
import logging
from serif.model.document_model import DocumentModel
from nlplingo.annotation.ingestion import read_doc_annotation

from nlplingo.decoding.decoder import Decoder
from nlplingo.decoding.prediction_theory import DocumentPrediction, SentencePrediction, EventPrediction, \
    TriggerPrediction, ArgumentPrediction,EntityPrediction,ValueMentionPrediction,EntityMentionRelationPrediction,EventEventRelationPrediction,EntityCoreferencePrediction,EventCoreferencePrediction
from nlplingo.decoding.prediction_theory import nlplingo_doc_to_prediction_theory
from serif.model.impl.nlplingo_decoder import get_or_create_new_value_mention,get_or_create_new_entity_mention,get_or_create_new_event_mention,get_or_create_new_entity_mention_relation,get_or_create_new_event_mention_event_mention_relation
import serifxml3

logger = logging.getLogger(__name__)


class NLPLingoProxy(DocumentModel):

    def __init__(self, nlplingo_filelist, **kwargs):
        super(NLPLingoProxy, self).__init__(**kwargs)
        with open(nlplingo_filelist, 'r', encoding='utf-8') as f:
            filepaths = [line.strip() for line in f.readlines()]
        params = dict()
        params['add_serif_entity_mentions'] = False
        nlplingo_docs = read_doc_annotation(filepaths, params)
        self.doc_id_to_prediction_obj = dict()
        for lingo_doc in nlplingo_docs:
            for sentence in lingo_doc.sentences:
                lingo_doc.annotate_sentence_with_entity_mentions(sentence)
                lingo_doc.annotate_sentence_with_entity_relations(sentence)
                lingo_doc.annotate_sentence_with_events(sentence)
            doc_p = nlplingo_doc_to_prediction_theory(lingo_doc)
            self.doc_id_to_prediction_obj[lingo_doc.docid] = doc_p

    def process_document(self, serif_doc):
        docid = serif_doc.docid
        if docid in self.doc_id_to_prediction_obj:
            doc_p = self.doc_id_to_prediction_obj[docid]
            assert isinstance(doc_p,DocumentPrediction)
            sent_edt_off_to_sent = dict()
            event_mention_event_mention_relation_set = serif_doc.event_event_relation_mention_set
            if event_mention_event_mention_relation_set is None:
                event_mention_event_mention_relation_set = serif_doc.add_new_event_event_relation_mention_set()
            span_to_event_mention_event_mention_relations = dict()
            for serif_eerm in event_mention_event_mention_relation_set:
                relation_type = serif_eerm.relation_type
                serif_em_arg1 = None
                serif_em_arg2 = None
                for arg in serif_eerm.event_mention_relation_arguments:
                    if arg.role == "arg1":
                        serif_em_arg1 = arg.event_mention
                    if arg.role == "arg2":
                        serif_em_arg2 = arg.event_mention
                if serif_em_arg1 is not None and serif_em_arg2 is not None:
                    span_to_event_mention_event_mention_relations.setdefault((serif_em_arg1,serif_em_arg2),list()).append(serif_eerm)
            entity_set = serif_doc.entity_set
            if entity_set is None:
                entity_set = serif_doc.add_new_entity_set()
            existed_entity = set()
            for entity in entity_set:
                mentions = set()
                mentions.update(entity.mentions)
                existed_entity.add(frozenset(mentions))
            event_coref_set = serif_doc.event_set
            if event_coref_set is None:
                event_coref_set = serif_doc.add_new_event_set()
            existed_event_coref = set()
            for event in event_coref_set:
                event_mentions = set()
                event_mentions.update(event.event_mentions)
                existed_event_coref.add(frozenset(event_mentions))
            for st_index, sentence in enumerate(serif_doc.sentences):
                if len(sentence.token_sequence) > 0:
                    sent_edt_off_to_sent[
                        sentence.token_sequence[0].start_edt, sentence.token_sequence[-1].end_edt] = sentence
            # These are for general usage
            token_start_edt_to_token_all = dict()
            token_end_edt_to_token_all = dict()
            # End
            # These are for coreference usage
            token_span_to_added_value_mention = dict()
            token_span_to_added_entity_mention = dict()
            token_span_to_added_event_mention = dict()
            # End
            for sent_p in doc_p.sentences.values():
                assert isinstance(sent_p, SentencePrediction)
                sent_start_edt = sent_p.start
                sent_end_edt = sent_p.end - 1
                if (sent_start_edt, sent_end_edt) not in sent_edt_off_to_sent:
                    logger.critical("Missing sentence {} {}. Available {}".format(sent_start_edt,sent_end_edt,sent_edt_off_to_sent.keys()))
                    continue
                sentence = sent_edt_off_to_sent[sent_start_edt, sent_end_edt]
                assert isinstance(sentence, serifxml3.Sentence)
                value_mention_set = sentence.value_mention_set
                if value_mention_set is None:
                    value_mention_set = sentence.add_new_value_mention_set()
                mention_set = sentence.mention_set
                if mention_set is None:
                    mention_set = sentence.add_new_mention_set()
                event_mention_set = sentence.event_mention_set
                if event_mention_set is None:
                    event_mention_set = \
                        sentence.add_new_event_mention_set()
                    ''':type: EventMentionSet'''
                entity_mention_relation_set = sentence.rel_mention_set
                if entity_mention_relation_set is None:
                    entity_mention_relation_set = sentence.add_new_relation_mention_set()
                span_to_value_mentions = dict()
                span_to_mentions = dict()
                span_to_event_mentions = dict()
                span_to_entity_mention_relations = dict()
                for value_mention in value_mention_set:
                    span_to_value_mentions.setdefault((value_mention.start_token,value_mention.end_token),list()).append(value_mention)
                for mention in mention_set:
                    if mention.syn_node is not None:
                        span_to_mentions.setdefault(mention.syn_node,list()).append(mention)
                    if mention.start_token is not None and mention.end_token is not None:
                        span_to_mentions.setdefault((mention.start_token,mention.end_token),list()).append(mention)
                for entity_mention_relation in entity_mention_relation_set:
                    span_to_entity_mention_relations.setdefault((entity_mention_relation.left_mention,entity_mention_relation.right_mention),list()).append(entity_mention_relation)
                for event_mention in event_mention_set:
                    assert event_mention.semantic_phrase_start is not None and event_mention.semantic_phrase_end is not None
                    span_to_event_mentions[(sentence.token_sequence[event_mention.semantic_phrase_start],sentence.token_sequence[event_mention.semantic_phrase_end])] = event_mention
                token_start_edt_to_token = {token.start_edt: token for token in sentence.token_sequence}
                token_start_edt_to_token_all.update(token_start_edt_to_token)
                token_end_edt_to_token = {token.end_edt: token for token in sentence.token_sequence}
                token_end_edt_to_token_all.update(token_end_edt_to_token)
                for value_mention_p in sent_p.value_mentions.values():
                    assert isinstance(value_mention_p,ValueMentionPrediction)
                    start_char_edt,end_char_edt = value_mention_p.start,value_mention_p.end-1
                    assert len(value_mention_p.labels) == 1
                    label,score = list(value_mention_p.labels.items())[0]
                    start_token = token_start_edt_to_token.get(start_char_edt,None)
                    end_token = token_end_edt_to_token.get(end_char_edt,None)
                    if start_token is not None and end_token is not None:
                        new_value_mentions = get_or_create_new_value_mention(start_token,end_token,label,span_to_value_mentions)
                        token_span_to_added_value_mention.setdefault((start_token,end_token),list()).extend(new_value_mentions)
                    else:
                        logger.warning("Cannot align value_mention_p {} {} {}".format(start_char_edt,end_char_edt,label))
                for entity_p in sent_p.entities.values():
                    assert isinstance(entity_p,EntityPrediction)
                    start_char_edt, end_char_edt = entity_p.start, entity_p.end - 1
                    assert len(entity_p.entity_type_labels) == 1
                    entity_label, score = list(entity_p.entity_type_labels.items())[0]
                    mention_type_label = serifxml3.MentionType.none
                    if len(entity_p.mention_type_labels) > 0:
                        assert len(entity_p.mention_type_labels) == 1
                        mention_type_label,_ = list(entity_p.mention_type_labels.items())[0]
                        mention_type_label = serifxml3.MentionType(mention_type_label)
                    entity_subtype_label = "UNDEF" # Align with https://ami-gitlab-01.bbn.com/text-group/nlplingo/-/blob/c497e4ccffdb108e28a109468e277be02a6fc709/nlplingo/annotation/serif.py#L173
                    start_token = token_start_edt_to_token.get(start_char_edt,None)
                    end_token = token_end_edt_to_token.get(end_char_edt,None)

                    if start_token is not None and end_token is not None:
                        new_mentions = get_or_create_new_entity_mention(start_token,end_token,None,entity_label,entity_subtype_label,mention_type_label,span_to_mentions)
                        token_span_to_added_entity_mention.setdefault((start_token,end_token),list()).extend(new_mentions)
                    else:
                        logger.warning("Cannot align entity_p {} {} {}".format(start_char_edt,end_char_edt,entity_label))
                # Handle entity relation
                for entity_relation_p in sent_p.entity_mention_entity_mention_relations.values():
                    assert isinstance(entity_relation_p,EntityMentionRelationPrediction)
                    assert len(entity_relation_p.labels) == 1
                    label,score = list(entity_relation_p.labels.items())[0]
                    left_entity_mention_start_edt,left_entity_mention_end_edt = entity_relation_p.left_entity_mention_prediction.start, entity_relation_p.left_entity_mention_prediction.end - 1
                    right_entity_mention_start_edt,right_entity_mention_end_edt = entity_relation_p.right_entity_mention_prediction.start, entity_relation_p.right_entity_mention_prediction.end - 1
                    left_start_token,left_end_token = token_start_edt_to_token.get(left_entity_mention_start_edt,None),token_end_edt_to_token.get(left_entity_mention_end_edt,None)
                    right_start_token,right_end_token = token_start_edt_to_token.get(right_entity_mention_start_edt,None),token_end_edt_to_token.get(right_entity_mention_end_edt,None)
                    left_mentions = token_span_to_added_entity_mention.get((left_start_token,left_end_token),())
                    right_mentions = token_span_to_added_entity_mention.get((right_start_token,right_end_token),())
                    if len(left_mentions) < 1 or len(right_mentions) < 1:
                        logger.warning("Cannot align entity relation")
                    else:
                        for left_mention in left_mentions:
                            for right_mention in right_mentions:
                                new_entity_relation_mentions = get_or_create_new_entity_mention_relation(left_mention,right_mention,label,span_to_entity_mention_relations)
                # Handle Event. First pass, only deal with event
                for event_p in sent_p.events.values():
                    assert isinstance(event_p, EventPrediction)
                    trigger_p = event_p.trigger
                    assert isinstance(trigger_p,TriggerPrediction)
                    start_char_edt, end_char_edt = trigger_p.start, trigger_p.end - 1
                    assert len(trigger_p.labels) == 1
                    event_type,score = list(trigger_p.labels.items())[0]
                    start_token = token_start_edt_to_token.get(start_char_edt, None)
                    end_token = token_end_edt_to_token.get(end_char_edt, None)

                    if start_token is not None and end_token is not None:
                        new_event_mentions = get_or_create_new_event_mention(start_token, end_token, event_type,span_to_event_mentions)
                        token_span_to_added_event_mention.setdefault((start_token,end_token),list()).extend(new_event_mentions)
                    else:
                        logger.warning("Cannot align event_p {} {} {}".format(start_char_edt,end_char_edt,event_type))
                # Handle event. Second pass, only deal with event argument. Think about event mention can have argument of another event mention.
                for event_p in sent_p.events.values():
                    assert isinstance(event_p, EventPrediction)
                    trigger_p = event_p.trigger
                    assert isinstance(trigger_p, TriggerPrediction)
                    start_char_edt, end_char_edt = trigger_p.start, trigger_p.end - 1
                    start_token = token_start_edt_to_token.get(start_char_edt, None)
                    end_token = token_end_edt_to_token.get(end_char_edt, None)
                    if start_token is not None and end_token is not None:
                        added_event_mentions = span_to_event_mentions.get((start_token,end_token),list())
                        for event_mention in added_event_mentions:
                            for event_args_p in event_p.arguments.values():
                                assert isinstance(event_args_p,ArgumentPrediction)
                                assert len(event_args_p.labels) == 1
                                arg_role,arg_score = list(event_args_p.labels.items())[0]
                                arg_start_char_edt,arg_end_char_edt = event_args_p.start,event_args_p.end-1
                                arg_start_token = token_start_edt_to_token.get(arg_start_char_edt,None)
                                arg_end_token = token_end_edt_to_token.get(arg_end_char_edt,None)
                                candidates = list()
                                # First try to look at entity_mention_set
                                candidates.extend(token_span_to_added_entity_mention.get((arg_start_token,arg_end_token),()))
                                if len(candidates) < 1:
                                    candidates.extend(token_span_to_added_value_mention.get((arg_start_token,arg_end_token),()))
                                if len(candidates) < 1:
                                    candidates.extend(token_span_to_added_event_mention.get((arg_start_token,arg_end_token),()))
                                if len(candidates) < 1:
                                    logger.warning("Cannot resolve argument to existed theory {} {} {}".format(arg_role,arg_start_char_edt,arg_end_char_edt))

                                for candidate in candidates:
                                    event_mention_arg = event_mention.construct_event_mention_argument(arg_role, candidate, arg_score)
                                    event_mention.add_event_mention_argument(event_mention_arg)

            # Handle entity coreference
            for entity_coref_p in doc_p.entity_coreference.values():
                assert isinstance(entity_coref_p,EntityCoreferencePrediction)
                coref_mention_set = set()
                for entity_mention_p in entity_coref_p.entity_mentions:
                    entity_mention_start_edt, entity_mention_end_edt = entity_mention_p.start, entity_mention_p.end - 1
                    start_token, end_token = token_end_edt_to_token_all.get(entity_mention_start_edt,
                                                                                    None), token_end_edt_to_token_all.get(
                        entity_mention_end_edt, None)
                    mentions = token_span_to_added_entity_mention.get((start_token, end_token), ())
                    coref_mention_set.update(mentions)
                if frozenset(coref_mention_set) not in existed_entity and len(coref_mention_set) > 0:
                    represent_mention = list(coref_mention_set)[0]
                    entity_set.add_new_entity(list(coref_mention_set),represent_mention.entity_type,represent_mention.entity_subtype,False)
                    existed_entity.add(frozenset(coref_mention_set))

            # Handle event coreference
            for event_coref_p in doc_p.event_coreference.values():
                assert isinstance(event_coref_p,EventCoreferencePrediction)
                coref_event_set = set()
                for event_mention_p in event_coref_p.event_mentions:
                    event_mention_start_edt, event_mention_end_edt = event_mention_p.trigger.start,event_mention_p.trigger.end-1
                    start_token, end_token = token_end_edt_to_token_all.get(event_mention_start_edt,
                                                                            None), token_end_edt_to_token_all.get(
                        event_mention_end_edt, None)
                    event_mentions = token_span_to_added_event_mention.get((start_token,end_token),())
                    coref_event_set.update(event_mentions)
                if frozenset(coref_event_set) not in existed_event_coref and len(coref_event_set)> 0:
                    represent_event_mention = list(coref_event_set)[0]
                    event_coref_set.add_new_event(list(coref_event_set),represent_event_mention.event_type)
                    existed_event_coref.add(frozenset(coref_event_set))

            # Handle event mention event mention relations
            for event_event_relation_p in doc_p.event_event_relations.values():
                assert isinstance(event_event_relation_p,EventEventRelationPrediction)
                left_event_p = event_event_relation_p.left_event
                right_event_p = event_event_relation_p.right_event
                assert isinstance(left_event_p,EventPrediction)
                assert len(event_event_relation_p.labels) == 1
                label,score = list(event_event_relation_p.labels.items())[0]
                left_event_mention_start_edt,left_event_mention_end_edt = left_event_p.trigger.start, left_event_p.trigger.end - 1
                right_event_mention_start_edt,right_event_mention_end_edt = right_event_p.trigger.start, right_event_p.trigger.end - 1
                left_start_token,left_end_token = token_start_edt_to_token_all.get(left_event_mention_start_edt,None),token_end_edt_to_token_all.get(left_event_mention_end_edt,None)
                right_start_token,right_end_token = token_start_edt_to_token_all.get(right_event_mention_start_edt,None),token_end_edt_to_token_all.get(right_event_mention_end_edt,None)
                left_event_mentions = token_span_to_added_event_mention.get((left_start_token,left_end_token),())
                right_event_mentions = token_span_to_added_event_mention.get((right_start_token,right_end_token),())
                if len(left_event_mentions) < 1 or len(right_event_mentions) < 1:
                    logger.warning("Cnanot resolve event event relation")
                else:
                    for left_event_mention in left_event_mentions:
                        for right_event_mention in right_event_mentions:
                            new_event_relation_mentions = get_or_create_new_event_mention_event_mention_relation(left_event_mention,right_event_mention,label,span_to_event_mention_event_mention_relations)



