package com.bbn.serif.driver;

import com.bbn.serif.io.SerifXMLFilesToDirectorySinkM;

import com.google.inject.AbstractModule;


/**
 * A module to load configuration for a JSerifProcessor pipeline.
 */
class JSerifProcessorM extends AbstractModule {

  @Override
  protected void configure() {
    install(new SerifXMLFilesToDirectorySinkM());
    // this appears half-completed and this line causes a problematic dependency
    // so I'm commenting it out
//    install(new CSerifWorkerM());
  }

}
