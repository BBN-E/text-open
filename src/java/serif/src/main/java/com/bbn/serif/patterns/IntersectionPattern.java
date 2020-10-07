package com.bbn.serif.patterns;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class IntersectionPattern extends LanguageVariantSwitchingPattern {

  private final List<Pattern> patternList;

  /**
   * getter method for patternList
   */
  public List<Pattern> getPatternList() {
    return this.patternList;
  }

  private IntersectionPattern(final Builder builder) {
    super(builder);
    this.patternList = ImmutableList.copyOf(builder.patternList);
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withPatternList(patternList);
    return b;
  }

  public static class Builder extends LanguageVariantSwitchingPattern.Builder {

    private List<Pattern> patternList;

    public Builder() {
    }

    @Override
    public IntersectionPattern build() {
      return new IntersectionPattern(this);
    }

    public Builder withPatternList(final List<Pattern> patternList) {
      this.patternList = patternList;
      return this;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((patternList == null) ? 0 : patternList.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    IntersectionPattern other = (IntersectionPattern) obj;
    if (patternList == null) {
      if (other.patternList != null) {
        return false;
      }
    } else if (!patternList.equals(other.patternList)) {
      return false;
    }
    return true;
  }

}
