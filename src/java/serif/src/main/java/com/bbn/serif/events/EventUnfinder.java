package com.bbn.serif.events;

import com.bbn.serif.driver.AbstractConstraintlessProcessingStep;
import com.bbn.serif.driver.ProcessingStep;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.DocumentEventArguments;
import com.bbn.serif.theories.DocumentEvents;
import com.bbn.serif.theories.EventMentions;
import com.bbn.serif.theories.Events;
import com.bbn.serif.theories.SentenceTheory;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import java.io.IOException;

import javax.inject.Inject;

import static com.bbn.serif.SerifEnvironmentM.preprocessorBindingKey;

/**
 * Deletes all {@link com.bbn.serif.theories.SentenceTheory} and {@link DocTheory} level events
 */
public final class EventUnfinder extends AbstractConstraintlessProcessingStep {

  @Inject
  private EventUnfinder() {

  }

  @Override
  public DocTheory process(final DocTheory docTheory) throws IOException {
    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();
    for(final SentenceTheory st: docTheory.sentenceTheories()) {
      ret.replacePrimarySentenceTheory(st, st.withEventMentions(EventMentions.absent()));
    }
    ret.events(Events.absent());
    ret.documentEventArguments(DocumentEventArguments.absent());
    ret.documentEvents(DocumentEvents.absent());
    return ret.build();
  }

  public static final class AsPreprocessorModule extends AbstractModule {

    @Override
    protected void configure() {
      final Multibinder<ProcessingStep> processingStepMultibinder =
          Multibinder.newSetBinder(binder(), preprocessorBindingKey());
      processingStepMultibinder.addBinding().to(EventUnfinder.class);
    }
  }
}
