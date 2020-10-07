package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.strings.LocatedString;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This represents the textual content of a sentence (as opposed to linguistic analyses associated
 * with one).
 */
@TextGroupImmutable
@Value.Immutable
public abstract class Sentence {

  public abstract Document document();

  public abstract Region region();
  /**
   * The 0-based index of this sentence in the document.
   */
  public abstract int sentenceNumber();

  public abstract LocatedString locatedString();

  protected void check() {
    checkArgument(sentenceNumber() >= 0);
    checkArgument(document().regions().isPresent()
            && document().regions().get().contains(region()),
        "Region for sentence not among document regions");
  }

  /**
   * Gets a builder for a {@code Sentence}.
   * @param doc The document this sentence belongs to.
   * @param sentenceNumber The 0-based index of this sentence in the document.
   */
  public static Builder forSentenceInDocument(Document doc, int sentenceNumber) {
    return new Builder().document(doc).sentenceNumber(sentenceNumber);
  }


  public static class Builder extends ImmutableSentence.Builder {

    /**
     * Synonym for {@link #region()}, provided for backwards-compatibility
     */
    public Builder withRegion(Region r) {
      return region(r);
    }

    /**
     * Synonym for {@link #locatedString(LocatedString)} ()}, provided for backwards-compatibility
     */
    public Builder withContent(LocatedString content) {
      return locatedString(content);
    }
  }

  @Override
  public String toString() {
    return document().name() + "-" + sentenceNumber() + ":" + locatedString().referenceBounds();
  }
}
