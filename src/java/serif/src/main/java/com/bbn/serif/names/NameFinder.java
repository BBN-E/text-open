package com.bbn.serif.names;


import com.bbn.bue.common.EvalHack;
import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.io.GZIPByteSource;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.serialization.jackson.JacksonSerializationM;
import com.bbn.bue.common.serialization.jackson.JacksonSerializer;
import com.bbn.serif.names.constraints.NameConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;

/**
 *
 * Returns a new {@code DocTheory} which is the same as the input except with names added
 * (technically, added to primary sentence theory beams).
 *
 * Whether or not pre-existing names are kept is implementation-dependent. Each implementation is
 * responsible for checking that its constraints are satisfied and warning or throwing an exception
 * depending on tolerance.
 */
@Beta
public interface NameFinder extends Finishable {

  DocTheory addNames(final DocTheory docTheory);
  DocTheory addNames(final DocTheory docTheory, final Set<NameConstraint> constraints);

  SentenceTheory addNames(final DocTheory docTheory, final SentenceTheory sentenceTheory);

  SentenceTheory addNames(final DocTheory docTheory, final SentenceTheory sentenceTheory,
      final Set<NameConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.names.jacserif.tolerant";
  }

  /**
   * Guice module to obtain a {@link NameFinder} by de-serializing the {@code
   * com.bbn.serif.names.model} parameter.
   */
  class DeserializeModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(DeserializeModule.class);

    public static final String MODEL_PARAM = "com.bbn.serif.names.model";

    @Override
    protected void configure() {
      install(new JacksonSerializationM());
    }

    @Provides
    @Singleton
    // It's serialization, what can you do?
    @SuppressWarnings("unchecked")
    @EvalHack(eval = "lorelei-Y1")
    NameFinder getNameFinder(JacksonSerializer.Builder deserializer, Parameters params) throws
                                                                                        IOException {
      if (params.isPresent(MODEL_PARAM)) {
        final File modelFile = params.getExistingFile(MODEL_PARAM);
        log.info("Loading name finder from {}", modelFile);
        return (NameFinder) deserializer.forJson().build()
            .deserializeFrom(GZIPByteSource.fromCompressed(Files.asByteSource(modelFile)));
      } else {
        log.warn("Running without a name finder model. Are you sure you meant to do that?");
        return NoOpNameFinder.create();
      }
    }
  }
}

