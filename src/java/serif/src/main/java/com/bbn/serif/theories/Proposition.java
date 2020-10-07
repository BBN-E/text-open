package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.types.Attribute;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Proposition implements Spanning {

  public PredicateType predType() {
    return predType;
  }

  public Optional<SynNode> predHead() {
    return Optional.fromNullable(predHead);
  }

  public Optional<SynNode> particle() {
    return Optional.fromNullable(particle);
  }

  public Optional<SynNode> adverb() {
    return Optional.fromNullable(adverb);
  }

  public Optional<SynNode> negation() {
    return Optional.fromNullable(negation);
  }

  public Optional<SynNode> modal() {
    return Optional.fromNullable(modal);
  }

  public int numArgs() {
    return args.size();
  }

  public Argument arg(final int idx) {
    return args.get(idx);
  }

  public List<Argument> args() {
    return args;
  }

  public void addArg(final Argument a) {
    args.add(a);
  }

  public Optional<Symbol> predSymbol() {
    if (predHead != null) {
      return Optional.of(predHead.headWord());
    } else {
      return Optional.absent();
    }
  }

  public Set<Status> statuses() {
    return statuses;
  }

  public boolean hasStatus(final Status status) {
    return statuses.contains(status);
  }

  public boolean hasAnyStatus() {
    return !statuses.isEmpty();
  }

  /**
   * This is a deprecated alias for {@link #firstMentionOfRole(Symbol)} to support legacy code that gives a second
   * (unused) argument.
   *
   * @deprecated use {@link #firstMentionOfRole(Symbol)}
   */
  @Deprecated
  public Optional<Mention> firstMentionOfRole(final Symbol role, final Mentions unused) {
    return firstMentionOfRole(role);
  }

  /**
   * Returns the first {@link MentionArgument} argument with the specified role.
   */
  public Optional<Mention> firstMentionOfRole(final Symbol role) {
    for (final Argument arg : args) {
      if (arg instanceof MentionArgument && arg.role().isPresent() && arg.role().get() == role) {
        final MentionArgument mentionArg = (MentionArgument) arg;
        return Optional.of(mentionArg.mention());
      }
    }
    return Optional.absent();
  }

  /**
   * This is a deprecated alias for {@link #roleOfMention(Mention)} to support legacy code that gives a second
   * (unused) argument.
   *
   * @deprecated use {@link #roleOfMention(Mention)}
   */
  @Deprecated
  public Optional<Symbol> roleOfMention(final Mention m, final Mentions unused) {
    return roleOfMention(m);
  }

  /**
   * Returns the role of the first {@link MentionArgument} argument with the specified mention. All list arguments'
   * children are checked for the specific mention as well.
   */
  public Optional<Symbol> roleOfMention(final Mention m) {
    for (final Argument arg : args) {
      if (arg instanceof MentionArgument) {
        final Mention argMention = ((MentionArgument) arg).mention();
        if (argMention.mentionType() == Mention.Type.LIST) {
          Optional<Mention> memberMent = argMention.child();
          while (memberMent.isPresent()) {
            if (memberMent.get() == m) {
              return arg.role();
            }
            memberMent = memberMent.get().child();
          }
        } else if (argMention == m) {
          return arg.role();
        }
      }
    }
    return Optional.absent();
  }

  /**
   * Returns the proposition of the first {@link PropositionArgument} argument with the specified role.
   */
  public Optional<Proposition> firstPropositionOfRole(final Symbol role) {
    for (final Argument arg : args) {
      if (arg instanceof PropositionArgument && arg.role().isPresent()
          && arg.role().get() == role) {
        return Optional.of(((PropositionArgument) arg).proposition());
      }
    }
    return Optional.absent();
  }

  /**
   * A convenience method for getting the 'extent' of a proposition.  The extent of a proposition is not completely
   * defined. For these purposes, the extent is defined as:
   * <ul>
   * <li>start token: the first token of any argument (or argument of arguments) or the extent of the pred head</li>
   * <li>end token: last token of any argument (or argument of arguments) or the extent of the pred head</li>
   * </ul>
   */
  @Override
  public TokenSequence.Span span() {
    final List<TokenSequence.Span> spans = Lists.newArrayList();

    if (predHead != null) {
      spans.add(predHead.span());
    }

    if (adverb != null) {
      spans.add(adverb.span());
    }

    if (modal != null) {
      spans.add(modal.span());
    }

    if (particle != null) {
      spans.add(particle.span());
    }

    if (negation != null) {
      spans.add(negation.span());
    }

    for (final Argument arg : args) {
      spans.add(arg.span());
    }

    if (spans.isEmpty()) {
      throw new RuntimeException("Proposition has no span!");
    }

    return spans.get(0).tokenSequence().maxSpan(spans);
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  public SentenceTheory sentenceTheory(final DocTheory docTheory) {
    return span().sentenceTheory(docTheory);
  }

  private final PredicateType predType;
  private final SynNode predHead;
  private final SynNode particle;
  private final SynNode adverb;
  private final SynNode negation;
  private final SynNode modal;
  private final List<Argument> args;
  private final ImmutableSet<Status> statuses;

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("[prop/").append(predType).append(" ");
    if (predHead != null) {
      sb.append("head=").append(predHead).append(";");
    }
    if (particle != null) {
      sb.append("particle=").append(particle).append(";");
    }
    if (adverb != null) {
      sb.append("adverb=").append(adverb).append(";");
    }
    if (negation != null) {
      sb.append("negation=").append(negation).append(";");
    }
    if (modal != null) {
      sb.append("modal=").append(modal).append(";");
    }
    if (!statuses.isEmpty()) {
      sb.append("status=").append(statuses).append(";");
    }
    sb.append("args=").append(args);
    sb.append("]");
    return sb.toString();
  }

  public Proposition(final PredicateType predType, final SynNode predHead, final SynNode particle,
      final SynNode adverb, final SynNode negation, final SynNode modal,
      final Iterable<? extends ArgumentBuilder> argBuilders, final Iterable<Status> statuses) {
    this.predType = checkNotNull(predType);
    this.predHead = predHead;
    this.particle = particle;
    this.adverb = adverb;
    this.negation = negation;
    this.modal = modal;
    this.args = argsFromBuilders(checkNotNull(argBuilders));
    this.statuses = ImmutableSet.copyOf(checkNotNull(statuses));
  }

  /**
   * Constructor for making a new prop from an old one, but with different arguments. This is useful
   * for rebuilding propositions with additional arguments.
   */
  public Proposition(final Proposition prop,
      final Iterable<? extends Argument> args) {
    this.predType = checkNotNull(prop.predType);
    this.predHead = prop.predHead;
    this.particle = prop.particle;
    this.adverb = prop.adverb;
    this.negation = prop.negation;
    this.modal = prop.modal;
    this.args = ImmutableList.copyOf(args);
    this.statuses = ImmutableSet.copyOf(checkNotNull(prop.statuses));
  }

  private List<Argument> argsFromBuilders(
      final Iterable<? extends ArgumentBuilder> argumentBuilders) {
    final List<Argument> newArgs = new ArrayList<Argument>();

    for (final ArgumentBuilder argBuilder : argumentBuilders) {
      newArgs.add(argBuilder.build(this));
    }

    return newArgs;
  }

  public static Predicate<Proposition> OfType(final List<PredicateType> types) {
    return new Predicate<Proposition>() {
      @Override
      public boolean apply(final Proposition prop) {
        return types.contains(prop.predType());
      }
    };
  }

  public static Predicate<Proposition> OfType(final PredicateType type) {
    return new Predicate<Proposition>() {
      @Override
      public boolean apply(final Proposition prop) {
        return prop.predType == type;
      }
    };
  }

  public static Predicate<Proposition> AtLeastNArgs(final int N) {
    return new Predicate<Proposition>() {
      @Override
      public boolean apply(final Proposition prop) {
        return prop.args.size() >= N;
      }
    };
  }

  public Optional<Mention> correspondingMention(Mentions mentions) {
    for (final Mention mention : mentions) {
      if (mention.head() == predHead) {
        return Optional.of(mention);
      }
    }
    return Optional.absent();
  }

  public abstract static class Argument {

    public Optional<Symbol> role() {
      return Optional.fromNullable(role);
    }

    public abstract TokenSequence.Span span();

    public Proposition parentProposition() {
      return prop;
    }

    static public final Symbol REF_ROLE = Symbol.from("<ref>");
    static public final Symbol SUB_ROLE = Symbol.from("<sub>");
    static public final Symbol OBJ_ROLE = Symbol.from("<obj>");
    static public final Symbol IOBJ_ROLE = Symbol.from("<iobj>");
    ;
    static public final Symbol POSS_ROLE = Symbol.from("<poss>");
    static public final Symbol TEMP_ROLE = Symbol.from("<temp>");
    static public final Symbol LOC_ROLE = Symbol.from("<loc>");
    static public final Symbol MEMBER_ROLE = Symbol.from("<member>");
    static public final Symbol UNKNOWN_ROLE = Symbol.from("<unknown>");

    private final Proposition prop;
    private final Symbol role;

    protected Argument(final Proposition prop, final Symbol role) {
      this.prop = checkNotNull(prop);
      this.role = role;
    }

    public abstract ArgumentBuilder modifiedCopyBuilder();

    public boolean roleIs(Symbol probeRole) {
      return role != null && role == probeRole;
    }
  }

  public abstract static class ArgumentBuilder {

    public ArgumentBuilder(final Symbol role) {
      this.role = role;
    }

    public abstract Argument build(Proposition prop);

    protected final Symbol role;
  }

  public static final class MentionArgument extends Argument {

    private MentionArgument(final Proposition prop, final Symbol role, final Mention mention) {
      super(prop, role);
      this.mention = checkNotNull(mention);
    }

    public Mention mention() {
      return mention;
    }

    @Override
    public TokenSequence.Span span() {
      return mention.span();
    }

    private final Mention mention;

    @Override
    public String toString() {
      return String.format("%s-->%s", role(), mention);
    }

    @Override
    public ArgumentBuilder modifiedCopyBuilder() {
      return new MentionArgumentBuilder(this.role().orNull(), mention);
    }
  }

  public static final class MentionArgumentBuilder extends ArgumentBuilder {

    public MentionArgumentBuilder(final Symbol role, final Mention mention) {
      super(role);
      this.mention = mention;
    }

    @Override
    public MentionArgument build(final Proposition prop) {
      return new MentionArgument(prop, role, mention);
    }

    private final Mention mention;
  }

  public static final class TextArgument extends Argument {

    private TextArgument(final Proposition prop, final Symbol role, final SynNode node) {
      super(prop, role);
      this.node = checkNotNull(node);
    }

    public SynNode node() {
      return node;
    }

    @Override
    public TokenSequence.Span span() {
      return node.highestHead().span();
    }

    @Override
    public String toString() {
      return String.format("%s-->%s", role(), node);
    }

    private final SynNode node;

    @Override
    public ArgumentBuilder modifiedCopyBuilder() {
      return new TextArgumentBuilder(this.role().orNull(), node);
    }
  }

  public static final class TextArgumentBuilder extends ArgumentBuilder {

    public TextArgumentBuilder(final Symbol role, final SynNode node) {
      super(role);
      this.node = node;
    }

    @Override
    public TextArgument build(final Proposition prop) {
      return new TextArgument(prop, role, node);
    }

    private final SynNode node;
  }

  public static final class PropositionArgument extends Argument {

    private PropositionArgument(final Proposition parentProp, final Symbol role,
        final Proposition prop) {
      super(parentProp, role);
      this.prop = checkNotNull(prop);
    }

    public Proposition proposition() {
      return prop;
    }

    @Override
    public TokenSequence.Span span() {
      return prop.span();
    }

    @Override
    public String toString() {
      return String.format("%s-->%s", role(), prop);
    }

    private final Proposition prop;

    @Override
    public ArgumentBuilder modifiedCopyBuilder() {
      return new PropositionArgumentBuilder(this.role().orNull(), prop);
    }
  }

  public static final class PropositionArgumentBuilder extends ArgumentBuilder {

    public PropositionArgumentBuilder(final Symbol role, final Proposition prop) {
      super(role);
      this.prop = checkNotNull(prop);
    }

    @Override
    public PropositionArgument build(final Proposition parentProp) {
      return new PropositionArgument(parentProp, role, prop);
    }

    private final Proposition prop;
  }

  public final static class Status extends Attribute {

    public static final Status DEFAULT = new Status("Default");
    public static final Status IF = new Status("If");
    public static final Status FUTURE = new Status("Future");
    public static final Status NEGATIVE = new Status("Negative");
    public static final Status ALLEGED = new Status("Alleged");
    public static final Status MODAL = new Status("Modal");
    public static final Status UNRELIABLE = new Status("Unreliable");

    private static final ImmutableList<Status> VALUES =
        ImmutableList.of(DEFAULT, IF, FUTURE, NEGATIVE, ALLEGED, MODAL, UNRELIABLE);

    public static Status from(final Symbol s) {
      final Status ret = stringToPropositionStatus.get(s);
      if (ret != null) {
        return ret;
      } else {
        throw new RuntimeException(String.format("Unknown proposition status %s", s));
      }
    }

    public static final Function<Symbol, Status> FromSymbol = new Function<Symbol, Status>() {
      @Override
      public Status apply(final Symbol s) {
        return Status.from(s);
      }
    };

    private Status(final String name) {
      super(name);
    }

    private static final ImmutableMap<Symbol, Status> stringToPropositionStatus =
        Maps.uniqueIndex(VALUES, Attribute.nameFunction());
  }

  public final static class PredicateType extends Attribute {

    public static final PredicateType VERB = new PredicateType("verb");
    public static final PredicateType COPULA = new PredicateType("copula");
    public static final PredicateType MODIFIER = new PredicateType("modifier");
    public static final PredicateType NOUN = new PredicateType("noun");
    public static final PredicateType POSS = new PredicateType("poss");
    public static final PredicateType LOC = new PredicateType("loc");
    public static final PredicateType SET = new PredicateType("set");
    public static final PredicateType NAME = new PredicateType("name");
    public static final PredicateType PRONOUN = new PredicateType("pronoun");
    public static final PredicateType COMP = new PredicateType("comp");
    public static final PredicateType DEPENDENCY = new PredicateType("dependency");

    public static final ImmutableList<PredicateType> VALUES =
        ImmutableList.<PredicateType>builder().add(VERB).add(COPULA)
            .add(MODIFIER).add(NOUN).add(POSS).add(LOC).add(SET).add(NAME).add(PRONOUN).add(COMP)
            .add(DEPENDENCY)
            .build();

    private static final ImmutableSet<Proposition.PredicateType> DEFINITIONAL_TYPES =
        ImmutableSet.of(
            Proposition.PredicateType.NOUN, Proposition.PredicateType.PRONOUN,
            Proposition.PredicateType.LOC, Proposition.PredicateType.SET);

    private static final ImmutableSet<Proposition.PredicateType> VERB_TYPES = ImmutableSet.of(
        Proposition.PredicateType.COMP, Proposition.PredicateType.COPULA,
        Proposition.PredicateType.VERB);

    public boolean isDefinitional() {
      return DEFINITIONAL_TYPES.contains(this);
    }

    public boolean isVerbal() {
      return VERB_TYPES.contains(this);
    }

    public static PredicateType from(final Symbol s) {
      return stringToPredicateType.get(s);
    }

    private PredicateType(final String name) {
      super(name);
    }

    private static final ImmutableMap<Symbol, PredicateType> stringToPredicateType =
        Maps.uniqueIndex(VALUES, Attribute.nameFunction());
  }

}
