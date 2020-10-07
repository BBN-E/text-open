import os,sys,logging

import numpy as np

from serif.embeddings.get_pretrained_embeddings import get_pretrained_embeddings
import serifxml3

logger = logging.getLogger(__name__)

def create_doc_id_to_path(file_list, extension):
    docid_to_path = dict()
    with open(file_list) as fp:
        for i in fp:
            i = i.strip()
            docid = os.path.basename(i).replace(extension, "")
            docid_to_path[docid] = i
    return docid_to_path


def get_key_for_event_mention(event_mention: serifxml3.EventMention):
    doc = event_mention.owner_with_type(serifxml3.Document)
    return "{}:{}".format(doc.docid, event_mention.id)

def get_key_for_event_mention_arg(arg: serifxml3.EventMentionArg):
    doc = arg.owner_with_type(serifxml3.Document)
    return "{}:{}".format(doc.docid, arg.id)

class BertEmbCache(object):
    def __init__(self, doc_id_to_bert_npz_path):
        self.doc_id_to_bert_npz_path = doc_id_to_bert_npz_path
        self.doc_id_to_bert = dict()

    def get_bert_emb(
            self, docid, sent_id, token_id, *, start_index=0, end_index=3072):
        if docid not in self.doc_id_to_bert.keys():
            with np.load(self.doc_id_to_bert_npz_path[docid],
                         allow_pickle=True) as fp2:
                embeddings = fp2['embeddings']
                token_map = fp2['token_map']
                d = self.doc_id_to_bert.setdefault(docid, dict())
                d['embeddings'] = embeddings
                d['token_map'] = token_map
        token_map = self.doc_id_to_bert[docid]['token_map']
        embeddings = self.doc_id_to_bert[docid]['embeddings']
        if token_id is None:  # get bert embedding of whole sentence
            embedding = embeddings[sent_id][0]
        elif token_id == 'REDUCE_MEAN':
            embedding = np.mean(embeddings[sent_id], axis=0)
        else:
            head_token_idx_in_bert = token_map[sent_id][token_id]
            embedding = embeddings[sent_id][head_token_idx_in_bert]
        return embedding[start_index:end_index]

def get_name(arg: serifxml3.EventMentionArg,
             event_mention: serifxml3.EventMention):
    name = arg.value.text
    if name is None and hasattr(arg.value, 'head'):
        name = arg.value.head.text
    sentence = event_mention.owner_with_type(serifxml3.Sentence)
    assert isinstance(sentence, serifxml3.Sentence)
    for actor_mention in sentence.actor_mention_set or []:
        mention = actor_mention.mention
        if mention == arg.value:
            if actor_mention.actor_name:
                name = actor_mention.actor_name
            elif actor_mention.geo_text:
                name = actor_mention.geo_text
            elif actor_mention.paired_agent_name:
                name = actor_mention.paired_agent_name
            elif actor_mention.paired_actor_name:
                name = actor_mention.paired_actor_name
            break
    return name


class NlplingoEmbCache(object):
    def __init__(self,serif_list_path,nplinogo_npz_list_path,bert_list_path):
        self.em_to_serif_doc = dict()
        # self.em_to_feature_dict = dict()
        self.serif_list_path = serif_list_path
        self.nplinogo_npz_list_path = nplinogo_npz_list_path
        doc_id_to_bert_npz_path = create_doc_id_to_path(bert_list_path, '.npz')
        self.bert_emb_cache = BertEmbCache(doc_id_to_bert_npz_path)
        self.em_to_names = dict()
        self.em_to_embs = dict()
        self.em_args_to_embs = dict()

    def event_trigger_callback_builder(self):
        def event_mention_trigger_callback(serif_doc,
                                           event_mention,
                                           trigger_vecs):
            docid = serif_doc.docid
            self.em_to_serif_doc[event_mention] = serif_doc
            if len(trigger_vecs) > 0:
                # 1 dict per anchor
                for trigger_vec_dict in trigger_vecs:
                    for extractor_name in trigger_vec_dict.keys():
                        feature_embs = None
                        if extractor_name == "bert":
                            sent_id, token_id = (
                                trigger_vec_dict[extractor_name])
                            try:
                                feature_embs = (
                                    self.bert_emb_cache.get_bert_emb(
                                        docid, sent_id, token_id))
                            except KeyError as e:
                                logger.error(
                                    "Cannot find bert emb for {} {} {}"
                                    .format(docid, sent_id, token_id))
                                pass
                            except IndexError as e:
                                logger.error(
                                    "Cannot find bert emb for {} {} {}"
                                    .format(docid, sent_id, token_id))
                                pass
                        else:
                            feature_embs = trigger_vec_dict.get(
                                extractor_name, (None,)*3)[2]

                        if feature_embs is not None:
                            self.em_to_embs.setdefault(get_key_for_event_mention(event_mention), dict()).setdefault(extractor_name, list()).append(feature_embs)
        return event_mention_trigger_callback

    def event_argument_callback_builder(self):

        def event_mention_argument_callback(serif_doc,
                                            event_mention,
                                            trigger_vecs,
                                            argument,
                                            argument_vecs):
            docid = serif_doc.docid
            self.em_to_serif_doc[event_mention] = serif_doc
            if len(argument_vecs) > 0:
                # 1 dict per node/key associated with argument
                for argument_vec_dict in argument_vecs:
                    # store arg actor names even if extractor isn't used
                    name = get_name(argument, event_mention)
                    for extractor_name in argument_vec_dict:
                        key_getter_str = "arguments.{}".format(
                            extractor_name)
                        self.em_to_names.setdefault(
                            event_mention, dict()).setdefault(
                            key_getter_str, set()).add(name)
                    for extractor_name in argument_vec_dict.keys():
                        feature_embs = None
                        if extractor_name == "bert":
                            sent_id, token_id = (
                                argument_vec_dict[extractor_name])
                            try:
                                feature_embs = (
                                    self.bert_emb_cache.get_bert_emb(
                                        docid, sent_id, token_id))
                            except KeyError as e:
                                logger.error(
                                    "Cannot find bert emb for {} {} {}"
                                    .format(docid, sent_id, token_id))
                                pass
                            except IndexError as e:
                                logger.error(
                                    "Cannot find bert emb for {} {} {}"
                                    .format(docid, sent_id, token_id))
                                pass
                        else:
                            feature_embs = argument_vec_dict[extractor_name][4]

                        if feature_embs is not None:
                            self.em_args_to_embs.setdefault(get_key_for_event_mention_arg(argument), dict()).setdefault(extractor_name, list()).append(feature_embs)

        return event_mention_argument_callback

    def build(self):
        # The function call below is expensive and can be commented out if all
        # that is needed are sentence embeddings.
        get_pretrained_embeddings(
            self.serif_list_path,
            self.nplinogo_npz_list_path,
            self.event_trigger_callback_builder(),
            self.event_argument_callback_builder())
        return self

    def get_bert_emb_for_a_word(self,serif_token:serifxml3.Token):
        serif_doc = serif_token.owner_with_type(serifxml3.Document)
        doc_id = serif_doc.docid
        serif_sent = serif_token.owner_with_type(serifxml3.Sentence)
        sent_id = serif_sent.sent_no
        token_to_token_idx = {token:idx for idx,token in enumerate(serif_sent.token_sequence)}
        try:
            e = self.bert_emb_cache.get_bert_emb(doc_id, sent_id, token_to_token_idx[serif_token])
            return e
        except:
            logger.warning("Cannot find emb for token {} {} {}".format(doc_id,sent_id,token_to_token_idx[serif_token]))
            return None

    def get_bert_emb_for_a_sentence(self, serif_sentence:serifxml3.Sentence):
        serif_doc = serif_sentence.owner_with_type(serifxml3.Document)
        doc_id = serif_doc.docid
        sent_id = serif_sentence.sent_no
        try:
            e = self.bert_emb_cache.get_bert_emb(
                doc_id, sent_id, 'REDUCE_MEAN', start_index=768, end_index=1536)
            return e
        except:
            logger.warning("Cannot find emb for sentence {} {}".format(doc_id,sent_id))
            return None

    def get_bert_emb_for_an_event_trigger(self,serif_em:serifxml3.EventMention):
        return self.em_to_embs.get(get_key_for_event_mention(serif_em), dict()).get('bert', list())

    def get_bert_emb_for_an_event_argument(self,serif_em_arg:serifxml3.EventMentionArg):
        return self.em_args_to_embs.get(get_key_for_event_mention_arg(serif_em_arg), dict()).get('bert', list())

    def get_event_emb_dict(self,serif_em:serifxml3.EventMention):
        d = dict()
        for k,v in self.em_to_embs.get(get_key_for_event_mention(serif_em), dict()).items():
            if k != "bert":
                d[k] = v
        return d

    def get_argument_emb_dict(self,serif_em_arg:serifxml3.EventMentionArg):
        d = dict()
        for k,v in self.em_args_to_embs.get(get_key_for_event_mention_arg(serif_em_arg), dict()).items():
            if k != "bert":
                d[k] = v
        return d

def main():
    serif_list_path = "/home/hqiu/tmp/serif.list"
    bert_list_path = "/home/hqiu/tmp/bert.list"
    nplinogo_npz_list_path = "/home/hqiu/tmp/nlplingo_npz.list.new"

    nlplingo_emb_cache = NlplingoEmbCache(serif_list_path,nplinogo_npz_list_path,bert_list_path)
    nlplingo_emb_cache.build()
    serif_doc_sets = set(nlplingo_emb_cache.em_to_serif_doc.values())
    serif_ems = set(nlplingo_emb_cache.em_to_serif_doc.keys())

    for serif_doc in serif_doc_sets:
        assert isinstance(serif_doc,serifxml3.Document)
        docid = serif_doc.docid
        for sentence in serif_doc.sentences:
            assert isinstance(sentence,serifxml3.Sentence)
            sent_no = sentence.sent_no
            sentence_embeding = nlplingo_emb_cache.get_bert_emb_for_a_sentence(sentence)
            print("[SentenceEMB]\t{}\t{}\t{}".format(docid,sent_no,sentence_embeding))
            for serif_em in sentence.event_mention_set:
                assert isinstance(serif_em,serifxml3.EventMention)
                event_bert_embedings = nlplingo_emb_cache.get_bert_emb_for_an_event_trigger(serif_em)
                event_embeding = nlplingo_emb_cache.get_event_emb_dict(serif_em)
                for event_bert_embeding in event_bert_embedings:
                    print("[EventTriggerBert]\t{}\t{}\t{}\t{}".format(docid,sent_no,serif_em.id,event_bert_embeding))
                for extractor_name,embs in event_embeding.items():
                    for emb in embs:
                        print("[EventEmb]\t{}\t{}\t{}\t{}\t{}".format(docid,sent_no,serif_em.id,extractor_name,emb))
                for serif_em_arg in serif_em.arguments:
                    assert isinstance(serif_em_arg,serifxml3.EventMentionArg)
                    argument_id = serif_em_arg.id
                    argument_role = serif_em_arg.role
                    arg_bert_embedings = nlplingo_emb_cache.get_bert_emb_for_an_event_argument(serif_em_arg)
                    arg_embeding = nlplingo_emb_cache.get_argument_emb_dict(serif_em_arg)
                    for arg_bert_embeding in arg_bert_embedings:
                        print("[EventArgBert]\t{}\t{}\t{}\t{}\t{}".format(docid,sent_no,serif_em.id,argument_role,arg_bert_embeding))
                    for extractor_name,embs in arg_embeding.items():
                        for emb in embs:
                            print("[EventArgEmb]\t{}\t{}\t{}\t{}\t{}\t{}".format(docid,sent_no,serif_em.id,argument_role,extractor_name,emb))

if __name__ == "__main__":
    main()

