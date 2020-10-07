package com.bbn.serif.patterns;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class UnionPattern extends LanguageVariantSwitchingPattern {

  private final List<Pattern> patternList;
  private final boolean isGreedy;

  /**
   * getter method for patternList
   */
  public List<Pattern> getPatternList() {
    return this.patternList;
  }

  /**
   * getter method for isGreedy
   */
  public boolean isGreedy() {
    return this.isGreedy;
  }

  private UnionPattern(final Builder builder) {
    super(builder);
    this.patternList = ImmutableList.copyOf(builder.patternList);
    this.isGreedy = builder.isGreedy;
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withPatternList(patternList);
    b.withIsGreedy(isGreedy);
    return b;
  }

  public static class Builder extends LanguageVariantSwitchingPattern.Builder {

    private List<Pattern> patternList;
    private boolean isGreedy;

    public Builder() {
    }

    @Override
    public UnionPattern build() {
      return new UnionPattern(this);
    }

    public Builder withPatternList(final List<Pattern> patternList) {
      this.patternList = patternList;
      return this;
    }

    public Builder withIsGreedy(final boolean isGreedy) {
      this.isGreedy = isGreedy;
      return this;
    }
  }

}
