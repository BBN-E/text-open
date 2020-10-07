package com.bbn.serif.ace;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.ValueMention;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ACEEventRole {

  public ACEEventRole(String name, List<String> entityTypesAllowed,
      List<String> valueTypesAllowed) {
    this.name = checkNotNull(name);
    this.entityTypesAllowed = ImmutableList.copyOf(entityTypesAllowed);
    this.valueTypesAllowed = ImmutableList.copyOf(valueTypesAllowed);
  }

  public String name() {
    return name;
  }

  public boolean validCandidate(Mention m) {
    if (validMentionTypes.contains(m.mentionType())) {
      return entityTypesAllowed.contains(m.entityType().name().toString());
    }
    return false;
  }

  public boolean validCandidate(ValueMention vm) {
    return valueTypesAllowed.contains(vm.fullType().baseTypeSymbol().toString());
  }

  private static Set<Mention.Type> validMentionTypes = ImmutableSet.of(
      Mention.Type.NAME, Mention.Type.DESC, Mention.Type.PRON,
      Mention.Type.PART);

  public static ACEEventRole entityOnly(String name, List<String> entityTypes) {
    return new ACEEventRole(name, entityTypes, ImmutableList.<String>of());
  }

  public static ACEEventRole valueOnly(String name, List<String> valueTypes) {
    return new ACEEventRole(name, ImmutableList.<String>of(), valueTypes);
  }

  public static Function<ACEEventRole, Symbol> nameFunction() {
    return new Function<ACEEventRole, Symbol>() {
      @Override
      public Symbol apply(final ACEEventRole input) {
        return Symbol.from(input.name());
      }
    };
  }

  private final String name;
  private final List<String> entityTypesAllowed;
  private final List<String> valueTypesAllowed;

  public Symbol symbol() {
    return Symbol.from(name);
  }
}


