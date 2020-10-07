package com.bbn.serif.theories.diff;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;

import com.google.common.base.Optional;

/**
 * Difference between two event mention arguments. Currently not used.
 */
interface EventMentionArgDiff<T extends EventMention.Argument> extends Difference<T> {
  Optional<Difference<Symbol>> roleDiff();
  Optional<Difference<Float>> scoreDiff();
}
