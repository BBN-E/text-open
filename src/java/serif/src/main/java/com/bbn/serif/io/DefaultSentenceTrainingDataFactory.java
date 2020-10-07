package com.bbn.serif.io;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.WithDocTheory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class DefaultSentenceTrainingDataFactory {

  private static final Logger
      log = LoggerFactory.getLogger(DefaultSentenceTrainingDataFactory.class);

  private DefaultSentenceTrainingDataFactory() {

  }

  public static DefaultSentenceTrainingDataFactory create() {
    return new DefaultSentenceTrainingDataFactory();
  }

  public Iterable<WithDocTheory<SentenceTheory>> getTrainingDataFrom(Parameters params,
      String taskPrefix, SerifXMLLoader loader) throws IOException {
    return getTrainingDataFrom(params, taskPrefix, loader, Functions.<DocTheory>identity());
  }

  public Iterable<WithDocTheory<SentenceTheory>> getTrainingDataFrom(Parameters params,
      String taskPrefix, SerifXMLLoader loader,
      Function<DocTheory, DocTheory> preprocessingFunction) throws IOException {
    final ImmutableMap.Builder<String, Iterable<WithDocTheory<SentenceTheory>>>
        trainingSetToSentencesB =
        ImmutableMap.builder();
    final Parameters taskSpecificParams = params.copyNamespace(taskPrefix);

    // it is best to store the training parameters under the task-specific namespace,
    // but we allow storing them at the root params for backwards-compatability
    final Parameters trainingDataParams;
    if (taskSpecificParams.isNamespacePresent("trainingData")) {
      trainingDataParams = taskSpecificParams.copyNamespace("trainingData");
    } else {
      trainingDataParams = params.copyNamespace("trainingData");
    }

    boolean foundTrainingData = false;
    for (final String compatibleTrainingSet : taskSpecificParams
        .getStringList("compatibleTrainingData")) {
      if (trainingDataParams.isNamespacePresent(compatibleTrainingSet)) {
        final Parameters mergedParameters =
            trainingDataParams.copyNamespace(compatibleTrainingSet)
                .copyMergingIntoCurrentNamespace(
                    taskSpecificParams.copyNamespaceIfPresent(compatibleTrainingSet));
        final Iterable<WithDocTheory<SentenceTheory>> sentences =
            getSentences(mergedParameters, loader, preprocessingFunction);
        trainingSetToSentencesB.put(compatibleTrainingSet, sentences);
        log.info("Loaded training set {}", compatibleTrainingSet);
        foundTrainingData = true;
      } else {
        log.info("{} is a compatible training set for this task, but was not found.",
            compatibleTrainingSet);
      }
    }
    if (foundTrainingData) {
      // TODO: task-specific preprocessing?
      return Iterables.concat(trainingSetToSentencesB.build().values());
    } else {
      throw new SerifException("No training data found searching in namespace " + taskPrefix);
    }
  }

  private Iterable<WithDocTheory<SentenceTheory>> getSentences(
      final Parameters trainingSetParameters,
      SerifXMLLoader loader, Function<DocTheory, DocTheory> preprocessingFunction) throws IOException {
    final TrainingDataSentenceLoader sentenceLoader;
    if (trainingSetParameters.isPresent("alternateSentenceLoader")) {
      sentenceLoader =
          trainingSetParameters.getParameterInitializedObject("alternateSentenceLoader",
              TrainingDataSentenceLoader.class);
    } else {
      sentenceLoader = new FromFileListSentenceLoader();
    }
    return sentenceLoader.getRawSentences(trainingSetParameters, loader, preprocessingFunction);
  }
}
