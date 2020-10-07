package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;

public abstract class LanguageVariantSwitchingPattern extends Pattern {

  private final Symbol language;
  private final Symbol variant;

  /**
   * getter method for language
   */
  public Symbol getLanguage() {
    return this.language;
  }

  /**
   * getter method for variant
   */
  public Symbol getVariant() {
    return this.variant;
  }

  protected Builder modifiedCopyBuilder(Builder b) {
    super.modifiedCopyBuilder(b);
    b.withLanguage(language);
    b.withVariant(variant);
    return b;
  }

  @Override
  public abstract Builder modifiedCopyBuilder();

  protected LanguageVariantSwitchingPattern(final Builder builder) {
    super(builder);
    this.language = builder.language;
    this.variant = builder.variant;
  }

  public static abstract class Builder extends Pattern.Builder {

    private Symbol language;
    private Symbol variant;

    public Builder() {
    }

    public Builder withLanguage(final Symbol language) {
      this.language = language;
      return this;
    }

    public Builder withVariant(final Symbol variant) {
      this.variant = variant;
      return this;
    }
  }

}
