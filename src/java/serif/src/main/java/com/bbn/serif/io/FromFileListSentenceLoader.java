package com.bbn.serif.io;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.WithDocTheory;

import com.google.common.base.Function;

import java.io.IOException;

public final class FromFileListSentenceLoader implements TrainingDataSentenceLoader {

  @Override
  public Iterable<WithDocTheory<SentenceTheory>> getRawSentences(Parameters trainingDataParams,
      SerifXMLLoader loader, Function<DocTheory, DocTheory> preprocessor)
      throws IOException {
    return SerifIOUtils.nonEmptySentenceTheoriesFromFiles(
        FileUtils.loadFileList(trainingDataParams.getExistingFile("fileList")),
        loader, preprocessor);
  }
}
