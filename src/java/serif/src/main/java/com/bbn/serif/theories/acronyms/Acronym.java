package com.bbn.serif.theories.acronyms;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.TokenSpan;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 Represents an acronym.  The acronym may optionally have an
 expansion if we know what it stands for.

 Originally created for us with the VIKTRS AcronymFinder.

 @author pshapiro
 **/
public final class Acronym {

  /**
  * Acronyms and expansions are both stored as a Provenance, which is structured to give it context
   * within the greater Doc.
   * We may or may not be able to obtain a Mention or a TokenSpan for a given acronym/expansion, so
   * these fields are optional.
   * Text contains the content of acronym/expansion
  **/
  public final static class Provenance {

    final Optional<Mention> mention;
    final Optional<TokenSpan> tokenSpan;
    final Symbol text;

    private Provenance(Optional<Mention> mention, Optional<TokenSpan> tokenSpan, Symbol text) {

      this.mention = checkNotNull(mention);
      this.tokenSpan = checkNotNull(tokenSpan);
      this.text = checkNotNull(text);

    }

    public Optional<Mention> getMention() {
      return this.mention;
    }

    public Optional<TokenSpan> getTokenSpan() {
      return this.tokenSpan;
    }

    public Symbol getText() {
      return this.text;
    }

  }

  public static Provenance createFromMention(final Mention mention, final Symbol text) {
    TokenSpan tokenSpan = mention.tokenSpan();
    return new Provenance(Optional.of(mention), Optional.of(tokenSpan), text);
  }
  public static Provenance createFromTokenSpan(final TokenSpan tokenSpan, final Symbol text) {
    return new Provenance(Optional.<Mention>absent(), Optional.of(tokenSpan), text);
  }

  public static Provenance createFromTextOnly(final Symbol text) {
    return new Provenance(Optional.<Mention>absent(), Optional.<TokenSpan>absent(), text);
  }

  private final Provenance acronym;
  private final Optional<Provenance> expansion;

  private Acronym(Provenance acronym, Optional<Provenance> expansion){
    checkArgument(acronym.tokenSpan.isPresent());
    this.acronym = checkNotNull(acronym);
    this.expansion = checkNotNull(expansion);
  }

  public static Acronym createUnexpandedAcronym(final Provenance acronym) {
    return new Acronym(acronym, Optional.<Provenance>absent());
  }

  public static Acronym createExpandedAcronym(final Provenance acronym, final Provenance expansion) {
    return new Acronym(acronym, Optional.of(expansion));
  }

  public Provenance getAcronym() {
    return this.acronym;
  }

  public Optional<Provenance> getExpansion() {
    return this.expansion;
  }
}
