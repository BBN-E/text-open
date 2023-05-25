package com.bbn.serif.languages;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.ModuleUtils;
import com.bbn.bue.common.SerifLocale;
import com.bbn.bue.common.StringNormalizer;
import com.bbn.bue.common.collections.MapUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.languages.Language;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import static com.bbn.bue.common.StringNormalizers.asFunction;
import static com.bbn.bue.common.StringNormalizers.toLowercase;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

/**
 * Extends {@link Language} with Serif-specific capabilities.
 *
 * <h1>Configuring the Global Serif Language</h1>
 *
 * Some Serif programs don't care what language they are running in.  In this case the global
 * Serif language (i.e. a binding to {@link SerifLanguage} does not need to be specified.
 *
 * At other times, code will use information about the global Serif language is available but
 * does not require it. Such code should inject {@code Optional<SerifLanguage>}.
 *
 * Code which depends on the the global Serif language should inject {@link SerifLanguage} directly.
 * If the language has not been specified, there will be a failure during Guice injection.
 * However, we will catch this error and provide a user-friendly error message in
 * {@link com.bbn.bue.common.TextGroupEntryPoints#runEntryPoint(Class, Class, String[])}.
 *
 * There are two ways the global language can be bound.  Most commonly the user specifies it by
 * providing the  {@code com.bbn.serif.language} parameter. Values can be an
 * ISO 639-1 code, ISO 639-2 code, or a long name.   In theory we could have multiple
 * {@link SerifLanguage} implementations for one ISO code (e.g. for different orthographies).
 * If we add such cases in the future we should provide an additional means of specification.
 * The global language can also be bound in code by calling the static method
 * {@link SerifLanguage.Module#setSerifLanguageTo(Binder, SerifLanguage)}.
 *
 * For multilingual programs where the idea of a single global language is not applicable,
 * {@link SerifLanguage.Module} also exposes name ot language mappings directly. See
 * {@link SerifLanguage.Module}.
 *
 * <h2>Adding a new language</h2>
 *
 * If you want to add a new language, first make a {@link SerifLanguage} implementation
 * using an existing language as a model. Then register it in
 * {@link Module.LanguageMappingsModule#configure()}.
 *
 */
public abstract class SerifLanguage {
  private static final Logger log = LoggerFactory.getLogger(SerifLanguage.class);

  private final Language language;

  @JsonCreator
  protected SerifLanguage(@JsonProperty("language") Language language) {
    this.language = checkNotNull(language);
  }

  @JsonProperty("language")
  public final Language language() {
    return language;
  }

  public boolean isVerbExcludingModals(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isNoun(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isModalVerb(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean canBeAuxillaryVerb(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isCopula(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isPrepositionalPhrase(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isPreposition(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isParticle(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public boolean isAdverb(SynNode node) {
    throw new UnsupportedOperationException();
  }

  public Optional<Symbol> getDeterminer(final SynNode n) {
    throw new UnsupportedOperationException();
  }

  public Optional<Symbol> findDeterminerForNoun(SynNode n) {
    throw new UnsupportedOperationException();
  }

  // TODO
  // The language classes should be refactored to do
  // Optional<PluralityDetector> getPluralityDetector()
  public boolean isPlural(Mention m) {
    throw new UnsupportedOperationException();
  }

  public boolean preterminalIsNumber(SynNode n) {
    throw new UnsupportedOperationException();
  }

  public boolean isQuestion(SentenceTheory st) {
    throw new UnsupportedOperationException();
  }

  /**
   * Hashing on the class is sufficient, since all implementations are required to be singletons.
   */
  @Override
  public final int hashCode() {
    return this.getClass().hashCode();
  }

  /**
   * Equality by class is sufficient, since all implementations are required to be singletons.
   */
  @Override
  public final boolean equals(Object o) {
    return o!=null && getClass().equals(o.getClass());
  }

  /**
   * Looks up a {@link SerifLanguage} from some key.  Language lookups are always
   * case-insensitive on the key, with lowercasing done using the standard US English locale.
   *
   * We have a special class for this due to the lowercasing behavior and the desirability of
   * throwing an exception on an unregistered language.
   */
  public static class SerifLanguageMap {
    private final ImmutableMap<String, SerifLanguage> nameToLanguage;

    private SerifLanguageMap(Map<String, SerifLanguage> nameToLanguage) {
      this.nameToLanguage = ImmutableMap.copyOf(nameToLanguage);
    }

    private static final StringNormalizer LOWERCASE =
        toLowercase(SerifLocale.forLocaleString("en-US"));

    public static SerifLanguage.SerifLanguageMap forKeyMap(Map<String, SerifLanguage> nameToLanguage) {
      // if two languages have keys differing only in case, crashing is good.
      return new SerifLanguage.SerifLanguageMap(MapUtils.copyWithKeysTransformedByInjection(
          nameToLanguage, asFunction(LOWERCASE)));
    }


    public SerifLanguage languageFor(String key) {
      final SerifLanguage ret = nameToLanguage.get(LOWERCASE.normalize(key));
      if (ret != null) {
        return ret;
      } else {
        throw new Language.UnregisteredLanguageException("No Serif language registered for : " + key);
      }
    }
  }

  /**
   * Binds a {@link Set} of all registered {@link SerifLanguage}s and the global default
   * {@link SerifLanguage}, as well as {@link SerifLanguage.ByLongNamesP},
   * {@link SerifLanguage.ByISO639Dash1CodeP}, and {@link SerifLanguage.ByISO639Dash2CodeP}.
   */
  public static class Module extends AbstractParameterizedModule {

    public Module(final Parameters parameters) {
      super(parameters);
    }

    public static final String SERIF_LANGUAGE_PARAM = "com.bbn.serif.language";

    /**
     * Use this to set the global {@link SerifLanguage} (the one you get it you inject
     * {@link SerifLanguage} with no qualifiers.
     */
    public static void setSerifLanguageTo(Binder binder, SerifLanguage serifLanguage) {
      OptionalBinder.newOptionalBinder(binder, SerifLanguage.class).setBinding()
          .toInstance(serifLanguage);
      OptionalBinder.newOptionalBinder(binder, Language.class).setBinding()
          .toInstance(serifLanguage.language());
    }

    @Override
    protected void configure() {
      // declare optional binding sites for Language and SerifLanguage
      OptionalBinder.newOptionalBinder(binder(), SerifLanguage.class);
      OptionalBinder.newOptionalBinder(binder(), Language.class);

      install(new LanguageMappingsModule(params()));

      // we want to allow the user to specify the language using a language name or ISO code
      // in place of a module. However, this is difficult because the mappings are not
      // constructed until when the injector is instantiated. Therefore we create another
      // injector here which has the mappings bound and use it to look up what language module
      // to install
      final Optional<String> language = params().getOptionalString(SERIF_LANGUAGE_PARAM);

      // if the user specified a long name or ISO 639 code for the language parameter,
      // we bind it globally
      if (language.isPresent()) {
        // bind the global default language
        setSerifLanguageTo(binder(), languageStringToLanguage(language.get()));
      }
      // not specifying the language is fine so long as the language is not used anywhere.
      // If it is a Guice binding failure exception will be thrown.  Because this problem is
      // likely to be common, we will capture this exception and provide a friendly error message
      // in TextGroupEntryPoints
    }

    // first try to resolve the user's language string as a full language name, then
    // as an ISO 639-2 code, then as an ISO 639-1 code
    private SerifLanguage languageStringToLanguage(final String language) {
      final Injector innerInjector = Guice.createInjector(new LanguageMappingsModule(params()));
      final SerifLanguageMap byLongNames =
          innerInjector.getInstance(Key.get(SerifLanguageMap.class, ByLongNamesP.class));
      final ImmutableMultimap<String, SerifLanguage> byIso6392 = innerInjector
          .getInstance(Key.get(new TypeLiteral<ImmutableMultimap<String, SerifLanguage>>() {
                               },
              ByISO639Dash2CodeP.class));
      final ImmutableMultimap<String, SerifLanguage> byIso6391 = innerInjector
          .getInstance(Key.get(new TypeLiteral<ImmutableMultimap<String, SerifLanguage>>() {
                               },
              ByISO639Dash1CodeP.class));

      if (byLongNames.nameToLanguage.containsKey(language.toLowerCase(Locale.ENGLISH))) {
        return byLongNames.languageFor(language);
      } else {
        final ImmutableCollection<SerifLanguage> iso6392Lookup = byIso6392.get(language);
        if (iso6392Lookup.size() == 1) {
          return getOnlyElement(iso6392Lookup);
        } else if (iso6392Lookup.size() > 1) {
          throw new SerifException("Multiple language mappings by ISO 639-2 for " + language
              + " so you must specify the language using its long name");
        } else {
          final ImmutableCollection<SerifLanguage> iso6391Lookup = byIso6391.get(language);
          if (iso6391Lookup.size() == 1) {
            return getOnlyElement(iso6391Lookup);
          } else if (iso6392Lookup.size() > 1) {
            throw new SerifException("Multiple language mappings by ISO 639-1 for " + language
                + " so you must specify the language using its long name or ISO 639-2 code");
          } else {
            throw new SerifException(
                "Unknown Serif language " + language + ". Known languages by full "
                    + "name are " + byLongNames.nameToLanguage.keySet() + ","
                    + "by ISO 639-2 " + byIso6392.keySet() + ", and by ISO 639-1 " + byIso6391
                    .keySet() + ".");
          }
        }
      }
    }

    // these are pulled into their own module because we will need them in the injector
    // we create within SerifLanguage.Module#configure
    private static class LanguageMappingsModule extends AbstractParameterizedModule {

      protected LanguageMappingsModule(final Parameters parameters) {
        super(parameters);
      }

      @Override
      protected void configure() {
        // register all known languages
        registerLanguage(binder(),
                // AmharicSerifLanguage.class, ArabicSerifLanguage.class,
            ChineseSerifLanguage.class, EnglishSerifLanguage.class, SpanishSerifLanguage.class
                /*
                , FarsiSerifLanguage.class,
            HausaSerifLanguage.class, HungarianSerifLanguage.class, RussianSerifLanguage.class,
            SomaliSerifLanguage.class, SpanishSerifLanguage.class, TurkishSerifLanguage.class,
            UyghurSerifLanguage.class, UzbekSerifLanguage.class, VietnameseSerifLanguage.class,
            YorubaSerifLanguage.class, TigrinyaSerifLanguage.class, OromoSerifLanguage.class
            */
                );
      }

      @SafeVarargs
      public final void registerLanguage(Binder binder,
          Class<? extends SerifLanguage> serifLanguage1,
          Class<? extends SerifLanguage>... serifLanguages) {
        final Multibinder<SerifLanguage> multibinder =
            Multibinder.newSetBinder(binder, SerifLanguage.class);
        try {
          multibinder.addBinding().to(serifLanguage1).in(Scopes.SINGLETON);
          install(ModuleUtils.classNameToModule(params(), serifLanguage1));
          for (final Class<? extends SerifLanguage> serifLanguage : serifLanguages) {
            install(ModuleUtils.classNameToModule(params(), serifLanguage));
            multibinder.addBinding().to(serifLanguage).in(Scopes.SINGLETON);
          }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
          addError(e);
        }
      }

      @Provides
      @Singleton
      @SerifLanguage.ByLongNamesP
      SerifLanguageMap byLongNames(Set<SerifLanguage> registeredLanguages) {
        final ImmutableMap.Builder<String, SerifLanguage> ret = ImmutableMap.builder();

        for (final SerifLanguage registeredLanguage : registeredLanguages) {
          ret.put(registeredLanguage.language().longName(), registeredLanguage);
        }

        return SerifLanguageMap.forKeyMap(ret.build());
      }

      @Provides
      @Singleton
      @SerifLanguage.ByISO639Dash1CodeP
      ImmutableMultimap<String, SerifLanguage> byISO639Dash1Code(
          Set<SerifLanguage> registeredLanguages) {
        final ImmutableMultimap.Builder<String, SerifLanguage> ret = ImmutableMultimap.builder();

        for (final SerifLanguage registeredLanguage : registeredLanguages) {
          ret.put(registeredLanguage.language().iso639Dash1Code().asString(), registeredLanguage);
        }

        return ret.build();
      }

      @Provides
      @Singleton
      @SerifLanguage.ByISO639Dash2CodeP
      ImmutableMultimap<String, SerifLanguage> byISO639Dash2Code(
          Set<SerifLanguage> registeredLanguages) {
        final ImmutableMultimap.Builder<String, SerifLanguage> ret = ImmutableMultimap.builder();

        for (final SerifLanguage registeredLanguage : registeredLanguages) {
          ret.put(registeredLanguage.language().iso639Dash2Code().asString(), registeredLanguage);
        }

        return ret.build();
      }
    }
  }

  /**
   * {@link SerifLanguage.Module} will bind a {@link SerifLanguageMap} annotated with this mapping
   * from long language names to the corresponding language objects.
   */
  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ByLongNamesP {}

  /**
   * {@link SerifLanguage.Module} will bind an {@link com.google.common.collect.ImmutableMultimap} annotated with these mapping
   * from ISO 639-1 strings (https://en.wikipedia.org/wiki/ISO_639-1) to the corresponding
   * {@link SerifLanguage} objects.
   */
  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ByISO639Dash1CodeP {}

  /**
   * {@link SerifLanguage.Module} will bind an {@link com.google.common.collect.ImmutableMultimap} annotated with these mapping
   * from ISO 639-2 strings (https://en.wikipedia.org/wiki/ISO_639-2) to the corresponding
   * {@link SerifLanguage} objects.
   */
  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ByISO639Dash2CodeP {}

  /**
   *@deprecated Prefer to use Guice injection. This is provided for backwards compatibility only
   * and will not be updated for new languages.
   */
  @Deprecated
  public static SerifLanguageMap byLongNames() {
    log.warn("Locating Serif language objects using deprecated method byLongNames. This is provided"
        + " for backwards compatability only. Prefer"
        + " to use Guice injection.");
    return SerifLanguageMap.forKeyMap(ImmutableMap.of(
        "english", EnglishSerifLanguage.getInstance(),
        "chinese", ChineseSerifLanguage.getInstance(),
        "spanish", SpanishSerifLanguage.getInstance(),
        "arabic", ArabicSerifLanguage.getInstance(),
            "farsi", FarsiSerifLanguage.getInstance()
            ));
  }

  public final Predicate<SynNode> isPrepositionPredicate() {
    return new Predicate<SynNode>() {
      @Override
      public boolean apply(final SynNode node) {
        return isPreposition(node);
      }
    };
  }

  public final Predicate<SynNode> isAdverbPredicate() {
    return new Predicate<SynNode>() {
      @Override
      public boolean apply(final SynNode node) {
        return isAdverb(node);
      }
    };
  }

  public final Predicate<SynNode> isParticlePredicate() {
    return new Predicate<SynNode>() {
      @Override
      public boolean apply(final SynNode node) {
        return isParticle(node);
      }
    };
  }
}
