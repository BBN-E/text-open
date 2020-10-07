package com.bbn.serif.theories.serialization;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMention;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Allows serialization of references to value mentions using Jackson.
 *
 * JSerif objects are not directly serializable because they would typically require serializing the
 * entire document theory along with them. However, you can use these serializable reference objects
 * to record relationships involving Serif objects and then resolve them against a document theory
 * when deserializing. In general resolving such references will be successful only against the the
 * DocTheory which they were derived from.  Attempt to resolve them against a diferent document
 * theory will give undefined results.
 */
public final class ValueMentionSerializationReference implements SpanningSerializationReference {

  @JsonProperty("docLevel")
  private final boolean docLevel;
  @JsonProperty("sentenceIdx")
  private final int sentenceIdx;
  @JsonProperty("valueMentionIdx")
  private final int valueMentionIdx;

  @JsonCreator
  ValueMentionSerializationReference(@JsonProperty("docLevel") boolean docLevel,
      @JsonProperty("sentenceIdx") int sentenceIdx,
      @JsonProperty("valueMentionIdx") int valueMentionIdx) {
    if (docLevel) {
      checkArgument(sentenceIdx == -1, "Sentence IDX should be -1 for doc-level mentions");
    } else {
      checkArgument(sentenceIdx >= 0);
    }
    checkArgument(valueMentionIdx >= 0);
    this.sentenceIdx = sentenceIdx;
    this.valueMentionIdx = valueMentionIdx;
    this.docLevel = docLevel;
  }

  /**
   * Creates a {@code ValueMentionSerializationReference} for the given mention.
   */
  public static ValueMentionSerializationReference from(ValueMention vm, DocTheory dt) {
    if (dt.valueMentions().asList().contains(vm)) {
      return new ValueMentionSerializationReference(true, -1,
          dt.valueMentions().asList().indexOf(vm));
    } else {
      final int sentenceIdx = vm.span().sentenceIndex();
      final int valueMentionIdx = dt.sentenceTheory(vm.span().sentenceIndex())
          .valueMentions().asList().indexOf(vm);

      return new ValueMentionSerializationReference(false, sentenceIdx, valueMentionIdx);
    }
  }

  /**
   * Resolves the value mention reference against the provided {@link
   * com.bbn.serif.theories.DocTheory}.
   */
  public ValueMention toValueMention(DocTheory dt) {
    if (docLevel) {
      if (valueMentionIdx >= dt.valueMentions().size()) {
        throw new NoSuchElementException("Value mention reference references "
            + "document-level value mention "
            + valueMentionIdx + " of sentence " + sentenceIdx
            + "of document with ID " + dt.docid() + " but only " + dt.valueMentions().size()
            + " doc-level value mentions are available");
      }
      return dt.valueMentions().get(valueMentionIdx);
    }
    if (sentenceIdx >= dt.numSentences()) {
      throw new NoSuchElementException("Value mention reference references sentence "
          + sentenceIdx + ", but only " + dt.numSentences()
          + "sentences are available in document with ID " + dt.docid());
    }
    final SentenceTheory st = dt.sentenceTheory(sentenceIdx);
    if (valueMentionIdx >= st.valueMentions().size()) {
      throw new NoSuchElementException("Value mention reference references value mention "
          + valueMentionIdx + " of sentence " + sentenceIdx
          + "of document with ID " + dt.docid() + " but only " + st.valueMentions().size()
          + " value mentions are available");
    }
    return st.valueMentions().get(valueMentionIdx);
  }

  @Override
  public ValueMention toSpanning(DocTheory dt) {
    return toValueMention(dt);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(docLevel, sentenceIdx, valueMentionIdx);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ValueMentionSerializationReference other = (ValueMentionSerializationReference) obj;
    return Objects.equal(this.docLevel, other.docLevel) && Objects
        .equal(this.sentenceIdx, other.sentenceIdx) && Objects
        .equal(this.valueMentionIdx, other.valueMentionIdx);
  }
}
