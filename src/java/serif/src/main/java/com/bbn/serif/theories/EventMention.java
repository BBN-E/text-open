package com.bbn.serif.theories;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.types.*;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.filter;

public final class EventMention implements Spanning, HasExternalID {

  public Symbol type() {
    return eventType;
  }

  public Optional<Symbol> pattern() {
    return Optional.fromNullable(patternID);
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  public Modality modality() {
    return modality;
  }

  /**
   * Be aware that currently C++ SERIF does not produce modality scores (only JSerif), so getting
   * the modality score from a C++ SERIF document will return 1.0.
   */
  public double modalityScore() {
    return modalityScore;
  }

  public Polarity polarity() {
    return polarity;
  }

  public Tense tense() {
    return tense;
  }

  public Genericity genericity() {
    return genericity;
  }

  public DirectionOfChange directionOfChange() { return directionOfChange; }

  /**
   * Be aware that currently C++ SERIF does not produce genericity scores (only JSerif), so getting
   * the genericity score from a C++ SERIF document will return 1.0.
   */
  public double genericityScore() {
    return genericityScore;
  }

  public Optional<GainLoss> gainLoss() {
    return Optional.fromNullable(gainLoss);
  }

  public Optional<Indicator> indicator() {
    return Optional.fromNullable(indicator);
  }

  public Optional<Proposition> anchorProposition() {
    return Optional.fromNullable(anchorProp);
  }

  public SynNode anchorNode() {
    return anchorNode;
  }

  public List<Argument> arguments() {
    return arguments;
  }

  public Iterable<Argument> temporalArguments() {
    return filter(arguments(), IsTemporalArgument);
  }

  public double score() {
    return score;
  }

//  public TokenSequence.Span anchorContextSpan() {
//    return anchorContextSpan;
//  }
//
//  public void setAnchorContextSpan(final TokenSequence.Span anchorContextSpan) {
//    this.anchorContextSpan = anchorContextSpan;
//  }

  //public Optional<String> contextWindow() {
  //  return Optional.fromNullable(contextWindow);
  //}

  public Optional<Integer> semanticPhraseStart() {
    return Optional.fromNullable(semanticPhraseStart);
  }

  public Optional<Integer> semanticPhraseEnd() {
    return Optional.fromNullable(semanticPhraseEnd);
  }

  public Optional<Symbol> model() {
    return Optional.fromNullable(model);
  }

  public List<EventType> eventTypes() {
    return eventTypes;
  }

  public List<EventType> factorTypes() {
    return factorTypes;
  }

  public List<Anchor> anchors() {
    return anchors;
  }

  public SentenceTheory sentenceTheory(final DocTheory docTheory) {
    if (anchorNode != null) {
      return anchorNode.sentenceTheory(docTheory);
    } else if (anchorProp != null) {
      return anchorProp.sentenceTheory(docTheory);
    } else {
      throw new SerifException("Malformed EventMention has neither anchorNode nor anchorProp!");
    }
  }

  public boolean hasMultipleArgsWithSameRole() {
    final Multiset<Symbol> types = HashMultiset.create();

    for (final EventMention.Argument arg : arguments()) {
      types.add(arg.role());
    }

    for (final Entry<Symbol> entry : types.entrySet()) {
      if (entry.getCount() > 1) {
        return true;
      }
    }

    return false;
  }

  public Collection<Argument> argsForRole(final Symbol targetRole) {
    final ImmutableList.Builder<Argument> ret = ImmutableList.builder();

    for (final Argument arg : arguments()) {
      if (arg.role() == targetRole) {
        ret.add(arg);
      }
    }

    return ret.build();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append(eventType).append("[");
    if (anchorProp != null) {
      sb.append("anchorP=").append(anchorProp);
    } else {
      sb.append("anchorN=").append(anchorNode);
    }
    sb.append(";args=").append(StringUtils.SemicolonJoiner.join(arguments));
    sb.append("]");
    return sb.toString();
  }

  public static ImmutableSet<Symbol> eventTypesContained(final SentenceTheory st) {
    final ImmutableSet.Builder<Symbol> ret = ImmutableSet.builder();

    for (final EventMention em : st.eventMentions()) {
      ret.add(em.type());
    }

    return ret.build();
  }

  @Override
  public Span span() {
    final List<Span> spans = Lists.newArrayList();

//    if (anchorProp != null) {
//      spans.add(anchorProp.span());
//    }

    if (anchorNode != null) {
      spans.add(anchorNode.span());
    }

//    for (final Argument arg : arguments) {
//      spans.add(arg.span());
//    }

    return TokenSequence.union(spans);
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  private final Symbol eventType;
  @Nullable private final Symbol patternID;
  private final Modality modality;
  private final double modalityScore;
  private final Polarity polarity;
  private final Genericity genericity;
  private final double genericityScore;
  private final Tense tense;
  private final DirectionOfChange directionOfChange;

  private final Proposition anchorProp;
  private final SynNode anchorNode;

  private final List<Argument> arguments;
  @Nullable private final Indicator indicator;
  @Nullable private final GainLoss gainLoss;
  @Nullable private final Symbol external_id;

  private final double score;
  //@Nullable private TokenSequence.Span anchorContextSpan;
  //@Nullable private final String contextWindow;     // this is the (startTokenIndex,endTokenIndex) around the anchor
  @Nullable private final Integer semanticPhraseStart;
  @Nullable private final Integer semanticPhraseEnd;

  @Nullable private final Symbol model;
  private final List<EventType> eventTypes;
  private final List<EventType> factorTypes;
  private final List<Anchor> anchors;

  // we know the cast is safe because Iterable is read-only
  @SuppressWarnings("unchecked")
  public Iterable<MentionArgument> mentionArguments() {
    return (Iterable<MentionArgument>) (Object) filter(arguments(),
        instanceOf(MentionArgument.class));
  }

  // we know the cast is safe because Iterable is read-only
  @SuppressWarnings("unchecked")
  public Iterable<ValueMentionArgument> valueMentionArguments() {
    return (Iterable<ValueMentionArgument>) (Object) filter(arguments(),
        instanceOf(ValueMentionArgument.class));
  }

  public static final class EventType {


    private final Symbol eventType;
    private final double score;
    private final Optional<Double> magnitude;
    private final Optional<Trend> trend;

    private EventType(
        final Symbol eventType, final double score, final Optional<Double> magnitude, final Optional<Trend> trend) {
      this.eventType = eventType;
      this.score = score;
      this.magnitude = magnitude;
      this.trend = trend;
    }

    public static EventType from(
        final Symbol eventType, final double score, final Optional<Double> magnitude, final Optional<Trend> trend) {
      return new EventType(eventType, score, magnitude,trend);
    }

    public Symbol eventType() {
      return eventType;
    }

    public double score() {
      return score;
    }

    public Optional<Double> getMagnitude() {
      return magnitude;
    }

    public Optional<Trend> getTrend(){
      return  trend;
    }

    public Builder modifiedCopyBuilder() {
      return new Builder(eventType, score, magnitude,trend);
    }

    public static class Builder {

      Symbol eventType;
      double score;
      Optional<Double> magnitude;
      Optional<Trend> trend;
      private Builder(final Symbol eventType, final double score,
          final Optional<Double> magnitude, final Optional<Trend> trend) {
        this.eventType = eventType;
        this.score = score;
        this.magnitude = magnitude;
        this.trend = trend;
      }

      public Builder setType(final Symbol eventType) {
        checkNotNull(eventType);
        this.eventType = eventType;
        return this;
      }

      public Builder setScore(final double score) {
        this.score = score;
        return this;
      }

      public Builder setMagnitude(final Optional<Double> magnitude) {
        this.magnitude = magnitude;
        return this;
      }

      public Builder setTrend(final Optional<Trend> trend){
        this.trend = trend;
        return this;
      }

      public EventMention.EventType build() {
        return new EventMention.EventType(eventType, score, magnitude,trend);
      }

    }
  }



  public static final class Anchor {
    private final SynNode anchorNode;
    private final Proposition anchorProp;

    private Anchor(final SynNode anchorNode, final Proposition anchorProp) {
      this.anchorNode = anchorNode;
      this.anchorProp = anchorProp;
    }

    public static Anchor from(final SynNode anchorNode, final Proposition anchorProp) {
      return new Anchor(anchorNode, anchorProp);
    }

    public SynNode anchorNode() {
      return anchorNode;
    }

    public Optional<Proposition> anchorProposition() {
      return Optional.fromNullable(anchorProp);
    }
  }

  public static abstract class Argument implements Spanning {

    private final Symbol role;
    private final float score;
    private String extractionMethod;

    private Argument(final Symbol role, final float score) {
      this.role = checkNotNull(role);
      this.score = score;
      this.extractionMethod = "";
    }

    public Symbol role() {
      return role;
    }

    public float score() {
      return score;
    }

    public String extractionMethod() {
      return this.extractionMethod;
    }

    public void setExtractionMethod(final String extractionMethod) {
      this.extractionMethod = extractionMethod;
    }

    @Override
    public abstract TokenSequence.Span span();

    public abstract Argument copyWithDifferentRole(Symbol role);

    public abstract Argument copyWithDifferentScore(float score);

    public static Function<EventMention.Argument, Symbol> roleFunction() {
      return RoleFunction.INSTANCE;
    }

    public static Function<EventMention.Argument, Float> scoreFunction() {
      return ScoreFunction.INSTANCE;
    }

    private enum RoleFunction implements Function<EventMention.Argument, Symbol> {
      INSTANCE;

      @Override
      public Symbol apply(final Argument input) {
        return input.role();
      }
    }

    private enum ScoreFunction implements Function<EventMention.Argument, Float> {
      INSTANCE;

      @Override
      public Float apply(final Argument input) {
        return input.score();
      }
    }
  }


  public static final class MentionArgument extends Argument {

    /**
     * Deprecated as public constructor, use factory method
     *
     * @deprecated
     */
    @Deprecated
    public MentionArgument(final Symbol role, final Mention mention, final float score) {
      super(role, score);
      this.mention = checkNotNull(mention);
    }

    public static MentionArgument from(final Symbol role, final Mention mention,
        final float score) {
      return new MentionArgument(role, mention, score);
    }

    @Override
    public TokenSequence.Span span() {
      return mention.span();
    }

    @Override
    public TokenSpan tokenSpan() {
      return span();
    }

    public Mention mention() {
      return mention;
    }

    @Override
    public MentionArgument copyWithDifferentRole(Symbol newRole) {
      return new MentionArgument(newRole, mention, score());
    }

    @Override
    public MentionArgument copyWithDifferentScore(float newScore) {
      return new MentionArgument(role(), mention, newScore);
    }

    public MentionArgument copyWithDifferentMention(Mention newMention) {
      return new MentionArgument(role(), newMention, score());
    }

    @Override
    public String toString() {
      return "mArg(" + role() + "=" + mention + ")";
    }

    private final Mention mention;

  }

  public static final class ValueMentionArgument extends Argument {

    /**
     * Only deprecated as public constructor.
     *
     * @deprecated
     */
    @Deprecated
    public ValueMentionArgument(final Symbol role, final ValueMention valueMention,
        final float score) {
      super(role, score);
      this.valueMention = valueMention;
    }

    public static ValueMentionArgument from(final Symbol role, final ValueMention valueMention,
        final float score) {
      return new ValueMentionArgument(role, valueMention, score);
    }

    @Override
    public TokenSequence.Span span() {
      return valueMention.span();
    }

    @Override
    public TokenSpan tokenSpan() {
      return span();
    }

    public ValueMention valueMention() {
      return valueMention;
    }

    @Override
    public ValueMentionArgument copyWithDifferentRole(Symbol newRole) {
      return new ValueMentionArgument(newRole, valueMention, score());
    }

    @Override
    public ValueMentionArgument copyWithDifferentScore(float newScore) {
      return new ValueMentionArgument(role(), valueMention, newScore);
    }

    @Override
    public String toString() {
      return "vArg(" + role() + "=" + valueMention + ")";
    }

    private final ValueMention valueMention;

  }

  public static final class EventMentionArgument extends Argument {

    /**
     * Deprecated as public constructor, use factory method
     *
     * @deprecated
     */
    @Deprecated
    public EventMentionArgument(final Symbol role, final EventMention eventMention, final float score) {
      super(role, score);
      this.eventMention = checkNotNull(eventMention);
    }

    public static EventMentionArgument from(final Symbol role, final EventMention eventMention,
                                       final float score) {
      return new EventMentionArgument(role, eventMention, score);
    }

    @Override
    public TokenSequence.Span span() {
      return eventMention.span();
    }

    @Override
    public TokenSpan tokenSpan() {
      return span();
    }

    public EventMention eventMention() {
      return eventMention;
    }

    @Override
    public EventMentionArgument copyWithDifferentRole(Symbol newRole) {
      return new EventMentionArgument(newRole, eventMention, score());
    }

    @Override
    public EventMentionArgument copyWithDifferentScore(float newScore) {
      return new EventMentionArgument(role(), eventMention, newScore);
    }

    @Override
    public String toString() {
      return "mArg(" + role() + "=" + eventMention + ")";
    }

    private final EventMention eventMention;

  }

  public static final class SpanArgument extends Argument {

    /**
     * Only deprecated as public constructor.
     *
     * @deprecated
     */
    @Deprecated
    public SpanArgument(final Symbol role, final TokenSequence.Span span, final float score) {
      super(role, score);
      this.span = span;
    }

    public static SpanArgument from(final Symbol role, final TokenSequence.Span span,
        final float score) {
      return new SpanArgument(role, span, score);
    }

    @Override
    public TokenSequence.Span span() {
      return span;
    }

    @Override
    public TokenSpan tokenSpan() {
      return span();
    }

    @Override
    public String toString() {
      return "sArg(" + role() + "=" + span + ")";
    }

    @Override
    public SpanArgument copyWithDifferentRole(Symbol newRole) {
      return new SpanArgument(newRole, span, score());
    }

    @Override
    public SpanArgument copyWithDifferentScore(float newScore) {
      return new SpanArgument(role(), span, newScore);
    }

    private final TokenSequence.Span span;
  }

  private static final Predicate<Argument> IsTemporalArgument = new Predicate<Argument>() {
    @Override
    public boolean apply(final Argument arg) {
      return arg instanceof ValueMentionArgument
          && ((ValueMentionArgument) arg).valueMention().isTimexValue();
    }
  };

  public EventMention(final Symbol eventType, final Proposition anchorProp,
      final SynNode anchorNode,
      final List<Argument> arguments, final Symbol patternID, final Modality modality,
      final double modalityScore,
      final Polarity polarity, final Genericity genericity, final double genericityScore,
      final GainLoss gainLoss, final Indicator indicator, final Tense tense,
      final DirectionOfChange directionOfChange,
      final double score, final Integer semanticPhraseStart, final Integer semanticPhraseEnd,
      final Symbol model, final List<EventType> eventTypes, final List<Anchor> anchors) {
    this(eventType, anchorProp, anchorNode, arguments, patternID, modality, modalityScore, polarity,
        genericity, genericityScore, gainLoss, indicator, tense, directionOfChange, null, score,
        semanticPhraseStart, semanticPhraseEnd, model, eventTypes, anchors, new ArrayList<EventType>());
  }

  public EventMention(final Symbol eventType, final Proposition anchorProp,
      final SynNode anchorNode,
      final List<Argument> arguments, final Symbol patternID, final Modality modality,
      final double modalityScore,
      final Polarity polarity, final Genericity genericity, final double genericityScore,
      final GainLoss gainLoss, final Indicator indicator, final Tense tense,
      final DirectionOfChange directionOfChange,
      @Nullable final Symbol external_id, final double score, final Integer semanticPhraseStart,
      final Integer semanticPhraseEnd, final Symbol model,
      final List<EventType> eventTypes, final List<Anchor> anchors,
      final List<EventType> factorTypes) {
    this.external_id = external_id;
    // at least one of anchorProp or anchorNode should be specified
    //checkArgument((anchorProp != null) || (anchorNode != null));

    this.eventType = checkNotNull(eventType);
    this.anchorProp = anchorProp;
    this.anchorNode = checkNotNull(anchorNode);
    this.arguments = ImmutableList.copyOf(arguments);
    this.patternID = patternID;
    this.modality = checkNotNull(modality);
    this.modalityScore = modalityScore;
    this.polarity = checkNotNull(polarity);
    this.genericity = checkNotNull(genericity);
    this.genericityScore = genericityScore;
    this.tense = checkNotNull(tense);
    this.directionOfChange = checkNotNull(directionOfChange);
    this.gainLoss = gainLoss;
    this.indicator = indicator;
    this.score = score;
    this.semanticPhraseStart = semanticPhraseStart;
    this.semanticPhraseEnd = semanticPhraseEnd;
    this.model = model;
    this.eventTypes = eventTypes;
    this.factorTypes = factorTypes;
    this.anchors = anchors;
  }

  public EventMention(final Symbol eventType, final Proposition anchorProp,
                      final SynNode anchorNode,
                      final List<Argument> arguments, final Symbol patternID, final Modality modality,
                      final double modalityScore,
                      final Polarity polarity, final Genericity genericity, final double genericityScore,
                      final GainLoss gainLoss, final Indicator indicator, final Tense tense,
                      final DirectionOfChange directionOfChange,
                      @Nullable final Symbol external_id, final double score, final Integer semanticPhraseStart,
                      final Integer semanticPhraseEnd, final Symbol model,
                      final List<EventType> eventTypes, final List<Anchor> anchors) {
    this.external_id = external_id;
    // at least one of anchorProp or anchorNode should be specified
    //checkArgument((anchorProp != null) || (anchorNode != null));

    this.eventType = checkNotNull(eventType);
    this.anchorProp = anchorProp;
    this.anchorNode = checkNotNull(anchorNode);
    this.arguments = ImmutableList.copyOf(arguments);
    this.patternID = patternID;
    this.modality = checkNotNull(modality);
    this.modalityScore = modalityScore;
    this.polarity = checkNotNull(polarity);
    this.genericity = checkNotNull(genericity);
    this.genericityScore = genericityScore;
    this.tense = checkNotNull(tense);
    this.directionOfChange = checkNotNull(directionOfChange);
    this.gainLoss = gainLoss;
    this.indicator = indicator;
    this.score = score;
    this.semanticPhraseStart = semanticPhraseStart;
    this.semanticPhraseEnd = semanticPhraseEnd;
    this.model = model;
    this.eventTypes = eventTypes;
    this.factorTypes = new ArrayList<>();
    this.anchors = anchors;
  }

  public Builder modifiedCopyBuilder() {
    return new Builder(eventType, anchorProp, anchorNode, arguments, patternID,
        modality, modalityScore, polarity, genericity, genericityScore, gainLoss, indicator, tense,
        directionOfChange,
        score, semanticPhraseStart, semanticPhraseEnd, model, eventTypes, anchors, factorTypes);
  }

  /**
   * Creates a build with default settings of (1) no arguments, (2) Asserted modality with score
   * 1.0, (3) positive polarity, (4) unspecified tense, and (5) specific genericity with score 1.0.
   */
  public static Builder builder(final Symbol eventType) {
    return new Builder(eventType, null, null, ImmutableList.<Argument>of(), null, Modality.ASSERTED,
        1.0,
        Polarity.POSITIVE, Genericity.SPECIFIC, 1.0, null, null, Tense.UNSPECIFIED, DirectionOfChange.UNSPECIFIED, 0.0,
        null, null, null, ImmutableList.<EventType>of(), ImmutableList.<Anchor>of(), ImmutableList.<EventType>of());
  }

  public static class Builder {

    @Nullable private Symbol external_id;

    private Builder(final Symbol eventType, final Proposition anchorProp, final SynNode anchorNode,
        final List<Argument> arguments, final Symbol patternID, final Modality modality,
        final double modalityScore,
        final Polarity polarity, final Genericity genericity, final double genericityScore,
        final GainLoss gainLoss,
        final Indicator indicator, final Tense tense, DirectionOfChange directionOfChange, final double score,
        final Integer semanticPhraseStart, final Integer semanticPhraseEnd, final Symbol model,
        final List<EventType> eventTypes, final List<Anchor> anchors, final List<EventType> factorTypes) {
      this.eventType = checkNotNull(eventType);
      this.anchorProp = anchorProp;
      this.anchorNode = anchorNode;
      this.arguments = ImmutableList.copyOf(arguments);
      this.patternID = patternID;
      this.modality = checkNotNull(modality);
      this.modalityScore = modalityScore;
      this.polarity = checkNotNull(polarity);
      this.genericity = checkNotNull(genericity);
      this.genericityScore = genericityScore;
      this.tense = tense;
      this.directionOfChange = directionOfChange;
      this.gainLoss = gainLoss;
      this.indicator = indicator;
      this.score = score;
      this.semanticPhraseStart = semanticPhraseStart;
      this.semanticPhraseEnd = semanticPhraseEnd;
      this.model = model;
      this.eventTypes = eventTypes;
      this.anchors = anchors;
      this.factorTypes = factorTypes;
    }

    public EventMention build() {
      return new EventMention(eventType, anchorProp, anchorNode,
          arguments, patternID, modality, modalityScore, polarity,
          genericity, genericityScore, gainLoss, indicator, tense, directionOfChange, external_id, score,
          semanticPhraseStart, semanticPhraseEnd, model, eventTypes, anchors, factorTypes);
    }

    public Builder setType(final Symbol type) {
      checkNotNull(type);
      checkArgument(!type.asString().isEmpty());
      this.eventType = type;
      return this;
    }

    public Builder setArguments(final List<Argument> arguments) {
      this.arguments = Lists.newArrayList(checkNotNull(arguments));
      return this;
    }

    public Builder setAnchorProp(final Proposition anchorProp) {
      this.anchorProp = anchorProp;
      return this;
    }

    public Builder setAnchorNode(final SynNode anchorNode) {
      this.anchorNode = anchorNode;
      return this;
    }

    public Builder setExternalID(final Symbol externalID) {
      this.external_id = externalID;
      return this;
    }

    /**
     * Attempts to set the anchor proposition based on the anchor node by using {@link
     * com.bbn.serif.theories.Propositions#findPropositionByNode(SynNode)}.  The anchor proposition
     * will only in fact be set if {@code findPropositionByNode} returns a proposition.  Attempting
     * to call this method if the anchor node has not previously been set will throw an {@link
     * java.lang.IllegalStateException}.
     *
     * @param st The {@link com.bbn.serif.theories.SentenceTheory} for the sentence containing the
     *           event mention to be built.
     */
    @SuppressWarnings("ReferenceEquality")
    public Builder setAnchorPropFromNode(SentenceTheory st) {
      if (anchorNode == null) {
        throw new IllegalStateException(
            "Cannot set anchor prop from node if anchor node has not been set");
      }
      checkArgument(anchorNode.span().tokenSequence() == st.tokenSequence(),
          "Provided sentence theory does not mach anchor node");

      final Optional<Proposition> curretProp = st.propositions().findPropositionByNode(anchorNode);
      if (curretProp.isPresent()) {
        setAnchorProp(curretProp.get());
      }
      return this;
    }

    public Builder setScore(final double score) {
      this.score = score;
      return this;
    }

    /**
     * Favor version which sets score explicitly. This one defaults to score 1.0. May be removed in
     * the future.
     *
     * @deprecated
     */
    @Deprecated
    public Builder setModality(final Modality modality) {
      this.modality = modality;
      return this;
    }

    public Builder setModality(final Modality modality, double modalityScore) {
      this.modality = modality;
      this.modalityScore = modalityScore;
      return this;
    }


    /**
     * Favor version which sets genericity explicitly. This one default to score 1.0. May be removed
     * in the future.
     *
     * @deprecated
     */
    public Builder setGenericity(final Genericity genericity) {
      this.genericity = genericity;
      return this;
    }

    public Builder setGenericity(final Genericity genericity, double genericityScore) {
      this.genericity = genericity;
      this.genericityScore = genericityScore;
      return this;
    }

    public Builder setPolarity(final Polarity polarity) {
      this.polarity = polarity;
      return this;
    }

    public Builder setTense(final Tense tense) {
      this.tense = tense;
      return this;
    }

    public Builder setDirectionOfChange(final DirectionOfChange directionOfChange) {
      this.directionOfChange = directionOfChange;
      return this;
    }

    public Builder setPatternID(final Symbol patternID) {
      this.patternID = patternID;
      return this;
    }

    public Builder setSemanticPhraseStart(final Integer semanticPhraseStart) {
      this.semanticPhraseStart = semanticPhraseStart;
      return this;
    }

    public Builder setSemanticPhraseEnd(final Integer semanticPhraseEnd) {
      this.semanticPhraseEnd = semanticPhraseEnd;
      return this;
    }

    public Builder setModel(final Symbol model) {
      this.model = model;
      return this;
    }


    public Builder setEventTypes(final List<EventType> eventTypes) {
      this.eventTypes = Lists.newArrayList(checkNotNull(eventTypes));
      return this;
    }

    public Builder setFactorTypes(final List<EventType> factorTypes) {
      this.factorTypes = Lists.newArrayList(checkNotNull(factorTypes));
      return this;
    }

    public Builder setAnchors(final List<Anchor> anchors) {
      this.anchors = Lists.newArrayList(checkNotNull(anchors));
      return this;
    }

    private Symbol eventType;
    private Symbol patternID;
    private Modality modality;
    private double modalityScore;
    private Polarity polarity;
    private Genericity genericity;
    private double genericityScore;
    private Tense tense;
    private DirectionOfChange directionOfChange;

    private Proposition anchorProp;
    private SynNode anchorNode;

    private List<Argument> arguments;
    private final Indicator indicator;
    private final GainLoss gainLoss;
    private double score;

    //private String contextWindow;
    private Integer semanticPhraseStart;
    private Integer semanticPhraseEnd;
    private Symbol model;
    private List<EventType> eventTypes;
    private List<EventType> factorTypes;
    private List<Anchor> anchors;
  }

  public static boolean containsMentionOfType(final SentenceTheory sentence,
      final Symbol type) {
    for (final EventMention em : sentence.eventMentions()) {
      if (em.type() == type) {
        return true;
      }
    }
    return false;
  }

  /*
   * Guava Functions.
   */

  /**
   * Returns a Guava Function which gets the type of an event mention. When we someday
   * are able to move to Java 8, this will be deprecated and eventually removed.
   */
  public static Function<EventMention, Symbol> typeFunction() {
    return new Function<EventMention, Symbol>() {
      @Override
      public Symbol apply(final EventMention input) {
        return input.type();
      }
    };
  }

  /**
   * Returns a Guava Function which gets the anchor node of an event mention.  When we
   * someday are able to move to Java 8, this will be deprecated and eventually removed.
   */
  @SuppressWarnings("deprecation")
  public static Function<EventMention, SynNode> anchorNodeFunction() {
    return AnchorNode;
  }

  /**
   * Prefer {@link #anchorNodeFunction()}.
   */
  @Deprecated
  public static final Function<EventMention, SynNode> AnchorNode =
      new Function<EventMention, SynNode>() {
        @Override
        public SynNode apply(EventMention input) {
          return input.anchorNode();
        }
      };

}

