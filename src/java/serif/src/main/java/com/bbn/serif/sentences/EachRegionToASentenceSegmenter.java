package com.bbn.serif.sentences;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.sentences.constraints.SentenceSegmentationConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Region;
import com.bbn.serif.theories.Sentence;
import com.bbn.serif.theories.SentenceTheoryBeam;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Makes each {@link com.bbn.serif.theories.Region} one big sentence.
 * Input documents must have {@link Region}s present.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class EachRegionToASentenceSegmenter implements SentenceFinder {

  @Override
  public DocTheory segmentSentences(final DocTheory docTheory) {
    return segmentSentences(docTheory, ImmutableSet.<SentenceSegmentationConstraint>of());
  }

  @Override
  public DocTheory segmentSentences(final DocTheory docTheory,
      final Set<SentenceSegmentationConstraint> constraints) {
    if (!constraints.isEmpty()) {
      throw new RuntimeException("Constraint handling not implemented!");
    }
    checkArgument(docTheory.document().regions().isPresent(),
        "Can't segment sentences in a document without regions");

    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();

    // checked above
    //noinspection OptionalGetWithoutIsPresent
    for (final Region region : docTheory.document().regions().get()) {
      ret.addSentenceTheoryBeams(
          new SentenceTheoryBeam.Builder()
              .sentence(
                  Sentence.forSentenceInDocument(docTheory.document(), 0)
                      .withContent(region.content())
                      .region(region)
                      .build())
              .build()).build();
    }
    return ret.build();
  }

  @Override
  public void finish() throws IOException {

  }

  public static class Builder extends ImmutableEachRegionToASentenceSegmenter.Builder {

  }

}
