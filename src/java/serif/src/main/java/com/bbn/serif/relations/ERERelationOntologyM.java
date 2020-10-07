package com.bbn.serif.relations;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.io.IOException;
import java.util.Set;

public class ERERelationOntologyM extends AbstractModule {

  @Override
  protected void configure() {

  }

  @Provides
  @RelationTypesP
  Set<Symbol> relationTypes() throws IOException {
    return SymbolUtils.setFrom(Resources.asCharSource(
        Resources.getResource(ERERelationOntologyM.class, "ere-relation-types.txt"),
        Charsets.UTF_8).readLines());
  }
}
