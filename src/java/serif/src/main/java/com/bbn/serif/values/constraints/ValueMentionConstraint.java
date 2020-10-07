package com.bbn.serif.values.constraints;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

@Beta
public interface ValueMentionConstraint {

  OffsetRange<CharOffset> offsets();

  boolean satisfiedBy(final DocTheory dt);

}
