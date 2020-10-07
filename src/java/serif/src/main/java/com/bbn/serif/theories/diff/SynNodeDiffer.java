package com.bbn.serif.theories.diff;

import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.serif.theories.SynNode;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import javax.inject.Inject;

/**
 * Determines the difference between two {@link SynNode}s. Currently just compares dominated token strings!
 */
@Beta
final class SynNodeDiffer implements Differ<SynNode, Difference<SynNode>> {

  @Inject
  private SynNodeDiffer() {

  }


  @Override
  public Optional<Difference<SynNode>> diff(final SynNode left, final SynNode right) {
    final UnicodeFriendlyString leftTokenString = left.span().tokenizedText();
    final UnicodeFriendlyString rightTokenString = right.span().tokenizedText();

    if (leftTokenString.equals(rightTokenString)) {
      return Optional.absent();
    } else {
      return Optional.of(AtomicDiff.fromLeftRight(left, right));
    }
  }
}
