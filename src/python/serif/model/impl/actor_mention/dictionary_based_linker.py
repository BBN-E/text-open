from collections import defaultdict
import enum
import logging
import json
import heapq
import spacy
from spacy.matcher import Matcher
from serif.model.actor_mention_model import ActorMentionModel


logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

MY_SPECIAL_UNIQUE_DELIMITER = "|*_^|"
BLOCK_ENTITY_TYPES = {"ORDINAL", "CARDINAL", "DATE", "TIME", "PERCENT", "MONEY", "QUANTITY"}

# Specifies the file format of the dictionary used for linking
class DictionaryFileFormat(enum.Enum):
    """
    frequency: Each line has an alias, the title of the wiki article that alias most often links to,
               the frequency to which that alias maps to its most common link, and the article's qnode.
    ex: The Triangle  The Triangle (newspaper)	0.8181818181818182	Q7770173
    e.g. /nfs/raid83/u13/caml/users/mselvagg_ad/wiki_extract/qnode_dictionary_table.txt
    """
    frequency = enum.auto()
    """
    qid: Each line has a qnode, canonical name, and then a list of aliases
    ex: Q144	dog	domestic dog	Canis lupus familiaris	Canis familiaris	dogs
    e.g. /nfs/raid66/u15/aida/releases/entity_linking_files/wikilinks.aida_canonical_names_all.txt
    """
    qid = enum.auto()
    """
    multiple: Each line has an alias, followed by a list of canonical names that can map to that alias
    ex: City Hall	City Hall	New York City Hall	Philadelphia City Hall	Toronto City Hall
    e.g. /home/hqiu/tmp/processed_geoname_table.txt
    """
    multiple = enum.auto()


# Specifies the file format of PageRank information, if PageRank is being used
class PageRankFileFormat(enum.Enum):
    """
    json: Specifies that pagerank_scores_fp points to a file that contains a dictionary mapping wikipedia article
          titles to PageRank scores.
    e.g. /nfs/raid90/u10/users/brozonoy-ad/data/wikipedia_pagerank/pagerank.json
    """
    json = enum.auto()
    """
    merged: Specifies that PageRank information is merged into the same file that name_dict_path points to. 
            This is only compatible with DictionaryFileFormat.qid. 
            Each line of input has qnode, canonical name, PageRank score, and then a list of aliases
    ex: Q144	dog	58.00255810279971	domestic dog	Canis lupus familiaris	Canis familiaris	dogs
    e.g. /nfs/raid66/u15/aida/releases/entity_linking_files/wikilinks.aida_canonical_names_all.v6.txt
    """
    merged = enum.auto()


class DictionaryBasedLinker(ActorMentionModel):

    def __init__(self,
                 lang='en',
                 spacy_model_path='/nfs/raid87/u10/nlp/spacy_3_0_6/en_core_web_sm',
                 name_dict_path='/home/hqiu/tmp/processed_geoname_table.txt',
                 link_name_mentions_only=False,
                 mode='multiple',
                 use_pagerank_scores=False,
                 pagerank_file_format='json',
                 pagerank_scores_fp="/nfs/raid90/u10/users/brozonoy-ad/data/wikipedia_pagerank/pagerank.json",
                 top_k=1,
                 source_note="geonames",  # wikidata
                 **kwargs):

        super(DictionaryBasedLinker, self).__init__(**kwargs)
        self.lang = lang
        self.spacy_model_path = spacy_model_path
        self.name_dict_path = name_dict_path
        self.link_name_mentions_only = link_name_mentions_only
        self.mode = DictionaryFileFormat[mode]
        self.pagerank_mode=PageRankFileFormat[pagerank_file_format]
        self.use_pagerank_scores = use_pagerank_scores
        self.pagerank_scores_fp = pagerank_scores_fp
        self.pagerank_scores = dict()
        self.top_k = int(top_k)
        self.source_note = source_note

    def load_model(self):
        self.name_dict = self.load_name_dict(name_dict_path=self.name_dict_path)
        self.nlp = spacy.load(self.spacy_model_path)
        self.matcher = Matcher(self.nlp.vocab)
        self.convert_name_dict_to_spacy_patterns(name_dict=self.name_dict, matcher=self.matcher)
        if self.use_pagerank_scores:
            if self.pagerank_mode == PageRankFileFormat.json:
                with open(self.pagerank_scores_fp, 'r') as f:
                    self.pagerank_scores = json.load(f)
            elif self.pagerank_mode == PageRankFileFormat.merged:
                with open(self.name_dict_path, 'r') as f:
                    lines = [l.strip().split('\t') for l in f.readlines()]
                    for entry in lines:
                        self.pagerank_scores[entry[0]] = float(entry[2])
            else:
                raise NotImplementedError

    def unload_model(self):
        del self.name_dict
        del self.nlp
        del self.matcher
        self.name_dict = None
        self.nlp = None
        self.matcher = None

    def load_name_dict(self, name_dict_path):

        name_dict = defaultdict(list)

        with open(name_dict_path, 'r') as f:
            lines = [l.strip().split('\t') for l in f.readlines()]

            if self.mode == DictionaryFileFormat.frequency:

                for entry in lines:
                    if len(entry) < 4:
                        continue
                    else:
                        name_dict[MY_SPECIAL_UNIQUE_DELIMITER.join([entry[3], entry[1]])].append(entry[0])

            elif self.mode == DictionaryFileFormat.qid:

                for entry in lines:
                    if self.pagerank_mode == PageRankFileFormat.merged:
                        new_entry = [entry[1]]
                        new_entry.extend(entry[3:])
                        name_dict[MY_SPECIAL_UNIQUE_DELIMITER.join([entry[0], entry[1]])].extend(new_entry)
                    else:
                        name_dict[MY_SPECIAL_UNIQUE_DELIMITER.join([entry[0], entry[1]])].extend(entry[1:])

            elif self.mode == DictionaryFileFormat.multiple:

                for entry in lines:
                    name_dict[entry[1]].append(entry[0])  # don't enter every possible label for an alias, just the most common one
                    # for label in entry[1:2]:            # equivalently
                    #     name_dict[label].append(entry[0])

            else:
                raise NotImplementedError

        return name_dict

    def convert_name_dict_to_spacy_patterns(self, name_dict, matcher):
        for label, variants in name_dict.items():
            self.add_entry_to_matcher_patterns(label=label, variants=variants, matcher=matcher)

    def add_entry_to_matcher_patterns(self, label, variants, matcher):
        '''
        :param canonical: str
        :param variants:  list[str]
        :param matcher: spacy.matcher.Matcher
        :return:
        '''

        patterns = []
        logger.debug("++++++++++++++++++++++++++++++++")
        logger.debug("label: {}".format(label))
        for name in variants:
            words = name.split(' ')
            pattern = [{'LOWER': w.lower()} for w in words]
            patterns.append(pattern)
            logger.debug("pattern: {}".format(pattern))
        matcher.add(label, patterns)

    def get_name_mentions(self, mentions):
        return [m for m in mentions if m.mention_type.value == "name"]

    def add_actor_mentions_to_sentence(self, sentence):

        added_actor_mentions = []

        if self.link_name_mentions_only:
            mentions_to_link = self.get_name_mentions(sentence.mention_set)
        else:
            mentions_to_link = sentence.mention_set
        mentions_to_link = [m for m in mentions_to_link if m.entity_type not in BLOCK_ENTITY_TYPES]

        for mention in mentions_to_link:

            words = [t.text for t in mention.tokens]
            spacy_doc = spacy.tokens.Doc(self.nlp.vocab, words=words)

            logger.debug("******************************")
            logger.debug("mention words: {}".format("".join(words)))
            logger.debug("mention entity type: {}".format(mention.entity_type))

            matches = self.matcher(spacy_doc)

            if len(matches) < 1:
                continue

            longest_range = max([end - start for (_, start, end) in  matches])
            longest_matches = []
            for (match_id, start, end) in matches:
                if end - start == longest_range:
                    longest_matches.append((match_id, start, end))
            best_matches = longest_matches

            if self.use_pagerank_scores:
                if self.pagerank_mode == PageRankFileFormat.json:
                    def pagerank_sort_key(x):
                        (match_id, start, end) = x
                        match_label = self.nlp.vocab.strings[match_id]
                        if self.mode == DictionaryFileFormat.qid:
                            actor_name = match_label.split(MY_SPECIAL_UNIQUE_DELIMITER)[1]
                        else:
                            actor_name = match_label
                        pagerank_score = self.pagerank_scores.get(actor_name, -1)
                        return float(pagerank_score)
                elif self.pagerank_mode == PageRankFileFormat.merged and self.mode == DictionaryFileFormat.qid:
                    def pagerank_sort_key(x):
                        (match_id, start, end) = x
                        match_label = self.nlp.vocab.strings[match_id]
                        qnode = match_label.split(MY_SPECIAL_UNIQUE_DELIMITER)[0]
                        pagerank_score = self.pagerank_scores.get(qnode, -1)
                        return float(pagerank_score)
                else:
                    raise NotImplementedError

                best_matches = heapq.nlargest(self.top_k, best_matches, key=lambda x: pagerank_sort_key(x))

            for (match_id, start, end) in best_matches:
                match_label = self.nlp.vocab.strings[match_id]
                logger.debug(">match id: {}".format(match_id))
                logger.debug(">match label: {}".format(match_label))
                if self.mode == DictionaryFileFormat.qid or self.mode == DictionaryFileFormat.frequency:
                    (actor_db_name, actor_name) = match_label.split(MY_SPECIAL_UNIQUE_DELIMITER)
                else:
                    actor_name = match_label
                    actor_db_name = "N/A"
                actor_uid = -1

                importance_score = None

                if self.use_pagerank_scores:
                    if self.pagerank_mode == PageRankFileFormat.json:
                        importance_score = self.pagerank_scores.get(actor_name, -1)
                    elif self.mode == PageRankFileFormat.merged:
                        importance_score = self.pagerank_scores.get(actor_db_name, -1)

                actor_mentions = ActorMentionModel.add_new_actor_mention(actor_mention_set=sentence.actor_mention_set,
                                                                         mention=mention,
                                                                         actor_db_name=actor_db_name,
                                                                         actor_uid=actor_uid,
                                                                         actor_name=actor_name,
                                                                         source_note=self.source_note,
                                                                         importance_score=importance_score)
                added_actor_mentions.extend(actor_mentions)

        return added_actor_mentions