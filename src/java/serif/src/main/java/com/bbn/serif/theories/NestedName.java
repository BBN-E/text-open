package com.bbn.serif.theories;

import com.bbn.serif.types.EntityType;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NestedName implements Spanning {
  private NestedName(TokenSequence.Span span, EntityType type, final Name parent,
      final String transliteration,
      final Double score) {
    this.parent = checkNotNull(parent);
    this.span = checkNotNull(span);
    this.type = checkNotNull(type);
    // nullable
    this.transliteration = transliteration;
    this.score = score;
  }

  @Override
  public TokenSequence.Span span() {
    return span;
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  public Optional<String> transliteration() {
    return Optional.fromNullable(transliteration);
  }

  public EntityType type() {
    return type;
  }

  public Optional<Double> score() {
    return Optional.fromNullable(score);
  }

  public Name parent() {
    return parent;
  }

  public static Builder builder(final TokenSequence.Span span, final EntityType type,
      final Name parent) {
    return new Builder(span, type, parent);
  }

  private final TokenSequence.Span span;
  private final EntityType type;
  private final Name parent;

  // nullable
  private final String transliteration;
  private final Double score;

  public static final class Builder {

    private Builder(final TokenSequence.Span span, final EntityType type, final Name parent) {
      this.span = checkNotNull(span);
      this.type = checkNotNull(type);
      this.parent = checkNotNull(parent);
    }

    public Builder withTransliteration(String transliteration) {
      this.transliteration = transliteration;
      return this;
    }

    public Builder withScore(Double score) {
      this.score = score;
      return this;
    }

    public NestedName build() {
      return new NestedName(span, type, parent, transliteration, score);
    }

    private final TokenSequence.Span span;
    private final EntityType type;
    private final Name parent;

    // nullable
    private String transliteration = null;
    private Double score = null;
  }
}
