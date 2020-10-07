package com.bbn.serif.ace;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.RelationMention;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ACERelationType {

  public ACERelationType(String type, String subtype, List<String> arg1EntityTypes,
      List<String> arg2EntityTypes) {
    this.type = checkNotNull(type);
    this.subtype = checkNotNull(subtype);
    this.arg1EntityTypes = ImmutableList.copyOf(arg1EntityTypes);
    this.arg2EntityTypes = ImmutableList.copyOf(arg2EntityTypes);
  }

  public String name() {
    return type + "." + subtype;
  }

  public List<String> arg1EntityTypes() {
    return arg1EntityTypes;
  }

  public List<String> arg2EntityTypes() {
    return arg2EntityTypes;
  }

  public boolean isCandidateRelation(String arg1EntityType, String arg2EntityType) {
    return arg1EntityTypes.contains(arg1EntityType) && arg2EntityTypes.contains(arg2EntityType);
  }

  public boolean isCandidateRelation(Mention arg1, Mention arg2) {
    return isCandidateRelation(arg1.entityType().name().toString(),
        arg2.entityType().name().toString());
  }

  public boolean matches(RelationMention rm) {
    return rm.type() == Symbol.from(name());
  }

  public static final Function<ACERelationType, String> Name =
      new Function<ACERelationType, String>() {
        @Override
        public String apply(ACERelationType et) {
          return et.name();
        }
      };

  private final String type;
  private final String subtype;
  private final List<String> arg1EntityTypes;
  private final List<String> arg2EntityTypes;
}
