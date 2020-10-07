package com.bbn.serif.patterns;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class CombinationPattern extends Pattern {

  private final boolean isGreedy;
  private final CombinationType combinationType;
  private final List<Pattern> patternList;

  public enum CombinationType {ANY_OF, ALL_OF, NONE_OF}

  /**
   * getter method for isGreedy
   */
  public boolean isGreedy() {
    return this.isGreedy;
  }

  /**
   * getter method for combinationType
   */
  public CombinationType getCombinationType() {
    return this.combinationType;
  }

  /**
   * getter method for patternList
   */
  public List<Pattern> getPatternList() {
    return this.patternList;
  }

  private CombinationPattern(final Builder builder) {
    super(builder);
    this.isGreedy = builder.isGreedy;
    this.combinationType = builder.combinationType;
    this.patternList = ImmutableList.copyOf(builder.patternList);
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder(this.combinationType);
    super.modifiedCopyBuilder(b);
    b.withIsGreedy(isGreedy);
    b.withPatternList(patternList);
    return b;
  }

  public static class Builder extends Pattern.Builder {

    private boolean isGreedy;
    private final CombinationType combinationType;
    private List<Pattern> patternList;

    public Builder(final CombinationType combinationType) {
      this.combinationType = combinationType;
    }

    @Override
    public CombinationPattern build() {
      return new CombinationPattern(this);
    }

    public Builder withIsGreedy(final boolean isGreedy) {
      this.isGreedy = isGreedy;
      return this;
    }

    public Builder withPatternList(final List<Pattern> patternList) {
      this.patternList = patternList;
      return this;
    }
  }

  ;
}
