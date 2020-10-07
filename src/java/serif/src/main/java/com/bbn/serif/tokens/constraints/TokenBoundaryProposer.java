package com.bbn.serif.tokens.constraints;

import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

/**
 * Provides a {@code Set} of OffsetGroupRanges as proposed Token boundaries given some text. Only
 * the CharOffsets are used, but future implementations reserve the right to change this contract.
 *
 * The {@code Tokenizer} is free to ignore these proposed boundaries by default.
 */
@Beta
public interface TokenBoundaryProposer {

  ImmutableSet<OffsetGroupRange> offsetsForText(final LocatedString text);

}
