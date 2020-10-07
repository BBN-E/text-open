package com.bbn.serif.patterns2;

import com.bbn.bue.common.SExpression;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

/**
 * Placeholder. Issue #338.
 */
@Beta
interface ScoreGroup {}

/**
 * Placeholder. Issue #338.
 */
@Beta
interface ScoreFunction {}

/**
 * A pattern which can match against Serif structures. See Brandy Pattern Language documentation.
 */
@Beta
public interface Pattern {

  /**
   * A unique name by which the pattern can be referenced by another pattern. These may not contain
   * spaces and by convention are written in ALLCAPS.
   */
  Optional<Symbol> shortcut();

  /**
   * A value (typically between 0 and 1) that expresses the system’s confidence in a particular
   * pattern.
   */
  Optional<Double> score();


  /**
   * An alternative, coarser-grained method for expressing the system’s confidence in a pattern.
   * Typically the lower the score group, the more reliable the pattern.
   */
  Optional<ScoreGroup> scoreGroup();

  /**
   * A method for combining the scores of subpatterns.
   */
  Optional<ScoreFunction> scoreFunction();

  Optional<Symbol> patternLabel();

  SExpression toSexpression();
}


