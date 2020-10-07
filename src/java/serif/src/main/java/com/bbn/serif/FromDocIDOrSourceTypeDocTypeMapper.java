package com.bbn.serif;

import com.bbn.bue.common.serialization.jackson.ImmutableMapProxy;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.DocTheory;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Qualifier;

import static com.google.common.collect.Iterables.concat;

/**
 * A strategy for mapping documents to document types based on (a) a given set of source type
 * mappings and (b) a collection of regular expressions to match against document IDs.  If neither
 * works, it falls back on using the source type if available and crashing otherwise.
 */
public final class FromDocIDOrSourceTypeDocTypeMapper implements DocTypeMapper {

  private static final Logger log =
      LoggerFactory.getLogger(FromDocIDOrSourceTypeDocTypeMapper.class);

  private final ImmutableMap<String, Symbol> docIDRegexesToDocType;
  private final ImmutableMap<Pattern, Symbol> compiledDocIDRegexesToDocType;
  private final ImmutableMap<Symbol, Symbol> sourceTypeMap;

  @Inject
  FromDocIDOrSourceTypeDocTypeMapper(
      @DocIDToSourceTypeRegexesP
      final Map<String, Symbol> docIDRegexesToDocType,
      @SourceTypeMapP
      final Map<Symbol, Symbol> sourceTypeMap) {
    this.docIDRegexesToDocType = ImmutableMap.copyOf(docIDRegexesToDocType);
    this.sourceTypeMap = ImmutableMap.copyOf(sourceTypeMap);

    // we cache the compilation of the regular expressions
    final ImmutableMap.Builder<Pattern, Symbol> compiledDocIdRegexesToDocType =
        ImmutableMap.builder();
    for (final Map.Entry<String, Symbol> e : docIDRegexesToDocType.entrySet()) {
      compiledDocIdRegexesToDocType.put(
          Pattern.compile(e.getKey(), Pattern.CASE_INSENSITIVE), e.getValue());
    }
    this.compiledDocIDRegexesToDocType = compiledDocIdRegexesToDocType.build();
  }

  /**
   * This is mostly for use in tests; otherwise prefer dependency injection
   */
  public static DocTypeMapper fromMaps(final Map<String, Symbol> docIDRegexesToDocType,
      final Map<Symbol, Symbol> sourceTypeMap) {
    return new FromDocIDOrSourceTypeDocTypeMapper(docIDRegexesToDocType, sourceTypeMap);
  }

  // this is fancy - we provide injection points so the user can augmented the source type map
  // and docID ---> sourceType regular expressions at runtime
  @JsonCreator
  @SuppressWarnings("deprecation")
  static FromDocIDOrSourceTypeDocTypeMapper createFromJson(
      @JsonProperty("docIdRegexesToDocType") Map<String, Symbol> serializedDocIDRegexesToDocType,
      @JacksonInject("augmentDocIdToDocType") @DocIDToSourceTypeRegexesP Map<String, Symbol> runtimeDocIdToDocTypeAugmentations,
      @JsonProperty("sourceTypeMap") ImmutableMapProxy<Symbol, Symbol> serializedSourceTypeMap,
      @JacksonInject("sourceTypeMap") @SourceTypeMapP Map<Symbol, Symbol> runtimeSourceTypeMapAugmentations) {
    return new FromDocIDOrSourceTypeDocTypeMapper(
        mergeSerializedAndRuntimeMaps(serializedDocIDRegexesToDocType,
            runtimeDocIdToDocTypeAugmentations),
        mergeSerializedAndRuntimeMaps(serializedSourceTypeMap.toImmutableMap(),
            runtimeSourceTypeMapAugmentations));
  }

  @Override
  public Symbol mapSourceType(final DocTheory dt) {
    // if we have a source type and can map it, that's best
    if (dt.sourceType().isPresent()) {
      final Symbol rawSourceType = dt.sourceType().get();
      if (sourceTypeMap.containsKey(rawSourceType)) {
        return sourceTypeMap.get(rawSourceType);
      } else {
        log.warn(
            "Source type " + rawSourceType + " was present but could not be mapped to a doc type. "
                + "Known mappings are " + sourceTypeMap);
      }
    }

    // failing that try to map the doc ID
    for (final Map.Entry<Pattern, Symbol> e : compiledDocIDRegexesToDocType.entrySet()) {
      final Pattern sourceTypePattern = e.getKey();
      if (sourceTypePattern.matcher(dt.docid().asString()).matches()) {
        return e.getValue();
      }
    }

    // finally try the (unmapped) source type
    if (dt.sourceType().isPresent()) {
      return dt.sourceType().get();
    }

    throw new SerifException("Cannot map docID to doc type. Doc ID is " + dt.docid() +
        ".\nKnown mappings are " + docIDRegexesToDocType);
  }

  private static <K, V> Map<K, V> mergeSerializedAndRuntimeMaps(
      final Map<K, V> serialized, final Map<K, V> runtime) {
    // we use both a mutable and immutable copy to maintain determinism
    final Map<K, V> mutableMap = new HashMap<>();
    final ImmutableMap.Builder<K, V> ret = ImmutableMap.builder();

    // use runtime mappings first, then serialized mappings, so in case of conflicts
    // runtime mappings will be used. We give runtime mappings priority because we assume
    // the user at the time of application knows better, especially when adapting some generic
    // model
    final Iterable<Map.Entry<K, V>> mappingEntryOrder =
        concat(serialized.entrySet(), runtime.entrySet());

    for (final Map.Entry<K, V> e : mappingEntryOrder) {
      final V curMapping = mutableMap.get(e.getKey());

      if (curMapping == null) {
        mutableMap.put(e.getKey(), e.getValue());
        ret.put(e.getKey(), e.getValue());
      } else //noinspection StatementWithEmptyBody
        if (curMapping.equals(e.getValue())) {
        // redundant entry, do nothing
      } else {
        log.warn("Ignoring conflicting doc type mapping: {} --> {} (keeping: {} ---> {})",
            e.getKey(), e.getValue(), e.getKey(), curMapping);
      }
    }

    return ret.build();
  }

  // for serialization
  @JsonProperty("docIdRegexesToDocType")
  Map<String, Symbol> docIDRegexesToDocType() {
    // don't need a proxy because this has string keys anyway
    return docIDRegexesToDocType;
  }

  @JsonProperty("sourceTypeMap")
  @SuppressWarnings("deprecation")
  ImmutableMapProxy<Symbol, Symbol> sourceTypeMap() {
    return ImmutableMapProxy.forMap(sourceTypeMap);
  }

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface DocIDToSourceTypeRegexesP {

    String param = "com.bbn.serif.docTypeMapping.docIDRegexesFile";
  }

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface SourceTypeMapP {

    String param = "com.bbn.serif.docTypeMapping.sourceTypeMapFile";
  }

}

