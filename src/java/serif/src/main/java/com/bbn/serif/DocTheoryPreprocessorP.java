package com.bbn.serif;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Binding point for any preprocessing applied to {@link com.bbn.serif.theories.DocTheory}s
 * before application-specific processing.  For example, we used this in the Lorelei project to
 * perform corrections on the LDC-supplied tokenization before name training and decoding.
 *
 * Just because you bind something here doesn't mean your application will actually use it; you
 * have to check its code or documentation.
 */
@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocTheoryPreprocessorP {}


