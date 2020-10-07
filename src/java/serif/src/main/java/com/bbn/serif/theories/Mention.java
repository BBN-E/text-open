package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Mention implements Spanning, HasExternalID {

  public static final Function<Mention, Integer> AtomicStringLength =
      new Function<Mention, Integer>() {
        @Override
        public Integer apply(final Mention m) {
          return m.atomicCasedTextString().length();
        }
      };
  public static final Ordering<Mention> ByAtomicStringLength =
      Ordering.natural().onResultOf(AtomicStringLength);

  public enum Type {
    NONE, NAME, PRON, DESC, PART,
    APPO, LIST, INFL, NEST
  }

  public static Type typeForSymbol(final Symbol s) {
    final Type ret = symbolToType.get(checkNotNull(s));

    if (ret != null) {
      return ret;
    } else {
      throw new UnknownMentionTypeException(
          String.format("Unknown mention type %s. Known are %s.", s, symbolToType.keySet()));
    }
  }

  public static Symbol symbolForType(final Type t) {
    return symbolToType.inverse().get(t);
  }

  public boolean isPopulated() {
    return mentionType != Type.NONE;
  }

  /**
   * @deprecated This method is unreliable. Prefer to inject some ontology and explicitly check that
   * the entity type is recognized.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public boolean isOfRecognizedType() {
    return entityType.isNotUndetOrOth();
  }

  public int sentenceNumber() {
    return node().span().tokenSequence().sentenceIndex();
  }

  public double confidence() {
    return confidence;
  }

  public double linkConfidence() {
    return linkConfidence;
  }

  public SentenceTheory sentenceTheory(final DocTheory dt) {
    return node().sentenceTheory(dt);
  }

  public Collection<Proposition.MentionArgument> propositionArguments(final DocTheory dt) {
    return sentenceTheory(dt).propositions().propositionArgumentsFor(this);
  }

  public Type mentionType() {
    return mentionType;
  }

  public EntityType entityType() {
    return entityType;
  }

  public EntitySubtype entitySubtype() {
    return entitySubtype;
  }

  public SynNode node() {
    return node;
  }

  public Optional<String> model(){return this.model;}

  public Optional<String> pattern(){return this.pattern;}

  public int indexInSentence(final DocTheory dt) {
    return sentenceTheory(dt).mentions().asList().indexOf(this);
  }

  private static final Set<Type> coreMentionTypes = ImmutableSet.of(
      Type.NAME, Type.PRON, Type.DESC);

  public boolean ofCoreMentionType() {
    return coreMentionTypes.contains(mentionType);
  }

  public boolean hasAppositiveRelationship() {
    if (mentionType == Type.APPO) {
      return true;
    } else {
      final Optional<Mention> parent = parent();
      if (parent.isPresent()) {
        return parent.get().mentionType == Type.APPO;
      }
    }
    return false;
  }

  public boolean isPartOfAppositive() {
    // Top of appositive doesn't count
    if (mentionType == Type.APPO || mentionType == Type.NONE) {
      return false;
    }

    final Optional<Mention> parent = parent();
    if (!parent.isPresent()) {
      return false;
    }
    if (parent.get().mentionType != Type.APPO) {
      return false;
    }
    final Optional<Mention> child1 = parent().get().child();
    if (!child1.isPresent()) {
      return false;
    }
    if (child1.get() == this) {
      return true;
    }
    final Optional<Mention> child2 = child1.get().next();
    if (!child2.isPresent()) {
      return false;
    }
    if (child2.get() == this) {
      return true;
    }
    return false;
  }

		/*public boolean hasIntendedType() {
                        return intendedType.isDetermined();
		}
		public EntityType intendedType() { return intendedType; }*/

  public SynNode head() {
    switch (mentionType) {
      case NAME:
        if (child != null) { // want the preterminal head - marj
          Mention m = child;
          while (m.child != null) {
            m = m.child;
          }
          return m.node;
        } else {
          return node;
        }
      case PRON:
        return node;
      case APPO:
      case LIST:
        return child().get().node();
      default:
        return node.headPreterminal();
    }
  }

  /**
   * Returns true if this mention is of type NAME or if it is of type NONE and its parent's isName()
   * is true.
   */
  public boolean isName() {
    return mentionType == Type.NAME || (parent != null && parent.isName());
  }

  public boolean isPronoun() {
    return mentionType == Type.PRON;
  }

  /**
   * Returns true is isName() and mention has no children
   */
  public boolean isBaseName() {
    return isName() && child == null;
  }

  /**
   * Replicates the behavior of SERIF's getAtomicHead.  If this mention is a name, it returns the
   * head preterminal's parent if it exists. Otherwise it returns the head's preterminal.  This is
   * provided for compatibility; often headWordOrName is to be preferred to deal nicely with
   * non-NAME mentions with NAME heads.
   */
  public SynNode atomicHead() {
    final SynNode preTerm = node().headPreterminal();

    if (mentionType == Type.NAME && preTerm.parent().isPresent()) {
      //System.err.println("Atomic head of "+this.toString()+" is "+preTerm.parent().get().toString());
      return preTerm.parent().get();
    } else {
      //System.err.println("Atomic head of "+this.toString()+" is "+preTerm.toString());
      return preTerm;
    }
  }

  /**
   * Given a mention, will return its head name, if present, or head preterminal otherwise.  See
   * SynNode's headPreterminalOrName (which this calls) or a complete description.
   */
  public SynNode headPreterminalOrName() {
    return node.headPreterminalOrName();
  }

  public Optional<Mention> parent() {
    return Optional.fromNullable(parent);
  }

  public Optional<Mention> child() {
    return Optional.fromNullable(child);
  }

  public Optional<Mention> next() {
    return Optional.fromNullable(next);
  }

  public Optional<Entity> entity(final DocTheory dt) {
    return dt.entities().entityByMention(this);
  }

  public boolean isMetonymyMention() {
    return metonymyInfo != null;
  }

  public Optional<MetonymyInfo> metonymyInfo() {
    return Optional.fromNullable(metonymyInfo);
  }

  public Optional<Mention> mostRecentAntecedent(final DocTheory dt) {
    final Optional<Entity> entOption = entity(dt);
    if (entOption.isPresent()) {
      final Entity entity = entOption.get();
      Mention antecedent = null;
      final int targetSentence = sentenceNumber();
      for (final Mention m : entity) {
        if (m.sentenceNumber() > targetSentence) {
          continue;
        }
        if (antecedent != null && m.sentenceNumber() < antecedent.sentenceNumber()) {
          continue;
        }
        if (m.mentionType() != Type.PRON) {
          if (antecedent == null ||
              antecedent.sentenceNumber() < m.sentenceNumber() ||
              antecedent.node().span().startIndex() < m.node().span().startIndex()) {
            antecedent = m;
          }
        }
      }
      return Optional.fromNullable(antecedent);
    } else {
      return Optional.absent();
    }
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  /**
   * Gets all names among those given which equal this mention in type and token span.
   * If this {@link Mention}'s {@link #isName()} returns false, this will return an
   * empty set.
   *
   * This returns a {@code Set} rather than an {@link Optional} because we don't ban
   * multiple names covering the same span.
   */
  public ImmutableSet<Name> correspondingNames(Names names) {
    if (isName()) {
      final ImmutableSet.Builder<Name> ret = ImmutableSet.builder();
      for (final Name name : names) {
        if (name.type().name().equalTo(entityType().name())
            && span().equals(name.span())) {
          ret.add(name);
        }
      }
      return ret.build();
    }
    return ImmutableSet.of();
  }

  /**
   * Returns true if and only if one of the child mentions of this mention has the name
   * {@link Mention.Type}
   */
  public boolean hasNamedChild() {
    for (Optional<Mention> child = child(); child.isPresent(); child = child.get().next()) {
      if (Mention.Type.NAME.equals(child.get().mentionType())) {
        return true;
      }
    }
    return false;
  }

  public void setEntityType(EntityType entityType){
    this.entityType = entityType;
  }

  public void setModel(Optional<String> model){this.model = model;}

  public void setPattern(Optional<String> pattern){this.pattern = pattern;}

  /**
   * Prefer atomicHead().span().tokenizedText()
   */
  @Deprecated
  public String atomicCasedTextString() {
    return atomicHead().toCasedTextString();
  }

  private EntityType entityType;
  private final EntitySubtype entitySubtype;
  private final SynNode node;
  private final Type mentionType;

  private final double confidence;
  private final double linkConfidence;

  private Mention parent = null;
  private Mention child = null;
  private Mention next = null;

  private Optional<String> pattern;
  private Optional<String> model;

  @Nullable private final Symbol external_id;

  /**
   * For constuction only
   */
  public void setParent(final Mention m) {
    if (parent == null) {
      parent = checkNotNull(m);
    } else {
      throw new RuntimeException("Parent has already been set");
    }
  }

  /**
   * For constuction only
   */
  public void setChild(final Mention m) {
    if (child == null) {
      child = checkNotNull(m);
    } else {
      throw new RuntimeException("Child has already been set");
    }
  }

  /**
   * For constuction only
   */
  public void setNext(final Mention m) {
    if (next == null) {
      next = checkNotNull(m);
    } else {
      throw new RuntimeException("Next has already been set");
    }
  }

  private final MetonymyInfo metonymyInfo;

  // exists to preserve backward compatibilty until we can bump versions
  @Deprecated
  public Mention(final SynNode node, final Type mentionType, final EntityType entityType,
      final EntitySubtype entitySubtype,
      final MetonymyInfo metonymyInfo, final double confidence, final double linkConfidence) {
    this(node, mentionType, entityType, entitySubtype, metonymyInfo, confidence,
        linkConfidence, null);
  }

  /**
   * Do not create {@link Mention}s directly; use {@link SynNode#setMention(Type, EntityType,
   * EntitySubtype, MetonymyInfo, double, double, Symbol)}.
   */
  @Deprecated
  public Mention(final SynNode node, final Type mentionType, final EntityType entityType,
      final EntitySubtype entitySubtype,
      final MetonymyInfo metonymyInfo, final double confidence, final double linkConfidence,
      @Nullable final Symbol external_id) {
    this.external_id = external_id;
    this.node = checkNotNull(node);
    this.mentionType = checkNotNull(mentionType);
    this.entityType = checkNotNull(entityType);
    this.entitySubtype = checkNotNull(entitySubtype);
    this.metonymyInfo = metonymyInfo;
    this.confidence = confidence;
    this.linkConfidence = linkConfidence;
  }

  private static BiMap<Symbol, Type> symbolToType = ImmutableBiMap.<Symbol, Type>builder()
      .put(Symbol.from("none"), Type.NONE)
      .put(Symbol.from("name"), Type.NAME)
      .put(Symbol.from("pron"), Type.PRON)
      .put(Symbol.from("desc"), Type.DESC)
      .put(Symbol.from("part"), Type.PART)
      .put(Symbol.from("appo"), Type.APPO)
      .put(Symbol.from("list"), Type.LIST)
      .put(Symbol.from("infl"), Type.INFL)
      .put(Symbol.from("nest"), Type.NEST).build();

  public static class UnknownMentionTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownMentionTypeException(final String msg) {
      super(msg);
    }
  }

  @Override
  public Span span() {
    return node.span();
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  @Override
  public String toString() {
    return entityType + "." + entitySubtype + "/" + mentionType + "/" + node;
  }

  /**
   * Returns the entity type of this mention if it has one (as a Symbol) or the headword if not.
   * This is commonly used in features.
   *
   * @deprecated This method is dangerous because {@link EntityType#isNotUndetOrOth()} is not reliably
   * populated.
   */
  @Deprecated
  public Symbol entityTypeOrHeadword() {
    if (entityType().isNotUndetOrOth()) {
      return entityType().name();
    } else {
      return node().headWord();
    }
  }

  public static final class MetonymyInfo {

    public MetonymyInfo(final EntityType role, final EntityType intendedType) {
      this.role = checkNotNull(role);
      this.intendedType = checkNotNull(intendedType);
    }

    public EntityType role() {
      return role;
    }

    public EntityType intendedType() {
      return intendedType;
    }

    private final EntityType role;
    private final EntityType intendedType;
  }


  public static Function<Mention, EntityType> entityTypeFunction() {
    return new Function<Mention, EntityType>() {
      @Override
      public EntityType apply(final Mention m) {
        return m.entityType;
      }
    };
  }

  public static Predicate<Mention> OfType(final Type type) {
    return new Predicate<Mention>() {
      @Override
      public boolean apply(final Mention m) {
        return m.mentionType == type;
      }
    };
  }

  public static Predicate<Mention> HasChild = new Predicate<Mention>() {
    @Override
    public boolean apply(final Mention m) {
      return m.child().isPresent();
    }
  };

}
