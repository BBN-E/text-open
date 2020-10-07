package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.types.ValueType;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents the mention of some value (e.g. a time, money, a crime) in text. After creation, be
 * sure to call {@link #seal()}.
 */
public final class ValueMention implements Spanning, HasExternalID {

  public ValueType fullType() {
    return type;
  }

  /**
   * @deprecated  Prefer to access {@link #fullType()} directly and
   * call {@link ValueType#baseTypeSymbol()}
   */
  @Deprecated
  public Symbol type() {
    return type.baseTypeSymbol();
  }

  /**
   * @deprecated  Prefer to access {@link #fullType()} directly and call
   * {@link ValueType#subtypeSymbol()} ()}
   */
  @Deprecated
  public Optional<Symbol> subType() {
    return type.subtypeSymbol();
  }

  public boolean isTimexValue() {
    return type.baseTypeSymbol() == TIMEX;
  }

  public boolean isSpecificDate() {
    return isTimexValue() && documentValue().isPresent() && documentValue().get()
        .isSpecificDate();
  }

  @Override
  public TokenSequence.Span span() {
    return span;
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  /**
   * Deprecated in favor of documentValue() because return value does not capture the possibility
   * that the document value has not yet been set.
   */
  @Deprecated
  public Value docValue() {
    if (docValue != null) {
      return docValue;
    } else {
      throw new UnsupportedOperationException(
          "Document value has not yet been set on this value mention. There must have been an error during deserialization.");
    }
  }

  /**
   * The resolution of this value mention text span into a Value object.
   */
  public Optional<Value> documentValue() {
    return Optional.fromNullable(docValue);
  }

  /**
   * This should never be called by API users.
   */
  @SuppressWarnings("deprecation")
  public void setDocValue(final Symbol timexVal, final Symbol timexAnchorVal,
      final Symbol timexAnchorDir, final Symbol timexSet,
      final Symbol timexMode, final Symbol timexNonSpecific) {
    if (!sealed) {
      docValue = checkNotNull(new Value(this, timexVal, timexAnchorVal, timexAnchorDir, timexSet,
          timexMode, timexNonSpecific));
      seal();
    } else {
      throw new UnsupportedOperationException(
          "Cannot set document value of ValueMention again after it's already been set once,");
    }
  }

  /**
   * because the {@link Value} needs to be set after the {@link ValueMention} is created, there is a
   * risk of loss of immutability.  Once the user is finished creating a {@link ValueMention}, they
   * should call {@link #seal()} to ensure it is truly immutable from that point onwards.
   */
  @Beta
  public void seal() {
    this.sealed = true;
  }

  @Override
  public String toString() {
    String ret = "ValueMention(" + type + ": " + span;
    if (docValue != null) {
      ret += " " + docValue.toStringNoValueMention();
    }
    return ret + ")";
  }

  public Object toStringNoValue() {
    return "ValueMention(" + type + ": " + span + ")";
  }

  private final TokenSequence.Span span;
  private final ValueType type;

  @Nullable private Value docValue;
  @Nullable private final Symbol external_id;
  private boolean sealed = false;

  private ValueMention(final ValueType type, final TokenSequence.Span span,
      @Nullable final Value docValue, @Nullable final Symbol external_id) {
    this.external_id = external_id;
    this.type = checkNotNull(type);
    this.span = checkNotNull(span);
    // nullable
    this.docValue = docValue;
    this.sealed = (docValue != null);
  }

  public static Builder builder(final ValueType valueType, final TokenSequence.Span span) {
    return new Builder(span, valueType);
  }

  private static final Symbol TIMEX = Symbol.from("TIMEX2");

  public static Optional<SynNode> node(final DocTheory doc, final ValueMention valueMention) {
    final Optional<SynNode> root = doc.sentenceTheory(valueMention.span().sentenceIndex())
        .parse().root();

    if (root.isPresent()) {
      return root.get().nodeByTokenSpan(valueMention);
    } else {
      return Optional.absent();
    }
  }

  /**
   * Returns the first {@link Mention} in {@code sentenceTheory} having the same {@link
   * com.bbn.serif.theories.TokenSequence.Span} as the given {@link ValueMention}, if any.
   */
  public static Optional<Mention> findMentionForValueMention(final SentenceTheory sentenceTheory,
      final ValueMention vm) {
    for (final Mention mention : sentenceTheory.mentions()) {
      if (mention.node().span().equals(vm.span())) {
        return Optional.of(mention);
      }
    }
    return Optional.absent();
  }

  public static final class Builder {

    private final ValueType valueType;
    private final TokenSequence.Span span;
    @Nullable private Value value = null;
    @Nullable private Symbol external_id = null;

    private Builder(final TokenSequence.Span span, final ValueType valueType) {
      this.span = span;
      this.valueType = valueType;
    }

    public Builder setDocumentValue(@Nullable Value value) {
      this.value = value;
      return this;
    }

    public Builder setExternalID(@Nullable final Symbol external_id) {
      this.external_id = external_id;
      return this;
    }

    public ValueMention build() {
      return new ValueMention(valueType, span, value, external_id);
    }
  }
}
