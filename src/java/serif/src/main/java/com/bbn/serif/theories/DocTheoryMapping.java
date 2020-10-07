package com.bbn.serif.theories;


import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DocTheoryMapping {

  private DocTheoryMapping() {
    throw new UnsupportedOperationException();
  }

  /**
   * Give an source Spanning from a source DocGraph, will produce a corresponding Spanning (same
   * sentence, same token indices within the sentence) from the supplied target DocGraph.  Will
   * throw an exception if the such a spanning cannot be created.
   */
  public static Optional<? extends Spanning> correspondingSpanning(Spanning sourceSpanning,
      DocTheory targetDoc) {
    checkNotNull(sourceSpanning);
    checkNotNull(targetDoc);

    final TokenSequence.Span sourceSpan = sourceSpanning.span();
    final TokenSequence sourceTokenSequence = sourceSpan.tokenSequence();
    final int sourceSentenceIndex = sourceTokenSequence.sentenceIndex();

    try {
      checkSpanValidInTarget(targetDoc, sourceSpan);
    } catch (InvalidSpanInTargetException e) {
      return Optional.absent();
    }

    final TokenSequence targetTokenSequence =
        targetDoc.sentenceTheory(sourceSentenceIndex).tokenSequence();

    return Optional.of(MinimalSpanning.forSpan(
        targetTokenSequence.span(sourceSpan.startIndex(), sourceSpan.endIndex())));
  }

  private static void checkSpanValidInTarget(DocTheory targetDoc,
      final TokenSequence.Span sourceSpan) {
    final TokenSequence sourceTokenSequence = sourceSpan.tokenSequence();
    final int sourceSentenceIndex = sourceTokenSequence.sentenceIndex();

    if (sourceSentenceIndex >= targetDoc.numSentences()) {
      throw new InvalidSpanInTargetException(String.format(
          "Attmepting to map a span from sentence %s to a target document with only %s sentences",
          sourceSentenceIndex, targetDoc.numSentences()));
    }

    final TokenSequence targetTokenSequence =
        targetDoc.sentenceTheory(sourceSentenceIndex).tokenSequence();

    if (sourceTokenSequence.size() != targetTokenSequence.size()) {
      throw new InvalidSpanInTargetException(String.format(
          "When attempting to map a spanning from one document to another, got sentence size mismatch for sentence %s; source size is %s, target is %s",
          sourceSentenceIndex, sourceTokenSequence.size(),
          targetTokenSequence.size()));
    }
  }


  public static Optional<SynNode> correspondingNodeForHead(SynNode sourceSynNode,
      DocTheory targetDoc) {
    final int sentenceIndex = sourceSynNode.span().sentenceIndex();

    try {
      checkSpanValidInTarget(targetDoc, sourceSynNode.span());
    } catch (InvalidSpanInTargetException e) {
      return Optional.absent();
    }

    final SentenceTheory targetSentenceTheory =
        targetDoc.sentenceTheory(sentenceIndex);

    final SynNode sourceHeadPreterminal = sourceSynNode.headPreterminal();
    final int sourceTokenIndex = sourceHeadPreterminal.span().startIndex();

    return Optional.of(targetSentenceTheory.parse().nodeForToken(
        targetSentenceTheory.tokenSequence().token(sourceTokenIndex)));
  }

  public static class InvalidSpanInTargetException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidSpanInTargetException(String msg) {
      super(msg);
    }
  }
}
