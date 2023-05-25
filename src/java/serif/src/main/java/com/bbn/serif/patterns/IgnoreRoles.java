package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class IgnoreRoles {

    // all pattern generation will ignore these roles
    static final Set<Symbol> ROLES_TO_IGNORE =
            new HashSet<>(Arrays.asList(
                    Symbol.from("punct"),
                    Symbol.from("det"),
                    Symbol.from("vocative"),
                    Symbol.from("expl"),
                    Symbol.from("advmod"),
                    Symbol.from("cc")
            ));

    static final Set<Symbol> UNARY_EVENT_ROLES_TO_IGNORE =
            new HashSet<>(Arrays.asList(
                    Symbol.from("mark"),
                    Symbol.from("aux"),
                    Symbol.from("aux:pass"),
                    Symbol.from("case"),
                    Symbol.from("conj"),
                    Symbol.from("list"),
                    Symbol.from("appos"),
                    Symbol.from("cop")
            ));

    static final Set<Symbol> BINARY_EVENT_ROLES_TO_IGNORE =
            new HashSet<>(
                    Arrays.asList(
                            Symbol.from("compound"),
                            Symbol.from("compound:prt"),
                            Symbol.from("amod")
                    ));

    static final Set<Symbol> BINARY_ENTITY_ROLES_TO_IGNORE =
            new HashSet<>(
            );

    static final Set<Symbol> ARG_ATTACHMENT_ROLES_TO_IGNORE =
            new HashSet<>(
            );



}
