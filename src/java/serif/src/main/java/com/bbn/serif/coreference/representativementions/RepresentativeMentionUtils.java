package com.bbn.serif.coreference.representativementions;

import com.bbn.nlp.languages.LanguageSpecific;
import com.bbn.serif.theories.Mention;

import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;


/**
 * Beware! Someday this will probably be refactored into an object instead of a static thing and
 * your code will break. Use at your own risk!
 */
@Beta
public class RepresentativeMentionUtils {

  private RepresentativeMentionUtils() {
    throw new UnsupportedOperationException();
  }

  // the code for handling demonyms should probably be shifted to something more general
  // use dependency injection
  private static final URL COUNTRIES_URL = Resources.getResource(
      "com/bbn/serif/coreference/representativementions/nationality-canonical-names");
  private static final ImmutableSet<String> countries = initializeCountries();

  // we pull this into a method because a static initializer can't throw
  // a checked exception
  private static ImmutableSet<String> initializeCountries() {
    try {
      return loadCountries(Resources.asCharSource(COUNTRIES_URL, Charsets.UTF_8));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  // should inject a locale for lowercasing
  @LanguageSpecific("locale")
  public static boolean isCountry(final Mention m) {
    return countries.contains(m.span().tokenizedText().utf16CodeUnits().toLowerCase());
  }

  private static ImmutableSet<String> loadCountries(final CharSource countryData)
      throws IOException {
    final ImmutableSet.Builder<String> ret = ImmutableSet.builder();
    final Splitter onColon = Splitter.on(":");

    int lineNo = 0;
    for (final String line : countryData.readLines()) {
      try {
        if (!(line.isEmpty() || line.startsWith("#"))) {
          // lines have format
          // demonym:country
          ret.add(Iterables.get(onColon.split(line), 1).toLowerCase());
        }
      } catch (final Exception e) {
        throw new RuntimeException(
            String.format("Error parsing line %d of country file: %s", lineNo, line), e);
      }
      ++lineNo;
    }
    return ret.build();
  }

  public static final Function<Mention, Boolean> IsCountry = new Function<Mention, Boolean>() {
    @Override
    public Boolean apply(final Mention m) {
      return isCountry(m);
    }
  };
  // recall true is ordered as greater than false in Java
  public static final Ordering<Mention> PreferCountries = Ordering.natural()
      .onResultOf(IsCountry);
}
