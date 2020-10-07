package com.bbn.serif.patterns;

import java.util.List;


public final class RegexPattern extends LanguageVariantSwitchingPattern {

  private final List<Pattern> subpatterns;
  private final boolean topMentionsOnly;
  private final boolean allowHeads;
  private final boolean matchWholeExtent;
  private final boolean addSpaces;

  /**
   * getter method for subpatterns
   */
  public List<Pattern> getSubpatterns() {
    return this.subpatterns;
  }

  /**
   * getter method for topMentionsOnly
   */
  public boolean isTopMentionsOnly() {
    return this.topMentionsOnly;
  }

  /**
   * getter method for allowHeads
   */
  public boolean isAllowHeads() {
    return this.allowHeads;
  }

  /**
   * getter method for matchWholeExtent
   */
  public boolean isMatchWholeExtent() {
    return this.matchWholeExtent;
  }

  /**
   * getter method for addSpaces
   */
  public boolean isAddSpaces() {
    return this.addSpaces;
  }

  public RegexPattern(final Builder builder) {
    super(builder);
    this.subpatterns = builder.subpatterns;
    this.topMentionsOnly = builder.topMentionsOnly;
    this.allowHeads = builder.allowHeads;
    this.matchWholeExtent = builder.matchWholeExtent;
    this.addSpaces = builder.addSpaces;
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withSubpatterns(subpatterns);
    b.withTopMentionsOnly(topMentionsOnly);
    b.withAllowHeads(allowHeads);
    b.withMatchWholeExtent(matchWholeExtent);
    b.withAddSpaces(addSpaces);
    return b;
  }

  public static class Builder extends LanguageVariantSwitchingPattern.Builder {

    private List<Pattern> subpatterns;
    private boolean topMentionsOnly = false;
    private boolean allowHeads = true;
    private boolean matchWholeExtent = false;
    private boolean addSpaces = true;

    public Builder() {
    }

    @Override
    public RegexPattern build() {
      return new RegexPattern(this);
    }

    public Builder withSubpatterns(final List<Pattern> subpatterns) {
      this.subpatterns = subpatterns;
      return this;
    }

    public Builder withTopMentionsOnly(final boolean topMentionsOnly) {
      this.topMentionsOnly = topMentionsOnly;
      return this;
    }

    public Builder withAllowHeads(final boolean allowHeads) {
      this.allowHeads = allowHeads;
      return this;
    }

    public Builder withMatchWholeExtent(final boolean matchWholeExtent) {
      this.matchWholeExtent = matchWholeExtent;
      return this;
    }

    public Builder withAddSpaces(final boolean addSpaces) {
      this.addSpaces = addSpaces;
      return this;
    }
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Pattern p : subpatterns) {
      if (!first)
        sb.append(" ");
      first = false;
      sb.append(p.toPrettyString());
    }

    return sb.toString().replaceAll("\\s+", " ");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (addSpaces ? 1231 : 1237);
    result = prime * result + (allowHeads ? 1231 : 1237);
    result = prime * result + (matchWholeExtent ? 1231 : 1237);
    result = prime * result
        + ((subpatterns == null) ? 0 : subpatterns.hashCode());
    result = prime * result + (topMentionsOnly ? 1231 : 1237);
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
    RegexPattern other = (RegexPattern) obj;
    if (addSpaces != other.addSpaces) {
      return false;
    }
    if (allowHeads != other.allowHeads) {
      return false;
    }
    if (matchWholeExtent != other.matchWholeExtent) {
      return false;
    }
    if (subpatterns == null) {
      if (other.subpatterns != null) {
        return false;
      }
    } else if (!subpatterns.equals(other.subpatterns)) {
      return false;
    }
    if (topMentionsOnly != other.topMentionsOnly) {
      return false;
    }
    return true;
  }

}
