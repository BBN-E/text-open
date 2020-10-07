package com.bbn.serif.io;

import com.bbn.bue.common.parameters.Parameters;

import com.google.inject.Provides;

public class SerifXMLIOFromParamsM extends SerifXMLIOM {

  @Provides
  @AllowSloppyOffsetsP
  Boolean allowSloppyOffsets(Parameters params) {
    return params.getOptionalBoolean(SerifXMLLoader.ALLOW_SLOPPY_OFFSETS_PARAM).or(false);
  }

  @Provides
  @TranslateForNonZeroOriginalTextStartOffsetsP
  Boolean compatibilityWithDocumentsOffsetIntoSource(Parameters params) {
    return params.getOptionalBoolean("com.bbn.serif.io.translateForNonZeroOriginalTextStartOffsets").or(false);
  }
}
