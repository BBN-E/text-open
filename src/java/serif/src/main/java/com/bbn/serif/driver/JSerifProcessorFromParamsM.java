package com.bbn.serif.driver;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;

import com.google.common.io.CharSource;
import com.google.inject.Provides;

import java.io.IOException;

import static com.google.common.collect.Iterables.transform;

final class JSerifProcessorFromParamsM extends JSerifProcessorM {

  @Provides
  public Iterable<CharSource> getInputs(Parameters params) throws IOException {
    return transform(FileUtils.loadFileList(params.getExistingFile(InputListP.param)),
        FileUtils.asUTF8CharSourceFunction());
  }

}
