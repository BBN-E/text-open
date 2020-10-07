package com.bbn.serif.theories;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In many situations you need to carry around a DocTheory along with some object (since knowledge
 * generally does not "flow up" in JSerif. This class allows you to do that. Equality and hashcode
 * are computed on the contained item and doctheory together.
 *
 * @author rgabbard
 */
public final class WithDocTheory<T> {

  private WithDocTheory(T item, DocTheory dt) {
    this.item = checkNotNull(item);
    this.docTheory = checkNotNull(dt);
  }

  public static <T> WithDocTheory<T> from(T item, DocTheory dt) {
    return new WithDocTheory<T>(item, dt);
  }

  public static <T> Function<T, WithDocTheory<T>> associateWith(final DocTheory dt) {
    return new Function<T, WithDocTheory<T>>() {
      @Override
      public WithDocTheory<T> apply(T item) {
        return from(item, dt);
      }
    };
  }

  @Nonnull
  public T item() {
    return item;
  }

  public DocTheory docTheory() {
    return docTheory;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(docTheory, item);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    @SuppressWarnings("rawtypes")
    WithDocTheory other = (WithDocTheory) obj;
    if (docTheory == null) {
      if (other.docTheory != null) {
        return false;
      }
    } else if (!docTheory.equals(other.docTheory)) {
      return false;
    }
    if (item == null) {
      if (other.item != null) {
        return false;
      }
    } else if (!item.equals(other.item)) {
      return false;
    }
    return true;
  }

  private final T item;
  private final DocTheory docTheory;

  public static <T> Function<WithDocTheory<T>, T> Item() {
    return new Function<WithDocTheory<T>, T>() {
      @Override
      public T apply(WithDocTheory<T> input) {
        return input.item();
      }
    };
  }

  /**
   * Concerts a function from T-->V to a function from WithDocTheory<T> to WithDocTheory<V>.
   */
  public static <T, V> Function<WithDocTheory<T>, WithDocTheory<V>>
  wrapFunction(final Function<T, V> f) {
    return new Function<WithDocTheory<T>, WithDocTheory<V>>() {
      @Override
      public WithDocTheory<V> apply(final WithDocTheory<T> input) {
        return WithDocTheory.from(f.apply(input.item()), input.docTheory());
      }

      @Override
      public String toString() {
        return "WithDocTheory.wrap(" + f + ")";
      }
    };
  }

  /**
   * Guava {@link Function} to convert a {@link DocTheory} into an {@link Iterable} of {@link
   * SentenceTheory}s linked to the {@link DocTheory}.
   *
   * This will skip empty sentences.
   */
  public static Function<DocTheory, Iterable<WithDocTheory<SentenceTheory>>> toWrappedSentencesFunction() {
    return ToWrappedSentences.INSTANCE;
  }


  private enum ToWrappedSentences
      implements Function<DocTheory, Iterable<WithDocTheory<SentenceTheory>>> {
    INSTANCE;

    @Override
    public Iterable<WithDocTheory<SentenceTheory>> apply(final DocTheory input) {
      return Iterables.transform(input.nonEmptySentenceTheories(),
          WithDocTheory.<SentenceTheory>associateWith(input));
    }

    @Override
    public String toString() {
      return "toWrappedSentences()";
    }
  }
}
