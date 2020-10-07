package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;

import static com.google.common.base.Preconditions.checkNotNull;

public class LabelPatternReturn implements PatternReturn {

  private final Symbol label;

  /**
   * getter method for label
   */
  public Symbol getLabel() {
    return this.label;
  }

  public LabelPatternReturn(Symbol label) {
    this.label = checkNotNull(label);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((label == null) ? 0 : label.stableHashCode());
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
    LabelPatternReturn other = (LabelPatternReturn) obj;
    if (label == null) {
      if (other.label != null) {
        return false;
      }
    } else if (!label.equals(other.label)) {
      return false;
    }
    return true;
  }

}
