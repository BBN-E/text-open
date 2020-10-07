package com.bbn.serif.theories.serialization;


import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Allows serialization of references to mentions using Jackson.
 *
 * JSerif objects are not directly serializable because they would typically require serializing the
 * entire document theory along with them. However, you can use these serializable reference objects
 * to record relationships involving Serif objects and then resolve them against a document theory
 * when deserializing. In general resolving such references will be successful only against the the
 * DocTheory which they were derived from.  Attempt to resolve them against a diferent document
 * theory will give undefined results.
 */
public final class MentionSerializationReference implements SpanningSerializationReference {

  @JsonProperty("sentenceIdx")
  private final int sentenceIdx;
  @JsonProperty("mentionIdx")
  private final int mentionIdx;

  @JsonCreator
  MentionSerializationReference(@JsonProperty("sentenceIdx") int sentenceIdx,
      @JsonProperty("mentionIdx") int mentionIdx) {
    checkArgument(sentenceIdx >= 0);
    checkArgument(mentionIdx >= 0);
    this.sentenceIdx = sentenceIdx;
    this.mentionIdx = mentionIdx;
  }

  /**
   * Creates a {@code MentionSerializationReference} for the given mention.
   */
  public static MentionSerializationReference from(Mention m, DocTheory dt) {
    final int sentenceIdx = m.span().sentenceIndex();
    final int mentionIdx = m.indexInSentence(dt);
    return new MentionSerializationReference(sentenceIdx, mentionIdx);
  }

  /**
   * Resolves the mention reference against the provided {@link com.bbn.serif.theories.DocTheory}.
   */
  public Mention toMention(DocTheory dt) {
    if (sentenceIdx >= dt.numSentences()) {
      throw new NoSuchElementException("Mention reference references sentence "
          + sentenceIdx + ", but only " + dt.numSentences()
          + "sentences are available in document with ID " + dt.docid());
    }
    final SentenceTheory st = dt.sentenceTheory(sentenceIdx);
    if (mentionIdx >= st.mentions().size()) {
      throw new NoSuchElementException("Mention reference references mention "
          + mentionIdx + " of sentence " + sentenceIdx
          + "of document with ID " + dt.docid() + " but only " + st.mentions().size()
          + " mentions are available");
    }
    return st.mentions().get(mentionIdx);
  }

  @Override
  public Spanning toSpanning(DocTheory dt) {
    return toMention(dt);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(sentenceIdx, mentionIdx);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MentionSerializationReference other = (MentionSerializationReference) obj;
    return Objects.equal(this.sentenceIdx, other.sentenceIdx) && Objects
        .equal(this.mentionIdx, other.mentionIdx);
  }

  /**
   * Returns an unresolvable dummy reference, for use in unit tests only.
   */
  public static MentionSerializationReference testingDummy() {
    return new MentionSerializationReference(0, 0);
  }
}
