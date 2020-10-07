package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.converters.SexpPatternConverter;
import com.bbn.bue.common.HasStableHashCode;

import java.util.List;
import java.util.Set;

public abstract class Pattern {

  private final Symbol id;
  private final Symbol shortcut;
  private final float score;
  private final PatternReturn patternReturn;
  private final int scoreGroup;
  private final boolean topLevelReturn;
  private final Symbol scoreFunction;

  /**
   * getter method for id
   */
  public Symbol getId() {
    return this.id;
  }

  /**
   * getter method for shortcut
   */
  public Symbol getShortcut() {
    return this.shortcut;
  }

  /**
   * getter method for score
   */
  public float getScore() {
    return this.score;
  }

  /**
   * getter method for patternReturn
   */
  public PatternReturn getPatternReturn() {
    return this.patternReturn;
  }

  /**
   * getter method for scoreGroup
   */
  public int getScoreGroup() {
    return this.scoreGroup;
  }

  /**
   * getter method for topLevelReturn
   */
  public boolean isTopLevelReturn() {
    return this.topLevelReturn;
  }

  /**
   * getter method for scoreFunction TODO: make this a proper score function instead of just the
   * name
   */
  public Symbol getScoreFunction() {
    return this.scoreFunction;
  }

  protected Builder modifiedCopyBuilder(Builder b) {
    b.withId(id);
    b.withShortcut(shortcut);
    b.withScore(score);
    b.withPatternReturn(patternReturn);
    b.withScoreFunction(scoreFunction);
    b.withScoreGroup(scoreGroup);
    b.withTopLevelReturn(topLevelReturn);
    return b;
  }

  public abstract Builder modifiedCopyBuilder();

  protected Pattern(final Builder builder) {
    this.id = builder.id;
    this.shortcut = builder.shortcut;
    this.score = builder.score;
    this.patternReturn = builder.patternReturn;
    this.scoreGroup = builder.scoreGroup;
    this.topLevelReturn = builder.topLevelReturn;
    this.scoreFunction = builder.scoreFunction;
  }

  public static abstract class Builder {

    private Symbol id;
    private Symbol shortcut;
    private float score;
    private PatternReturn patternReturn;
    private int scoreGroup;
    private boolean topLevelReturn;
    private Symbol scoreFunction;

    public abstract Pattern build();

    public Builder withId(final Symbol id) {
      this.id = id;
      return this;
    }

    public Builder withShortcut(final Symbol shortcut) {
      this.shortcut = shortcut;
      return this;
    }

    public Builder withScore(final float score) {
      this.score = score;
      return this;
    }

    public Builder withPatternReturn(final PatternReturn patternReturn) {
      this.patternReturn = patternReturn;
      return this;
    }

    public Builder withScoreGroup(final int scoreGroup) {
      this.scoreGroup = scoreGroup;
      return this;
    }

    public Builder withTopLevelReturn(final boolean topLevelReturn) {
      this.topLevelReturn = topLevelReturn;
      return this;
    }

    public Builder withScoreFunction(final Symbol scoreFunction) {
      this.scoreFunction = scoreFunction;
      return this;
    }
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();
    sb.append(toString());

    String str = sb.toString();

    // Is there a way to make this pattern becomes a string that (1) has no redundancy and (2) can be readable by a user (e.g., our customer)?
    // Examples (can we convert left items below to something look like the right?
    // (1) unary:
    // - [m]household[0] <ref> the household questionnaire[OTH] -> the household[0] questionnaire[OTH]
    // - [n]assistance[0] <unknown> ffp[ORG] -> ffp[ORG] assistance[0]
    // - [m]food[0] <ref> institute[ORG] -> food[0] institute[ORG]

    str = str.replace("<unknown>", "<premod>"); // <unknown> is premodifier if I understand correctly

    return str;
  }

  public String getPrettyReturnLabel() {
    if (patternReturn == null)
      return "";

    if (!(patternReturn instanceof LabelPatternReturn))
      return "";

    LabelPatternReturn lpr = (LabelPatternReturn) patternReturn;

    if (lpr.getLabel() == Symbol.from("slot0"))
      return "[0]";
    if (lpr.getLabel() == Symbol.from("slot1"))
      return "[1]";

    return "";
  }

  //temporary toString for convenience until we write more complete ones
  @Override
  public String toString() {
    return new SexpPatternConverter().convert(this).toString();
  }

  // Needed because hash codes of Symbols change from run to run, so the hash code of a list
  // of Symbols will also change from run to run. Must use stableHashCode() function to
  // have the hashCode to be consistent from run to run.
  public static int stableHashCode(List<? extends HasStableHashCode> list) {
    int hashCode = 1;
    for (HasStableHashCode e : list)
      hashCode = 31*hashCode + (e==null ? 0 : e.stableHashCode());
    return hashCode;
  }
  public static int stableHashCode(Set<? extends HasStableHashCode> set) {
    int hashCode = 1;
    for (HasStableHashCode e : set)
      hashCode = 31*hashCode + (e==null ? 0 : e.stableHashCode());
    return hashCode;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result
        + ((patternReturn == null) ? 0 : patternReturn.hashCode());
    result = prime * result + Float.floatToIntBits(score);
    result = prime * result
        + ((scoreFunction == null) ? 0 : scoreFunction.hashCode());
    result = prime * result + scoreGroup;
    result = prime * result
        + ((shortcut == null) ? 0 : shortcut.hashCode());
    result = prime * result + (topLevelReturn ? 1231 : 1237);
    return result;
  }

  // Just use parts of patterns that are guaranteed to be stable from run to run
  // TODO: Create stableHashCode function for subclasses of Pattern
  public int stableHashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.stableHashCode());
    result = prime * result
        + ((patternReturn == null) ? 0 : patternReturn.hashCode());
    result = prime * result + Float.floatToIntBits(score);
    result = prime * result
        + ((scoreFunction == null) ? 0 : scoreFunction.stableHashCode());
    result = prime * result + scoreGroup;
    result = prime * result
        + ((shortcut == null) ? 0 : shortcut.stableHashCode());
    result = prime * result + (topLevelReturn ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Pattern other = (Pattern) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (patternReturn == null) {
      if (other.patternReturn != null) {
        return false;
      }
    } else if (!patternReturn.equals(other.patternReturn)) {
      return false;
    }
    if (Float.floatToIntBits(score) != Float.floatToIntBits(other.score)) {
      return false;
    }
    if (scoreFunction == null) {
      if (other.scoreFunction != null) {
        return false;
      }
    } else if (!scoreFunction.equals(other.scoreFunction)) {
      return false;
    }
    if (scoreGroup != other.scoreGroup) {
      return false;
    }
    if (shortcut == null) {
      if (other.shortcut != null) {
        return false;
      }
    } else if (!shortcut.equals(other.shortcut)) {
      return false;
    }
    if (topLevelReturn != other.topLevelReturn) {
      return false;
    }
    return true;
  }


}
