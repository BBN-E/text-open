package com.bbn.serif.events;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.serif.relations.RelationTypesP;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.io.IOException;
import java.util.Set;

public class ACEEventOntologyM extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @RelationTypesP
    Set<Symbol> eventTypes() throws IOException {
        return SymbolUtils.setFrom(Resources.asCharSource(
                Resources.getResource(com.bbn.serif.events.ACEEventOntologyM.class, "ace-event-types.txt"),
                Charsets.UTF_8).readLines());
    }
}
