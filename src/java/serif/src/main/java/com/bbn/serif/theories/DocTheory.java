package com.bbn.serif.theories;

import com.bbn.bue.common.OptionalUtils;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.acronyms.Acronym;
import com.bbn.serif.theories.actors.ActorEntities;
import com.bbn.serif.theories.actors.ActorEntity;
import com.bbn.serif.theories.actors.ActorMention;
import com.bbn.serif.theories.actors.ActorMentions;
import com.bbn.serif.theories.facts.Fact;
import com.bbn.serif.theories.facts.Facts;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMentions;
import com.bbn.serif.theories.icewseventmentions.ICEWSEventMention;
import com.bbn.serif.theories.icewseventmentions.ICEWSEventMentions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.bbn.serif.theories.SentenceTheoryBeamFunctions.primaryTheory;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * Represents Serif's analysis of a document.
 *
 * @author rgabbard
 */
@JsonSerialize
@JsonDeserialize
@TextGroupImmutable
@Value.Immutable(prehash = true)
@Functional
public abstract class DocTheory implements Iterable<SentenceTheory> {

  // core fields
  public abstract Document document();

  @Value.Derived
  public ImmutableList<Sentence> sentences() {
    return FluentIterable.from(sentenceTheoryBeams())
        .transform(SentenceTheoryBeamFunctions.sentence())
        .toList();
  }

  /**
   * Returns an immutable list of the sentenceTheoryBeams in this document.
   */
  public abstract ImmutableList<SentenceTheoryBeam> sentenceTheoryBeams();

  /**
   * Returns the document-level value mention set some documents have. Most value mentions in a
   * document will be found in the sentence-level value mention sets instead. This method will not
   * return those.
   */
  @Value.Default
  public ValueMentions valueMentions() {
    return ValueMentions.absent();
  }

  @Value.Default
  public Entities entities() {
    return Entities.absent();
  }

  @Value.Default
  public Values values() {
    return Values.absent();
  }

  public abstract Optional<ImmutableList<Acronym>> acronyms();

  @Value.Default
  public Relations relations() {
    return Relations.absent();
  }

  @Value.Default
  public Events events() {
    return Events.absent();
  }

  /**
   * Events at the document-level. This is an event in terms of the involvement of entities, not in
   * terms of its textual realization (though backlinks to the text can be stored).  Usually this
   * should be preferred to {@link #events()}, which is kept around for backwards compatibility.
   */
  @Value.Default
  public DocumentEvents documentEvents() {
    return DocumentEvents.absent();
  }

  /**
   * This are assertions than an entity is involved in an event.  We store these
   * independently of {@link #documentEvents()} because some systems (e.g. TAC KBP 2014)
   * only produce these assertions without linking them.  These may or may not correspond
   * to the {@link DocumentEventArguments} found in {@link #documentEvents()}.
   */
  @Value.Default
  public DocumentEventArguments documentEventArguments() {
    return DocumentEventArguments.absent();
  }

  @Value.Default
  public ActorMentions actorMentions() {
    return ActorMentions.absent();
  }

  @Value.Default
  public ActorEntities actorEntities() {
    return ActorEntities.absent();
  }

  @Value.Default
  public EventEventRelationMentions eventEventRelationMentions() {
    return EventEventRelationMentions.absent();
  }

  public abstract Optional<DocumentActorInfo> documentActorInfo();

  @Value.Default
  public Facts facts() {
    return Facts.absent();
  }

  @Value.Default
  public ICEWSEventMentions icewsEventMentions() {
    return ICEWSEventMentions.absent();
  }


  @Value.Default
  public FlexibleEventMentions flexibleEventMentions() {
    return FlexibleEventMentions.absent();
  }

  /**
   * For internal use only to enable {@link Builder#replacePrimarySentenceTheory(SentenceTheory, SentenceTheory)}.
   * Do not use.
   */
  abstract ImmutableMap<SentenceTheory, SentenceTheory> primarySentenceTheoryReplacements();

  // derived fields

  /**
   * Get all the primary sentence theories in the document (the best in each sentence beam). Note
   * that if the document has not yet be tokenzied, there may be {@link #sentences()} but no
   * {@code sentenceTheories()}.
   */
  @Value.Derived
  public ImmutableList<SentenceTheory> sentenceTheories() {
    final ImmutableList<Optional<SentenceTheory>> primaryTheories =
        FluentIterable.from(sentenceTheoryBeams())
            .transform(SentenceTheoryBeamFunctions.primaryTheory())
            .toList();

    // we check in Value.Check that either all sentences are analyzed
    // or none are, so the gets below are safe
    if (OptionalUtils.numPresent(primaryTheories) > 0) {
      return FluentIterable.from(primaryTheories)
          .transform(OptionalUtils.<SentenceTheory>getFunction())
          .toList();
    } else {
      return ImmutableList.of();
    }
  }


  public final List<WithDocTheory<SentenceTheory>> sentenceTheoriesWithDocTheory() {
    //noinspection deprecation
    return Lists.transform(sentenceTheories(), WithDocTheory.<SentenceTheory>associateWith(this));
  }

  public final int numSentences() {
    // we want to include empty sentences in this count, hence using the deprecated method is ok
    //noinspection deprecation
    return sentenceTheories().size();
  }

  /**
   * This is less useful than it used to be because we now handle empty sentences more elegantly.
   * Prefer using simple {@link #sentenceTheories()} when possible.
   */
  public final Iterable<SentenceTheory> nonEmptySentenceTheories() {
    return filter(sentenceTheories(), SentenceTheory.isNonEmptyPredicate());
  }

  /**
   * This is less useful than it used to be because we now handle empty sentences more elegantly.
   */
  public final Iterable<WithDocTheory<SentenceTheory>> nonEmptySentenceTheoriesWithDocTheory() {
    return filter(sentenceTheoriesWithDocTheory(),
        compose(SentenceTheory.isNonEmptyPredicate(),
            WithDocTheory.<SentenceTheory>Item()));
  }

  /**
   * This is less useful than it used to be because we now handle empty sentences more elegantly.
   */
  public final Iterable<SentenceTheory> emptySentenceTheories() {
    return filter(sentenceTheories(), SentenceTheory.isEmptyPredicate());
  }

  /**
   * /**
   * This is less useful than it used to be because we now handle empty sentences more elegantly.
   */
  public final Iterable<WithDocTheory<SentenceTheory>> emptySentenceTheoriesWithDocTheory() {
	    return filter(sentenceTheoriesWithDocTheory(),
                compose(SentenceTheory.isEmptyPredicate(),
                    WithDocTheory.<SentenceTheory>Item()));
  }


  public final Symbol docid() {
    return document().name();
  }

  public final Sentence sentence(final int idx) {
    return sentenceTheoryBeams().get(idx).sentence();
  }

  /**
   * Get Serif's "best" analysis of the specified sentence, where best is determined by Serif
   * itself.
   */
  public final SentenceTheory sentenceTheory(final int idx) {
    return sentenceTheories().get(idx);
  }

  public final Optional<Symbol> sourceType() {
    return document().sourceType();
  }

  public final Optional<Metadata> metadata() {
    return document().metadata();
  }

  public Builder modifiedCopyBuilder() {
    return new Builder().from(this);
  }

  public static Builder builderForDocument(Document doc) {
    return new Builder().document(doc);
  }

  /**
   * Returns a {@link DocTheory} which is like this one, except each sentence theory is replaced
   * by the reslt of a given transformation.
   */
  public DocTheory copyWithTransformedPrimarySentenceTheories(
      SentenceTheoryTransformer sentenceTransform) {
    boolean dirty = false;
    final Builder ret = modifiedCopyBuilder();
    for (final SentenceTheory sentenceTheory : sentenceTheories()) {
      final SentenceTheory transformed = sentenceTransform.transform(this, sentenceTheory);
      if (!transformed.equals(sentenceTheory)) {
        ret.replacePrimarySentenceTheory(sentenceTheory, transformed);
        dirty = true;
      }
    }
    if (dirty) {
      return ret.build();
    } else {
      return this;
    }
  }

  /**
   * See {@link #copyWithTransformedPrimarySentenceTheories(SentenceTheoryTransformer)}.
   */
  public interface SentenceTheoryTransformer {

    SentenceTheory transform(DocTheory dt, SentenceTheory st);
  }

  /**
   * Returns an iterator of SentenceTheories for this document, one per sentence. When multiple
   * sentence theories are present in a sentence, the one Serif deemed best is returned.
   *
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public final Iterator<SentenceTheory> iterator() {
    return nonEmptySentenceTheories().iterator();
  }


  /**
   * Finds the entity a mention belongs to, if any
   */
  public final Optional<Entity> entityByMention(final Mention m) {
    return entities().entityByMention(m);
  }

  public final ImmutableList<SentenceTheory> lookupSentenceTheoriesContainedIn(
      OffsetRange<CharOffset> charOffsetOffsetRange) {
    // you could use binary search to make this faster if it were ever a bottleneck...
    final ImmutableList.Builder<SentenceTheory> ret = ImmutableList.builder();
    for (final SentenceTheory st : nonEmptySentenceTheories()) {
      if (charOffsetOffsetRange.contains(st.span().charOffsetRange())) {
        ret.add(st);
      }
    }
    return ret.build();
  }

  public final ImmutableList<SentenceTheory> lookupSentenceTheoriesContaining(OffsetRange<CharOffset> charOffsetOffsetRange) {
    // you could use binary search to make this faster if it were ever a bottleneck...
    final ImmutableList.Builder<SentenceTheory> ret = ImmutableList.builder();
    for (final SentenceTheory st : nonEmptySentenceTheories()) {
      if (st.span().charOffsetRange().contains(charOffsetOffsetRange)) {
        ret.add(st);
      }
    }
    return ret.build();
  }

  public final ImmutableList<SentenceTheory> lookupSentenceTheoriesOverlapping(
      OffsetRange<CharOffset> charOffsetOffsetRange) {
    // you could use binary search to make this faster if it were ever a bottleneck...
    final ImmutableList.Builder<SentenceTheory> ret = ImmutableList.builder();
    for (final SentenceTheory st : nonEmptySentenceTheories()) {
      if (st.span().charOffsetRange().overlaps(charOffsetOffsetRange)) {
        ret.add(st);
      }
    }
    return ret.build();
  }

  @org.immutables.builder.Builder.AccessibleFields
  public static class Builder extends ImmutableDocTheory.Builder {
    /**
     * Replaces the primary existing sentence theory for a sentence with a new sentence theory.
     * There must already be a primary SentenceTheory for this sentence, and the old and new
     * sentence theories must correspond to exactly the same Sentence object.  Non-primary sentence
     * theories are unchanged.
     *
     * Be aware that replacing sentence theories may require clearing or otherwise altering
     * document-level theories which refer to sentence level theory elements, such as entities,
     * values, relations, and events.  Inconsistencies may cause a build call to fail.
     */
    public Builder replacePrimarySentenceTheory(final SentenceTheory toReplace,
        final SentenceTheory st) {
      return putPrimarySentenceTheoryReplacements(toReplace, st);
    }

    public Builder addSentenceTheory(final SentenceTheory st) {
      return addSentenceTheoryBeams(SentenceTheoryBeam.forSentenceTheory(st));
    }

    @Override
    public DocTheory build() {
      // if the user has requested that certain sentence theory beams have their primary
      // sentence theories replaced, we need to do that before building. Before Immutables,
      // we did this directly in the replacePrimarySentenceTheory method. But now we can't,
      // so we instead collect the requests in a package-private field and apply them
      // before building
      final List<SentenceTheoryBeam> updatedSentenceTheoryBeams =
          Lists.newArrayList(sentenceTheoryBeams.build());

      // index the sentence theories by their primary beam
      final ImmutableMap.Builder<SentenceTheory, SentenceTheoryBeam> beamsByPrimaryTheoryB =
          ImmutableMap.builder();

      for (final SentenceTheoryBeam beam : updatedSentenceTheoryBeams) {
        if (beam.primaryTheory().isPresent()) {
          beamsByPrimaryTheoryB.put(beam.primaryTheory().get(), beam);
        }
      }
      final ImmutableMap<SentenceTheory, SentenceTheoryBeam>
          beamsByPrimaryTheory = beamsByPrimaryTheoryB.build();

      for (final Map.Entry<SentenceTheory, SentenceTheory> e : primarySentenceTheoryReplacements
          .build().entrySet()) {
        final SentenceTheory theoryToReplace = e.getKey();
        final SentenceTheoryBeam beamToReplaceIn = beamsByPrimaryTheory.get(theoryToReplace);
        if (beamToReplaceIn != null) {
          // get is safe because we filter above for primary theory being present
          //noinspection OptionalGetWithoutIsPresent
          updatedSentenceTheoryBeams.set(beamToReplaceIn.primaryTheory().get().sentenceNumber(),
              beamToReplaceIn.copyWithPrimaryTheoryReplaced(e.getValue()));
        } else {
          throw new SerifException("Attempting to replace a sentence theory not in the document!");
        }

      }
      this.sentenceTheoryBeams(updatedSentenceTheoryBeams);
      this.primarySentenceTheoryReplacements(ImmutableMap.<SentenceTheory, SentenceTheory>of());

      return super.build();
    }
  }

  private SentenceTheory containingPrimarySentenceTheory(final Spanning spanning) {
    final int containingSentenceIdx = spanning.span().sentenceIndex();
    final Optional<SentenceTheory> ret =
        sentenceTheoryBeams().get(containingSentenceIdx).primaryTheory();
    if (ret.isPresent()) {
      return ret.get();
    } else {
      throw new IllegalArgumentException("Cannot search for a spanning an an untokenized sentence");
    }
  }

  @Value.Check
  protected void check() {
    // this is intended for builder use only and should be empty in all built objects
    checkArgument(primarySentenceTheoryReplacements().isEmpty());

    final int numAnalyzedSentences = OptionalUtils.numPresent(
        transform(sentenceTheoryBeams(), primaryTheory()));
    checkArgument(numAnalyzedSentences == 0 || numAnalyzedSentences == numSentences(),
        "Either all sentences must be tokenized or none may be");

    for (int i = 0; i < sentenceTheoryBeams().size(); ++i) {
      checkArgument(i == sentenceTheoryBeams().get(i).sentence().sentenceNumber(),
          "Sentence theory beams not properly ordered by sentence number");
    }

    for (final SentenceTheory st : sentenceTheories()) {
      if (!st.tokenSequence().documentOriginalText().equals(document().originalText()) && !st
          .tokenSequence().isAbsent()) {
        throw new RuntimeException("In document " + docid() + ", original text for sentence "
            + st.sentenceNumber() + " does not match that for the document itself.");
      }
    }

    for (final Entity e : entities()) {
      for (final Mention m : e) {
        final SentenceTheory st = containingPrimarySentenceTheory(m);
        if (!st.mentions().asList().contains(m)) {
          throw new RuntimeException(String.format(
              "Attempting to build document with an Entity %s which refers to a non-existent mention %s.",
              e, m));
        }
      }
    }

    for (final Relation r : relations()) {
      for (final RelationMention rm : r) {
        final SentenceTheory st = containingPrimarySentenceTheory(rm);
        if (!st.relationMentions().asList().contains(rm)) {
          throw new RuntimeException(String.format(
              "Attempting to build document with a Relation %s which refers to a non-existent relation mention %s.",
              r, rm));
        }
      }
    }

    for (final Event e : events()) {
      for (final EventMention em : e) {
        final SentenceTheory st = containingPrimarySentenceTheory(em);
        if (!st.eventMentions().asList().contains(em)) {
          throw new RuntimeException(String.format(
              "Attempting to build document with an Event %s which refers to a non-existent event mention %s.",
              e, em));
        }
      }
    }

    for (final ActorEntity ae : actorEntities()) {
      for (final ActorMention am : ae) {
        Mention m = am.mention();
        final SentenceTheory st = containingPrimarySentenceTheory(m);
        if (!st.mentions().asList().contains(m)) {
          throw new RuntimeException(String.format(
              "Attempting to build document with an ActorMention  %s which refers to a non-existent mention %s.",
              am, m));
        }
      }
    }

    for (final Fact f : facts()) {
      for (final Fact.Argument arg : f.arguments()) {
        if (arg instanceof Fact.MentionArgument) {
          Mention m = ((Fact.MentionArgument) arg).mention();
          final SentenceTheory st = containingPrimarySentenceTheory(m);
          if (!st.mentions().asList().contains(m)) {
            throw new RuntimeException(String.format(
                "Attempting to build document with a Fact.MentionArgument  %s which refers to a non-existent mention %s.",
                arg, m));
          }
        } else if (arg instanceof Fact.ValueMentionArgument) {
          Optional<ValueMention> vmOpt = ((Fact.ValueMentionArgument) arg).valueMention();
          if (vmOpt.isPresent()) {
            final ValueMention vm = vmOpt.get();
            // if the VM isn't doc-level...
            if (!valueMentions().asList().contains(vm)) {
              final SentenceTheory st = containingPrimarySentenceTheory(vm);
              // then it must be present in the appropriate sentence
              if (!st.valueMentions().asList().contains(vm)) {
                throw new RuntimeException(String.format(
                    "Attempting to build document with a Fact.ValueMentionArgument  %s which refers to a non-existent valueMention %s.",
                    arg, vm));
              }
            }
          }
        }
      }
    }

    for (final ActorMention am : actorMentions()) {
      Mention m = am.mention();
      final SentenceTheory st = containingPrimarySentenceTheory(m);
      if (!st.mentions().asList().contains(m)) {
        throw new RuntimeException(String.format(
            "Attemping to build document with an ActorMention %s which refers to a non-existent mention %s.",
            am, m));
      }
    }

    for (final ICEWSEventMention icewsEventMention : icewsEventMentions()) {
      for (ICEWSEventMention.ICEWSEventParticipant participant : icewsEventMention
          .eventParticipants()) {
        ActorMention am = participant.actorMention();
        Mention m = am.mention();
        final SentenceTheory st = containingPrimarySentenceTheory(m);
        if (!st.mentions().asList().contains(m)) {
          throw new RuntimeException(String.format(
              "Attemping to build document with an ICEWSEventParticipant for an ActorMention  %s which refers to a non-existent mention %s.",
              am, m));
        }
      }
    }
  }


  public static Function<DocTheory, Symbol> docIdFunction() {
    return DocIdFunction.INSTANCE;
  }

  private enum DocIdFunction implements Function<DocTheory, Symbol> {
    INSTANCE;
    @Override
    public Symbol apply(final DocTheory input) {
      return input.docid();
    }
  }

  public static Function<DocTheory, DocTheory> asDocTheoryFunction(
      Function<WithDocTheory<SentenceTheory>, WithDocTheory<SentenceTheory>> sentenceFunction) {
    return new FunctionFromSentenceTheoryFunction.Builder()
        .sentenceFunction(sentenceFunction).build();
  }
}

@TextGroupImmutable
@Value.Immutable
abstract class FunctionFromSentenceTheoryFunction implements Function<DocTheory, DocTheory> {
  abstract Function<WithDocTheory<SentenceTheory>, WithDocTheory<SentenceTheory>> sentenceFunction();


  @Override
  public DocTheory apply(final DocTheory input) {
    final DocTheory.Builder ret = input.modifiedCopyBuilder();

    boolean dirty = false;

    for (final SentenceTheory st : input.nonEmptySentenceTheories()) {
      final WithDocTheory<SentenceTheory> transformed =
          sentenceFunction().apply(WithDocTheory.from(st, input));
      if (!st.equals(transformed.item())) {
        dirty = true;
        ret.replacePrimarySentenceTheory(st, transformed.item());
      }
    }

    if (dirty) {
      return ret.build();
    } else {
      return input;
    }
  }

  static class Builder extends ImmutableFunctionFromSentenceTheoryFunction.Builder {}
}
