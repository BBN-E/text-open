package com.bbn.serif.theories.actors;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;

import com.google.common.base.Optional;

public interface ActorMention {


  Optional<Symbol> actorName();

  Symbol sourceNote();

  Mention mention();

  Optional<Symbol> actorDBName();

  @Override
  public String toString();
}
