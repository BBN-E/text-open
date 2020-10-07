package com.bbn.serif;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * This binds {@link DocTypeMapper} to a {@link FromDocIDOrSourceTypeDocTypeMapper} which will use
 * any mapping specified in {@code com.bbn.serif.docTypeMapping.sourceTypeMapFile} (tab-separated
 * mappings of source type attributes) or {@oce com.bbn.serif.docTypeMapping.docIDRegexesFile}
 * (tab-separate; regular expression on the document ID then mapped source type).
 */
public class DefaultDocTypeMapperM extends AbstractModule {

  private static final Logger log = LoggerFactory.getLogger(DefaultDocTypeMapperM.class);

  @Override
  protected void configure() {
    bind(DocTypeMapper.class).to(FromDocIDOrSourceTypeDocTypeMapper.class);
  }

  @Provides
  @FromDocIDOrSourceTypeDocTypeMapper.DocIDToSourceTypeRegexesP
  Map<String, Symbol> getDocIdToSourceTypeRegexes(Parameters params) throws IOException {
    if (params.isPresent(FromDocIDOrSourceTypeDocTypeMapper.DocIDToSourceTypeRegexesP.param)) {
      final CharSource source = Files.asCharSource(params.getExistingFile(
          FromDocIDOrSourceTypeDocTypeMapper.DocIDToSourceTypeRegexesP.param), Charsets.UTF_8);
      final ImmutableMap<String, Symbol> ret = loadTypeMapFrom(source);
      log.info("Loaded {} regexs to extract doc types from document IDs", ret.size());
      return ret;
    } else {
      log.info("No regexes found for extracting doc types from document IDs, using defaults");
      return loadTypeMapFrom(
          Resources.asCharSource(Resources.getResource(DefaultDocTypeMapperM.class,
              "docIdRegexes.default.txt"), Charsets.UTF_8));
    }

  }

  private ImmutableMap<String, Symbol> loadTypeMapFrom(final CharSource source) throws IOException {
    return ImmutableMap.copyOf(Maps.transformValues(
        FileUtils.loadStringMap(source),
        SymbolUtils.symbolizeFunction()));
  }

  @Provides
  @FromDocIDOrSourceTypeDocTypeMapper.SourceTypeMapP
  Map<Symbol, Symbol> getSourceTypeMap(Parameters params) throws IOException {
    if (params.isPresent(FromDocIDOrSourceTypeDocTypeMapper.SourceTypeMapP.param)) {
      final ImmutableMap<Symbol, Symbol> ret =
          FileUtils.loadSymbolMap(Files.asCharSource(
              params.getExistingFile(FromDocIDOrSourceTypeDocTypeMapper.SourceTypeMapP.param),
              Charsets.UTF_8));
      log.info("Loaded {} source-type-to-doc-type mappings", ret.size());
      return ret;
    } else {
      log.info("No source-type-to-doc-type mappings found, using defaults");
      return FileUtils
          .loadSymbolMap(Resources.asCharSource(Resources.getResource(DefaultDocTypeMapperM.class,
              "sourceTypeMap.default.txt"), Charsets.UTF_8));
    }
  }
}
