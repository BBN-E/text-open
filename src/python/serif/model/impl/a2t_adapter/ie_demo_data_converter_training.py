import serifxml3
import json
import logging

logger = logging.getLogger(__name__)

granular_types = {
    "Corruplate",
    "Epidemiplate",
    "Protestplate",
    "Terrorplate",
    "Disasterplate",
    "Displacementplate"
}

allowed_basic_event_types = {
    "Apply-NPI",
    "Communicate-Event",
    "Death-from-Crisis-Event",
    "Disease-Infects",
    "Disease-Outbreak",
    "Natural-Phenomenon-Event-or-SoA",
    "Other-Government-Action",
    "Provide-Aid",
    "Refugee-Movement",
    "Weather-or-Environmental-Damage"
}

allowed_arg_roles = {
    "where",
    # "NPI-Events",
    # "related-natural-phenomena",
    "outcome",
    "when",
    "current-location",
    "origin",
    # "damage",
    "killed-count",
    # "Assistance-provided",
    # "disease",
    # "human-displacement-event",
    # "major-disaster-event",
    # "event-or-SoA-at-origin",
    "total-displaced-count",
    # "outbreak-event",
    # "group-identity",
    "destination",
    "transiting-location",
    "outcome-occurred",
    "infected-count",
    # "settlement-status-event-or-SoA",
    # "human-displacement-events",
    # "responders",
    "assistance-provided",
    # "Transitory-events",
    "who",
    # "rescue-events",
    "infected-cumulative",
    "killed-cumulative",
    # "missing-count",
    # "injured-count",
    # "Assistance-needed",
    "infected-individuals",
    # "terror-event",
    # "target-physical",
    # "affected-cumulative-count",
    # "protest-against",
    # "protest-event",
    # "assistance-needed",
    "killed",
    "killed-individuals",
    # "repair",
    "outcome-hypothetical",
    # "announce-disaster-warnings",
    # "protest-for",
    # "tested-count",
    # "tested-individuals",
    # "organizer",
    # "hospitalized-individuals",
    # "detained-count",
    # "hospitalized-count",
    # "judicial-actions",
    # "named-perp-org",
    # "recovered-count",
    # "blamed-by",
    # "weapon",
    # "vaccinated-individuals",
    # "named-organizer",
    # "charged-with",
    # "declare-emergency",
    # "rescued-count",
    # "target-human",
    # "wounded",
    # "perp-killed",
    "tested-cumulative",
    # "outcome-averted",
    # "corrupt-event",
    # "recovered-cumulative",
    # "claimed-by",
    # "named-perp",
    # "occupy",
    # "blocked-migration-count",
    # "exposed-cumulative",
    # "vaccinated-count",
    # "vaccinated-cumulative",
    # "arrested",
    # "perp-objective",
    # "exposed-individuals",
    # "perp-captured",
    # "disease-outbreak-events",
    # "exposed-count",
    # "individuals-affected",
    # "imprisoned",
}


def serifxml_dumper(serifxml_path):
    serif_doc = serifxml3.Document(serifxml_path)
    event_mentions = set()
    slot_fillers = set()
    sentence_to_event_types = dict()
    sentence_to_filler_types = dict()
    sentence_to_event_mention_spans = dict()
    sentence_to_mention_spans = dict()
    sentence_to_event_arg_dot_product = dict()
    anchor_node_to_ems = dict()
    for sentence in serif_doc.sentences:
        token_sequence = sentence.token_sequence
        sentence_start_char = sentence.start_char
        for event_mention in sentence.event_mention_set or ():
            sentence_to_event_types.setdefault(sentence, set()).add(event_mention.event_type)
            event_mentions.add(event_mention)
            event_mention_start_char = token_sequence[
                                           event_mention.semantic_phrase_start].start_char - sentence_start_char
            event_mention_end_char = token_sequence[event_mention.semantic_phrase_end].end_char - sentence_start_char
            sentence_to_event_mention_spans.setdefault(sentence, set()).add(
                (event_mention.semantic_phrase_start, event_mention.semantic_phrase_end, event_mention.event_type))
            if event_mention.anchor_node is not None:
                anchor_node_to_ems.setdefault(event_mention.anchor_node, set()).add(event_mention)
    for serif_event in serif_doc.event_set or ():
        if serif_event.event_type in granular_types:
            arg_role_to_entity_mentions = dict()
            arg_role_to_event_mentions = dict()
            for argument in serif_event.arguments or ():
                if argument.entity is not None:
                    for entity_mention in argument.entity.mentions:
                        arg_role_to_entity_mentions.setdefault(argument.role, set()).add(entity_mention)
                        slot_fillers.add(entity_mention)
                        sentence = entity_mention.sentence
                        sentence_start_char = sentence.start_char
                        sentence_to_filler_types.setdefault(entity_mention.sentence, set()).add(argument.role)
                        sentence_to_mention_spans.setdefault(sentence, set()).add((entity_mention.tokens[0].index(),
                                                                                   entity_mention.tokens[-1].index(),
                                                                                   entity_mention.entity_type, argument.role))
                if argument.event_mention is not None:
                    arg_role_to_event_mentions.setdefault(argument.role, set()).add(argument.event_mention)
                    sentence_to_filler_types.setdefault(argument.event_mention.sentence, set()).add(argument.role)
            possible_ems = set()
            for event_anchor in serif_event.anchors:
                if event_anchor.anchor_node is not None:
                    possible_ems.update(anchor_node_to_ems.get(event_anchor.anchor_node, ()))
                if event_anchor.anchor_event_mention is not None:
                    event_anchor.add(event_anchor.anchor_event_mention)
            # for argument in serif_event.arguments or ():
            #     if argument.event_mention is not None:
            #         possible_ems.add(argument.event_mention)
            for event_mention in possible_ems:
                for arg_role, entity_mentions in arg_role_to_entity_mentions.items():
                    for entity_mention in entity_mentions:
                        if event_mention.sentence is entity_mention.sentence:
                            sentence_to_event_arg_dot_product.setdefault(event_mention.sentence,
                                                                         list()).append([[
                                event_mention.semantic_phrase_start,
                                event_mention.semantic_phrase_end,
                                event_mention.event_type],
                                arg_role,
                                [entity_mention.tokens[
                                     0].index(),
                                 entity_mention.tokens[
                                     -1].index(),
                                 entity_mention.entity_type, "Mention"]])
                for arg_role, arg_event_mentions in arg_role_to_event_mentions.items():
                    for arg_event_mention in arg_event_mentions:
                        if event_mention.sentence is arg_event_mention.sentence:
                            sentence_to_event_arg_dot_product.setdefault(event_mention.sentence,
                                                                         list()).append([[
                                event_mention.semantic_phrase_start,
                                event_mention.semantic_phrase_end,
                                event_mention.event_type],
                                arg_role,
                                [arg_event_mention.semantic_phrase_start,
                                 arg_event_mention.semantic_phrase_end,
                                 arg_event_mention.event_type, "EventMention"]])

    ret = []
    sentences_pool = set(sentence_to_event_mention_spans.keys())
    sentences_pool = sentences_pool.union(set(sentence_to_mention_spans.keys()))
    sentences_pool = sentences_pool.union(set(sentence_to_event_arg_dot_product.keys()))
    for sentence in sentences_pool:
        ret.append(
            ["{}#{}".format(serif_doc.docid, sentence.sent_no), [token.text for token in sentence.token_sequence],
             list(sentence_to_event_types.get(sentence, list())),
             list(sentence_to_filler_types.get(sentence, list())),
             list(list(i) for i in sentence_to_event_mention_spans.get(sentence, list())),
             list(list(i) for i in sentence_to_mention_spans.get(sentence, list())),
             sentence.parse.root._treebank_str(),
             sentence_to_event_arg_dot_product.get(sentence, list())])
    return ret


def sentence_type_getter(sentence_en):
    event_types = set()
    filler_types = set()
    for _, _, event_type in sentence_en["event_spans"]:
        event_types.add(event_type)
    for _, role, _ in sentence_en["event_arg_edges"]:
        filler_types.add(role)
    return event_types, filler_types

class RoundRobinFeatureSelector():
    NATag = "NA"
    def __init__(self, all_feature_tags):
        self.pool = {
            feature_tag: set() for feature_tag in all_feature_tags
        }
        self.pool[self.NATag] = set()
        self.valid_example_ids = set()

    def observe_datapoint(self, example_id, tags):
        self.valid_example_ids.add(example_id)
        if len(tags) < 1:
            self.pool[self.NATag].add(example_id)
        else:
            for tag in tags:
                self.pool[tag].add(example_id)

    def build_dataset(self):
        ret = []
        non_na_tags = sorted(list(set(self.pool.keys())))
        non_na_tags.remove(self.NATag)
        current_iter = 0
        while True:
            selected_tag = non_na_tags[current_iter % len(non_na_tags)]
            if len(self.pool[selected_tag]) > 0:
                selected_example_id = list(self.pool[selected_tag])[0]
                self.valid_example_ids.remove(selected_example_id)
                for tag, example_ids in self.pool.items():
                    example_ids.discard(selected_example_id)
                ret.append(selected_example_id)
            current_iter += 1
            logger.info("At iter {}".format(current_iter))
            if sum(len(i) for i in self.pool.values()) - len(self.pool[self.NATag]) == 0:
                break
        if len(self.valid_example_ids) > 0:
            ret.extend(sorted(list(self.valid_example_ids)))
        self.valid_example_ids.clear()
        return ret

def sentences_arrangement(sent_id_to_sent_en):
    ret = []
    all_types = set(allowed_basic_event_types).union(set(allowed_arg_roles))
    all_types.add(RoundRobinFeatureSelector.NATag)
    selector = RoundRobinFeatureSelector(all_types)
    for sent_id, sent_en in sent_id_to_sent_en.items():
        event_types, entity_types = sentence_type_getter(sent_en)
        event_types = event_types.intersection(allowed_basic_event_types)
        entity_types = entity_types.intersection(allowed_arg_roles)
        all_types_local = event_types.union(entity_types)
        selector.observe_datapoint(sent_id, all_types_local)
    for sent_id in selector.build_dataset():
        ret.append(sent_id_to_sent_en[sent_id])
    return ret


def main():
    # input_serifxml_list = "/nfs/raid88/u10/users/ychan-ad/BETTER/p2_granular_events/data/serifxml/analysis_p2/output.list"
    input_serifxml_list = "/d4m/ears/expts/48484.082922.v1/expts/p2_granular_farsi_eval/serif_files.list"
    output_sentence_path = "/nfs/raid88/u10/users/hqiu_ad/data/better/ie_demo/selected_farsi_sentences_all.ljson"
    sent_id_to_sent_en = dict()
    with open(input_serifxml_list) as fp:
        for i in fp:
            i = i.strip()
            selected_sentences = serifxml_dumper(i)
            for sent_id, sentence_tokens, basic_types, granular_toles, event_spans, entity_mention_spans, parse_str, event_arg_edges in selected_sentences:
                sent_id_to_sent_en[sent_id] = {
                    "tokens": sentence_tokens, "event_spans": event_spans, "entity_spans": entity_mention_spans,
                    "parse": parse_str, "event_arg_edges": event_arg_edges, "sent_id": sent_id
                }
    with open(output_sentence_path, 'w') as wfp:
        for i in sentences_arrangement(sent_id_to_sent_en):
            wfp.write("{}\n".format(json.dumps(i, ensure_ascii=False)))


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
