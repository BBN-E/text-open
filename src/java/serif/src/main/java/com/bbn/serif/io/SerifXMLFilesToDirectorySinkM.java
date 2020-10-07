package com.bbn.serif.io;

import com.bbn.bue.common.parameters.Parameters;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSink;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.io.File;
import java.util.Set;

import javax.inject.Singleton;

public class SerifXMLFilesToDirectorySinkM extends AbstractModule {

  @Provides
  public Set<DocTheorySink> getDocTheorySinks(SerifXMLFilesToDirectorySink sink) {
    return ImmutableSet.of((DocTheorySink) sink);
  }

  @Provides
  @OutputDirectoryP
  public File getOutputDirectory(Parameters params) {
    return params.getCreatableDirectory(OutputDirectoryP.param);
  }

  @Provides
  @Singleton
  @SerifXMLFilesToDirectorySink.ProcessedDocListP
  CharSink getProcessedDocListSink(Parameters params) {
    return Files
        .asCharSink(params.getCreatableFile(SerifXMLFilesToDirectorySink.ProcessedDocListP.param),
            Charsets.UTF_8);
  }

  @Provides
  @Singleton
  @SerifXMLFilesToDirectorySink.ProcessedDocMapP
  CharSink getProcessedDocMapSink(Parameters params) {
    return Files
        .asCharSink(params.getCreatableFile(SerifXMLFilesToDirectorySink.ProcessedDocMapP.param),
            Charsets.UTF_8);
  }

  @Override
  protected void configure() {
    bind(DocTheorySink.class).to(SerifXMLFilesToDirectorySink.class);
  }
}
