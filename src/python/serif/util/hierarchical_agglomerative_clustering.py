# hierarchical agglomerative clustering applied to pairwise mention coref predictions from nlplingo
# https://scikit-learn.org/stable/modules/generated/sklearn.cluster.AgglomerativeClustering.html

import logging

from sklearn.cluster import AgglomerativeClustering
import numpy as np

from collections import defaultdict

from nlplingo.decoding.prediction_theory import DocumentPrediction, EntityCoreferencePrediction

logger = logging.getLogger(__name__)

def hierarchical_agglomerative_clustering(mentions, pairwise_similarity, distance_threshold=0.4):
    '''
    :type mentions: list[tup] of mention or event mention refs
    :type pairwise_similarity: dict[dict[int]]
    :return: doc_p with transformed '.entity_coreference' predictions
    '''

    ### CREATE SIMILARITY AND DISTANCE MATRICES

    similarity_matrix = np.identity(len(mentions))

    for i, m1 in enumerate(mentions):
        for j, m2 in enumerate(mentions):
            if m1 in pairwise_similarity and m2 in pairwise_similarity[m1]:
                score = pairwise_similarity[m1][m2]
                similarity_matrix[i][j] = score

    distance_matrix = np.ones((len(mentions), len(mentions))) - similarity_matrix  # TODO is this the correct conversion?

    ### APPLY HIERARCHICAL AGGLOMERATIVE CLUSTERING

    logger.debug('similarity matrix\n{}'.format(similarity_matrix))
    logger.debug('distance matrix\n{}'.format(distance_matrix))

    # apply algorithm
    clusterer = AgglomerativeClustering(n_clusters=None, affinity='precomputed', linkage='average', distance_threshold=distance_threshold)
    cluster_labels = clusterer.fit_predict(distance_matrix)  # predicted cluster ids

    ### REDISTRIBUTE MENTIONS ACCORDING TO NEW CLUSTERS

    cluster_label_to_mentions = defaultdict(list)
    for i, cluster_label in enumerate(cluster_labels):
        cluster_label_to_mentions[cluster_label].append(mentions[i])

    return cluster_label_to_mentions


def redistribute_entity_coref_predictions_based_on_hierarchical_agglomerative_clustering(doc_p, distance_threshold=0.4):
    '''
    :type doc_p: nlplingo.decoding.prediction_theory.DocumentPrediction
    :return: doc_p with transformed '.entity_coreference' predictions
    '''

    ### CREATE SIMILARITY AND DISTANCE MATRICES

    mention_set = set()
    pairwise_similarity = defaultdict(lambda: defaultdict(int))

    for entity_coref_id, entity_coref_p in doc_p.entity_coreference.items():

        assert len(entity_coref_p.entity_mentions) == 2  # from spanpair classification model

        m1 = entity_coref_p.entity_mentions[0]
        m2 = entity_coref_p.entity_mentions[1]

        mention_set.add(m1)
        mention_set.add(m2)

        pairwise_similarity[m1][m2] = entity_coref_p.score
        pairwise_similarity[m2][m1] = entity_coref_p.score

    mentions = sorted(list(mention_set), key=lambda x: x[0])  # sort by sent no

    cluster_label_to_mentions = hierarchical_agglomerative_clustering(mentions=mentions,
                                                                      pairwise_similarity=pairwise_similarity,
                                                                      distance_threshold=distance_threshold)

    ### CREATE UPDATED .entity_coreference FOR doc_p

    doc_p.entity_coreference = dict()
    for cluster_label, cluster_mentions in cluster_label_to_mentions.items():

        entity_coref_p = EntityCoreferencePrediction()
        entity_coref_p.id = cluster_label
        entity_coref_p.entity_mentions = cluster_mentions

        doc_p.entity_coreference[entity_coref_p.id] = entity_coref_p

    return doc_p
