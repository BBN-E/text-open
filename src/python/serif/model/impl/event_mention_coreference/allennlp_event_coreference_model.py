import logging
from allennlp.predictors.predictor import Predictor
from serif.model.event_mention_coref_model import EventMentionCoreferenceModel

logger = logging.getLogger(__name__)


class AllenNLPEventCoreferenceModel(EventMentionCoreferenceModel):

    def __init__(self, model, **kwargs):
        super(AllenNLPEventCoreferenceModel, self).__init__(**kwargs)
        self.predictor = Predictor.from_path(model)

    @staticmethod
    def build_token_index_maps(serif_doc):
        index = 0
        token_to_index = dict()
        index_to_token = list()
        for sentence in serif_doc.sentences:
            for token in sentence.token_sequence:
                token_to_index[token.id] = index
                index_to_token.append(token)
                index += 1
        return token_to_index, index_to_token

    @staticmethod
    def build_token_to_event_mention_map(serif_doc):
        # logger.debug("All Event Mentions:")
        tokens_to_event_mentions = dict()
        for sentence in serif_doc.sentences:
            for em in sentence.event_mention_set:
                if em.anchor_node is not None:
                    tokens_to_event_mentions[(em.anchor_node.start_token.id, em.anchor_node.end_token.id)] = em
                else:
                    tokens_to_event_mentions[(em.owner_with_type("Sentence").token_sequence[em.semantic_phrase_start].id,
                                              em.owner_with_type("Sentence").token_sequence[em.semantic_phrase_end].id)] = em
                # logger.debug("\t{} ({},{})".format(
                #            em.anchor_node.text,
                #            em.anchor_node.start_token.id, em.anchor_node.end_token.id))
        return tokens_to_event_mentions

    @staticmethod
    def get_event_mention_from_span(index_to_token_map, tokens_to_event_mention_map, span):
        start_token = index_to_token_map[span[0]]
        end_token = index_to_token_map[span[1]]
        return AllenNLPEventCoreferenceModel.get_event_mention_from_tokens(
            tokens_to_event_mention_map, start_token, end_token)

    @staticmethod
    def get_event_mention_from_tokens(tokens_to_event_mention_map, start_token, end_token):
        event_mention = None
        if (start_token.id, end_token.id) in tokens_to_event_mention_map:
            event_mention = tokens_to_event_mention_map[(start_token.id, end_token.id)]
        # Make the (possibly faulty) assumption that the last token is the head
        elif (end_token.id, end_token.id) in tokens_to_event_mention_map:
            event_mention = tokens_to_event_mention_map[(end_token.id, end_token.id)]
        return event_mention

    def add_new_events_to_document(self, serif_doc):
        results = []
        token_to_index, index_to_token = self.build_token_index_maps(serif_doc)
        tokens_to_event_mention = self.build_token_to_event_mention_map(serif_doc)
        doc_tokens = [t.text for t in index_to_token]

        # result fields: top_spans, antecedent_indices, predicted_antecedents, document, clusters
        result = self.predictor.predict_tokenized(doc_tokens)

        # build a mapping of span tuples to mentions
        spans_to_event_mentions = dict()
        for span in result["top_spans"]:
            spans_to_event_mentions[tuple(span)] = self.get_event_mention_from_span(
                index_to_token, tokens_to_event_mention, tuple(span))

        for cluster in result["clusters"]:
            event_mentions = []
            logger.debug("Cluster:")
            for span in cluster:
                event_mention = spans_to_event_mentions[tuple(span)]
                if event_mention is not None:
                    event_mentions.append(event_mention)
                    start_token = index_to_token[span[0]]
                    end_token = index_to_token[span[1]]
                    logger.debug("\t{} ({},{})".format(
                        event_mention.anchor_node.text if event_mention.anchor_node is not None \
                            else event_mention.owner_with_type("Sentence").get_original_text_substring(event_mention.owner_with_type("Sentence").token_sequence[event_mention.semantic_phrase_start].start_char,
                                                                                                       event_mention.owner_with_type("Sentence").token_sequence[event_mention.semantic_phrase_end].end_char),
                        start_token.id, end_token.id))
                else:
                    s = " ".join([doc_tokens[i] for i in range(span[0], span[1] + 1, 1)])
                    logger.debug("\t{} ({},{}) (Unmapped)".format(
                        s, index_to_token[span[0]].id, index_to_token[span[1]].id))
            results.extend(EventMentionCoreferenceModel.add_new_event(serif_doc.event_set, event_mentions))

        logger.debug("Singletons:")
        for span, antecedent in zip(result["top_spans"],
                                    result["predicted_antecedents"]):
            event_mention = spans_to_event_mentions[tuple(span)]
            if antecedent == -1:
                if event_mention is not None:
                    results.extend(EventMentionCoreferenceModel.add_new_event(serif_doc.event_set, [event_mention]))
                    start_token = index_to_token[span[0]]
                    end_token = index_to_token[span[1]]
                    logger.debug("\t{} ({},{})".format(
                        event_mention.anchor_node.text if event_mention.anchor_node is not None \
                            else event_mention.owner_with_type("Sentence").get_original_text_substring(event_mention.owner_with_type("Sentence").token_sequence[event_mention.semantic_phrase_start].start_char,
                                                                                                       event_mention.owner_with_type("Sentence").token_sequence[event_mention.semantic_phrase_end].end_char),
                        start_token.id, end_token.id))
                else:
                    s = " ".join([doc_tokens[i] for i in range(span[0], span[1] + 1, 1)])
                    logger.debug(
                        "\t{} ({},{}) (Unmapped)".format(s, index_to_token[span[0]].id, index_to_token[span[1]].id))

        return results


if __name__ == "__main__":
    import argparse
    import serifxml3

    logger.setLevel(logging.DEBUG)

    parser = argparse.ArgumentParser()
    parser.add_argument('input_serifxml')
    parser.add_argument('output_serifxml')
    args = parser.parse_args()

    model = AllenNLPEventCoreferenceModel(
        "https://storage.googleapis.com/allennlp-public-models/coref-spanbert-large-2020.02.27.tar.gz")
    serifdoc = serifxml3.Document(args.input_serifxml)
    model.process_document(serifdoc)
    serifdoc.save(args.output_serifxml)
