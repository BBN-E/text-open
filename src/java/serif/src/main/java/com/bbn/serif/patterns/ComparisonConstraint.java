package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;

public final class ComparisonConstraint {

  private final Symbol constraintType;
  private final Symbol comparisonOperator;
  private final Integer value;

  public Symbol getConstraintType() {
    return constraintType;
  }

  public Symbol getComparisonOperator() {
    return comparisonOperator;
  }

  public Integer getValue() {
    return value;
  }

  public ComparisonConstraint(Symbol constraintType,
      Symbol comparisonOperator, Integer value) {
    this.constraintType = constraintType;
    this.comparisonOperator = comparisonOperator;
    this.value = value;
  }
}
