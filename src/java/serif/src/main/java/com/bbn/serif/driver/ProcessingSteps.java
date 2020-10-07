package com.bbn.serif.driver;

import com.bbn.bue.common.annotations.MoveToBUECommon;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;

/**
 * Utility methods for working with {@link ProcessingSteps}.
 */
public final class ProcessingSteps {
  private ProcessingSteps() {
    throw new UnsupportedOperationException();
  }

  @Beta
  public static Function<DocTheory, DocTheory> composeToFunctionFirstIsInnermost(
      Iterable<? extends ProcessingStep> processingSteps) {
    return composeFirstIsInnermost(Iterables.transform(processingSteps, asFunctionFunction()));
  }

  public static Function<ProcessingStep, Function<DocTheory, DocTheory>> asFunctionFunction() {
    return AsFunctionFunction.INSTANCE;
  }

  @MoveToBUECommon
  private static <T> Function<T,T> composeFirstIsInnermost(final Iterable<? extends Function<? super T, ? extends T>> endomorphisms) {
    return new Function<T, T>() {
      @Nullable
      @Override
      public T apply(final T input) {
        T val = input;
        for (final Function<? super T, ? extends T> endomorphism : endomorphisms) {
          val = endomorphism.apply(val);
        }
        return val;
      }
    };
  }

  private enum AsFunctionFunction implements Function<ProcessingStep, Function<DocTheory, DocTheory>> {
    INSTANCE;

    @Override
    public Function<DocTheory, DocTheory> apply(final ProcessingStep input) {
      return input.asFunction();
    }
  }
}
