package com.bbn.serif.theories;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.Mention.MetonymyInfo;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a node in a syntax tree.
 *
 * {@code SynNode} objects are almost immutable. The exception is their links to {@link Mention}.
 * When a {@code SynNode} is first created, this link does not exist. Such a link can be created
 * using {@link #setMention(Mention.Type, EntityType, EntitySubtype, MetonymyInfo, double, double, Symbol)}.
 * When all {@code Mention} links are complete, all {@code SynNode}s should be sealed using the
 * convenience method {@link #sealAllSynNodes(DocTheory)}.  After sealing, {@code SynNodes} are
 * immutable. All operations on unsealed {@code SynNode}s are undefined.
 */
@SuppressWarnings("WeakerAccess")
public final class SynNode implements Iterable<SynNode>, Spanning, HasHeadPreterminal {

  private SynNode parent;
  private final Symbol tag;
  private List<Mention> mentions;

  private final List<SynNode> children;
  private final TokenSequence.Span tokenSpan;
  private final int headIndex;


  private SynNode(final Symbol tag, final List<SynNode> children, final Optional<Integer> headIndex,
      final TokenSequence.Span tokenSpan) {
    this.children =
        (children != null) ? ImmutableList.copyOf(children) : ImmutableList.<SynNode>of();
    this.tag = checkNotNull(tag);
    this.tokenSpan = checkNotNull(tokenSpan);
    this.headIndex = headIndex.isPresent() ? headIndex.get() : -1;
    this.mentions = new ArrayList<>();

    checkValidity();
  }

  private void checkValidity() {
    SynNode previousChild = null;
    for (final SynNode child : this.children) {
      checkArgument(this.tokenSpan.contains(child.tokenSpan()),
          "A SynNode must contain its children");
      if (previousChild != null) {
        checkArgument(child.tokenSpan().startsAfter(previousChild.tokenSpan()), "SynNode children"
            + "must be specified in token order");
      }
      previousChild = child;
    }
  }

  private void setParent(final SynNode p) {
    parent = p;
  }

  public SentenceTheory sentenceTheory(final DocTheory dt) {
    return span().sentenceTheory(dt);
  }

  public Symbol tag() {
    return tag;
  }

  public Optional<SynNode> parent() {
    return Optional.fromNullable(parent);
  }


  public boolean parentIs(SynNode probe) {
    checkNotNull(probe);
    return parent == probe;
  }

  public int nChildren() {
    return children.size();
  }

  public List<SynNode> children() {
    return children;
  }

  public SynNode child(final int idx) {
    return children.get(idx);
  }

  @Override
  public Iterator<SynNode> iterator() {
    return children.iterator();
  }

  public int headIndex() {
    return headIndex;
  }

  @Override
  public TokenSequence.Span span() {
    return tokenSpan;
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  @Deprecated
  public Optional<Mention> mention() {
    // We cannot assume each SynNode could have 0 or 1 Mention anymore. Please use mentions() above.
    if(this.mentions.size()>0)return Optional.of(this.mentions.get(0));
    return Optional.absent();
  }

  public ImmutableList<Mention> mentions(){
    return ImmutableList.copyOf(this.mentions);
  }

  public Optional<SynNode> closestProjectionWithMention() {
    SynNode curNode = this;

    while (curNode.mentions.size() < 1) {
      if (curNode.parent == null || curNode.parent.head() != curNode) {
        break;
      }
      curNode = curNode.parent;
    }

    if (curNode.mentions.size()> 0) {
      return Optional.of(curNode);
    } else {
      return Optional.absent();
    }
  }

  /**
   * The highest node for which this node is the head.
   */
  public SynNode maximalProjection() {
    SynNode curNode = this;

    while (curNode.parent().isPresent() &&
        curNode.parent().get().head() == curNode) {

      curNode = curNode.parent().get();
    }
    return curNode;
  }

  public SynNode head() {
    if (children.isEmpty()) {
      return this;
    } else {
      return children.get(headIndex);
    }
  }

  public GornAddress gornAddress() {
    final List<Integer> address = Lists.newArrayList();

    for (Optional<SynNode> curNode = Optional.of(this);
         curNode.isPresent(); curNode = curNode.get().parent()) {
      final SynNode realCurNode = curNode.get();
      final Optional<SynNode> parent = realCurNode.parent();
      if (parent.isPresent()) {
        address.add(parent.get().children().indexOf(realCurNode));
      } else {
        address.add(0);
      }
    }

    return new GornAddress(Lists.reverse(address));
  }

  public boolean isHeadChild() {
    if (parent != null) {
      final SynNode parHead = parent.head();
      if (parHead == this) {
        return true;
      }
    }
    return false;
  }

  public boolean isTerminal() {
    return children.isEmpty();
  }

  public boolean isPreterminal() {
    return children.size() == 1 && children.get(0).isTerminal();
  }

  public Optional<Symbol> singleWord() {
    if (isTerminal()) {
      return Optional.of(tag);
    } else if (children.size() == 1) {
      return children.get(0).singleWord();
    } else {
      return Optional.absent();
    }
  }

  @Override
  public SynNode headPreterminal() {
    if (isPreterminal()) {
      return this;
    } else if (isTerminal()) {
      return parent;
    } else {
      return children.get(headIndex).headPreterminal();
    }
  }

  public Symbol headWord() {
    if (isTerminal()) {
      return tag;
    } else {
      return children.get(headIndex).headWord();
    }
  }

  public Symbol headPOS() {
    return headPreterminal().tag();
  }

  public SynNode highestHead() {
    if (parent != null) {
      if (parent.head() != this) {
        return this;
      } else {
        return parent.highestHead();
      }
    } else {
      return this;
    }
  }

  public int numTerminals() {
    return tokenSpan.size();
  }

  public List<Symbol> terminalSymbols() {
    final List<Symbol> ret = Lists.newArrayList();

    if (isTerminal()) {
      ret.add(tag);
    } else {
      for (final SynNode child : this) {
        ret.addAll(child.terminalSymbols());
      }
    }
    return ret;
  }

  public List<Symbol> POSSymbols() {
    final List<Symbol> ret = Lists.newArrayList();

    if (isPreterminal()) {
      ret.add(tag);
    } else {
      for (final SynNode child : this) {
        ret.addAll(child.POSSymbols());
      }
    }

    return ret;
  }

  public Optional<SynNode> nodeByTokenSpan(final Spanning spanning) {
    return nodeByTokenSpan(spanning.span());
  }

  public Optional<SynNode> nodeByTokenSpan(final TokenSequence.Span searchSpan) {
    if (tokenSpan.isSingleToken()) {
      SynNode thisNode = nthTerminal(searchSpan.startIndex());
      while (thisNode.parent != null && thisNode.parent.span().equals(searchSpan)) {
        thisNode = thisNode.parent;
      }
      return Optional.of(thisNode);
    } else {
      SynNode thisNode = coveringNodeFromTokenSpan(searchSpan);
      if (!thisNode.span().equals(searchSpan)) {
        return Optional.absent();
      }
      while (thisNode.parent != null && thisNode.span().equals(searchSpan)) {
        thisNode = thisNode.parent;
      }
      return Optional.of(thisNode);
    }
  }

  /**
   * Returns the deepest node in the tree which dominates all of the tokens in {@code searchSpan}.
   * If no such node exists, this node itself is returned. This behavior may be counter-intuitive,
   * so this method is deprecated.  For most uses, {@link #coveringNonterminalFromTokenSpan} is a
   * good replacement.
   *
   * If provided with a token span from a different sentences than this node is for, the result is
   * undefined.
   *
   * @deprecated
   */
  @Deprecated
  public SynNode coveringNodeFromTokenSpan(final TokenSequence.Span searchSpan) {
    for (final SynNode child : this) {
      if (child.span().contains(searchSpan)) {
        return child.coveringNodeFromTokenSpan(searchSpan);
      }
    }
    return this;
  }

  /**
   * Returns the deepest non-terminal at or below this node which dominates all of the tokens in
   * {@code searchSpan}.  If no such node exists, returns {@link Optional#absent}. Note that this
   * implies that if this method is called on a terminal node, it will always return {@link
   * Optional#absent}.
   *
   * If provided with a token span from a different sentences than this node is for, the result is
   * undefined.
   */
  public Optional<SynNode> coveringNonterminalFromTokenSpan(final TokenSequence.Span searchSpan) {
    if (isTerminal()) {
      return Optional.absent();
    }

    if (span().contains(searchSpan)) {
      for (final SynNode child : this) {
        final Optional<SynNode> childCovering = child.coveringNonterminalFromTokenSpan(searchSpan);
        if (childCovering.isPresent()) {
          return childCovering;
        }
      }
      return Optional.of(this);
    }
    return Optional.absent();
  }

  /**
   * Returns the smallest constituent at or below this node which covers the given inclusive
   * code point offsets or absent if no such node exists.  "Smallest" here means shortest
   * character offset span and lower in the tree (in case of unary projections).
   */
  public Optional<SynNode> smallestConstituentCovering(OffsetRange<CharOffset> offsetsInclusive) {
    final boolean thisNodeCovers = span().charOffsetRange().contains(offsetsInclusive);

    if (thisNodeCovers) {
      for (final SynNode kid : children()) {
        final Optional<SynNode> coveringConsituentAtOrUnderKid =
            kid.smallestConstituentCovering(offsetsInclusive);
        // kids are disjoint, so at most one matches
        if (coveringConsituentAtOrUnderKid.isPresent()) {
          return coveringConsituentAtOrUnderKid;
        }
      }

      // none of our kids could cover, so we must do it ourselves
      return Optional.of(this);
    } else {
      return Optional.absent();
    }
  }

  /**
   * Returns all constituents at at or below this node which are (a) covered by the given inclusive
   * code point offsets and (b) do not have an ancestor covered by these offsets.
   */
  public ImmutableSet<SynNode> maximalConstituentsCoveredBy(
      OffsetRange<CharOffset> offsetsInclusive) {
    final boolean thisNodeCovered = offsetsInclusive.contains(span().charOffsetRange());

    if (thisNodeCovered) {
      return ImmutableSet.of(this);
    } else {
      final ImmutableSet.Builder<SynNode> ret = ImmutableSet.builder();

      for (final SynNode kid : children()) {
        if (offsetsInclusive.contains(kid.span().charOffsetRange())) {
          ret.add(kid);
        } else if (offsetsInclusive.overlaps(kid.span().charOffsetRange())) {
          ret.addAll(kid.maximalConstituentsCoveredBy(offsetsInclusive));
        }
      }
      return ret.build();
    }
  }

  public Optional<Integer> ancestorDistance(final SynNode ancestor) {
    if (!ancestor.isAncestorOf(this)) {
      return Optional.absent();
    }
    int dist = 0;
    SynNode node = this;
    while (node != ancestor) {
      node = node.parent;
      ++dist;
      if (node == null) {
        return Optional.absent();
      }
    }
    return Optional.of(dist);
  }

  public boolean isAncestorOf(final SynNode node) {
    return tokenSpan.contains(node.tokenSpan);
  }

  public List<Mention> descendentMentions() {
    final List<Mention> ret = Lists.newArrayList();


    ret.addAll(mentions);


    for (final SynNode child : this) {
      ret.addAll(child.descendentMentions());
    }

    return ret;
  }

  public Optional<SynNode> previousSibling() {
    if (parent == null) {
      return Optional.absent();
    } else {
      int index = 0;
      for (final SynNode child : parent) {
        if (child == this) {
          break;
        }
        index++;
      }
      if (index - 1 >= 0) {
        return Optional.of(parent.child(index - 1));
      } else {
        return Optional.absent();
      }
    }
  }

  public Optional<SynNode> nextSibling() {
    if (parent == null) {
      return Optional.absent();
    } else {
      int index = 0;
      for (final SynNode child : parent) {
        if (child == this) {
          break;
        }
        index++;
      }
      if (index + 1 < parent.nChildren()) {
        return Optional.of(parent.child(index + 1));
      } else {
        return Optional.absent();
      }
    }
  }

  public SynNode firstTerminal() {
    SynNode ret = this;
    while (!ret.isTerminal()) {
      ret = ret.firstChild();
    }
    return ret;
  }

  /**
   * This will only return absent if the current node is itself a terminal symbol.
   */
  public Optional<SynNode> firstPreterminal() {
    if (isTerminal()) {
      return Optional.absent();
    }

    SynNode ret = this;
    while (!ret.isPreterminal()) {
      ret = ret.firstChild();
    }
    return Optional.of(ret);
  }

  /**
   * This will only return absent if the current node is itself a terminal symbol.
   */
  public Optional<SynNode> lastPreterminal() {
    if (isTerminal()) {
      return Optional.absent();
    }

    SynNode ret = this;
    while (!ret.isPreterminal()) {
      ret = ret.lastChild();
    }
    return Optional.of(ret);
  }


  /**
   * Note this can throw {@link java.lang.IndexOutOfBoundsException} if the SynNode has no
   * children.
   */
  public SynNode firstChild() {
    return child(0);
  }

  public SynNode lastChild() {
    return child(nChildren() - 1);
  }

  public SynNode lastTerminal() {
    SynNode ret = this;
    while (!ret.isTerminal()) {
      ret = ret.lastChild();
    }
    return ret;
  }

  public Optional<SynNode> nextTerminal() {
    SynNode node = this;
    while (node.parent != null && node.parent.lastChild() == node) {
      node = node.parent;
    }
    if (node.parent == null) {
      return Optional.absent();
    }
    int index = 0;
    for (final SynNode child : node.parent) {
      if (child == node) {
        break;
      }
      ++index;
    }
    ++index;
    return Optional.of(node.parent.child(index).firstTerminal());
  }

  public Optional<SynNode> previousTerminal() {
    SynNode node = this;
    while (node.parent != null && node.parent.firstChild() == node) {
      node = node.parent;
    }
    if (node.parent == null) {
      return Optional.absent();
    }
    int index = 0;
    for (final SynNode child : node.parent) {
      if (child == node) {
        break;
      }
      ++index;
    }
    --index;
    return Optional.of(node.parent.child(index).lastTerminal());
  }

  public Optional<SynNode> nextPreterminal() {
    SynNode node = this;
    while (node.parent != null && node.parent.lastChild() == node) {
      node = node.parent;
    }
    if (node.parent == null) {
      return Optional.absent();
    }
    int index = 0;
    for (final SynNode child : node.parent) {
      if (child == node) {
        break;
      }
      ++index;
    }
    ++index;
    return node.parent.child(index).firstPreterminal();
  }

  public Optional<SynNode> previousPreterminal() {
    SynNode node = this;
    while (node.parent != null && node.parent.firstChild() == node) {
      node = node.parent;
    }
    if (node.parent == null) {
      return Optional.absent();
    }
    int index = 0;
    for (final SynNode child : node.parent) {
      if (child == node) {
        break;
      }
      ++index;
    }
    --index;
    return node.parent.child(index).lastPreterminal();
  }

  public SynNode nthTerminal(final int n) {
    SynNode curNode = firstTerminal();
    for (int i = 0; i < n; ++i) {
      final Optional<SynNode> next = curNode.nextTerminal();
      if (next.isPresent()) {
        curNode = next.get();
      } else {
        throw new SerifException(
            String.format("Wanted %dth terminal symbol of SynNode, but there weren't enough", n));
      }
    }
    return curNode;
  }

  public boolean hasMention() {
    return this.mentions.size()>0;
  }

  public static final Predicate<SynNode> HasMention = new Predicate<SynNode>() {
    @Override
    public boolean apply(final SynNode s) {
      return s.hasMention();
    }
  };

  /**
   * An interface for a procedure to apply when visiting nodes using
   * {@link #preorderTraversal(PreorderVisitor)}.
   */
  public interface PreorderVisitor {

    /**
     * Returns {@code true} if {@link #preorderTraversal(PreorderVisitor)} should visit the
     * children of this node; {@code false} if they should be skipped.
     */
    boolean visitChildren(SynNode node);
  }

  /**
   * Performs a pre-order traversal on the syntax tree starting from this node.  A node's children
   * will be visited only if {@code visitor} returns {@code true} for that node.
   */
  public void preorderTraversal(PreorderVisitor visitor) {
    if (visitor.visitChildren(this)) {
      for (final SynNode kid : children()) {
        kid.preorderTraversal(visitor);
      }
    }
  }

  public boolean hasChildNodeOfTag(final Symbol tag) {
    for (final SynNode node : children()) {
      if (node.tag() == tag) {
        return true;
      }
    }
    return false;
  }

  public String toDebugString() {
    return toDebugString(0);
  }

  public String toDebugString(int indent) {
    final StringBuilder result = new StringBuilder();

    result.append(String.format("%s  <<%d:%d", tag, tokenSpan.startIndex(), tokenSpan.endIndex()));

    for(Mention mention : this.mentions){
      result.append(mention.toString());
    }

    result.append(">>  ");
    indent += 2;
    for (final SynNode child : this) {
      if (child.isTerminal()) {
        result.append(child.tag.toString());
      } else {
        result.append('\n');
        for (int i = 0; i < indent; ++i) {
          result.append(' ');
          result.append(child.toDebugString(indent));
        }
      }
    }

    result.append(") ");
    return result.toString();
  }

  @Override
  public String toString() {
    return toFlatString();
  }

  String toIndentedString(int indent) {
    final StringBuilder result = new StringBuilder();
    result.append(String.format("%s  <<%d:%d", tag, tokenSpan.startIndex(), tokenSpan.endIndex()));
    if (this.mentions.size()>0) {
      result.append(String.format(" -- Mention %s", "IMPLEMENT MENTION REPRESENTATION"));
    }
    result.append(">>  ");
    indent += 2;
    for (final SynNode child : this) {
      result.append(' ');
      if (child.isTerminal()) {
        result.append(child.tag.toString());
      } else {
        result.append('\n');
        for (int i = 0; i < indent; ++i) {
          result.append(' ');
        }
        result.append(child.toIndentedString(indent));
      }
    }

    result.append(')');
    return result.toString();
  }

  public String toPrettyParse(int indent) {
    final StringBuilder result = new StringBuilder();

    result.append('(').append(tag);
    indent += 2;

    for (final SynNode child : this) {
      result.append(' ');
      if (child.isTerminal()) {
        result.append(child.tag.toString());
      } else {
        result.append('\n');
        if (child == head()) {
          indent--;
        }
        for (int i = 0; i < indent; ++i) {
          result.append(' ');
        }
        if (child == head()) {
          result.append("^");
        }
        result.append(child.toPrettyParse(indent));
      }
    }
    result.append(')');

    return result.toString();
  }

  public String toFlatString() {
    return toFlatString(false, false);
  }

  public String toHeadMarkedFlatString() {
    return toFlatString(true, true);
  }

  private String toFlatString(final boolean markHead, final boolean isHead) {
    final StringBuilder result = new StringBuilder();
    final SynNode head = head();

    result.append('(').append(tag.toString());
    if (isHead) {
      result.append("^");
    }
    for (final SynNode child : this) {
      result.append(' ');
      if (child.isTerminal()) {
        result.append(child.tag.toString());
      } else {
        if (child == head && markHead) {
          result.append(child.toFlatString(markHead, true));
        } else {
          result.append(child.toFlatString(markHead, false));
        }
      }
    }
    result.append(')');

    return result.toString();
  }

  String toTextString() {
    final StringBuilder result = new StringBuilder();

    for (final SynNode child : this) {
      if (child.isTerminal()) {
        result.append(child.tag.toString());
        result.append(' ');
      } else {
        result.append(child.toTextString());
      }
    }

    return result.toString();
  }

  /**
   * Prefer span().tokenizedText()
   */
  @Deprecated
  public String toCasedTextString() {
    final StringBuilder result = new StringBuilder();

    int idx = 0;
    for (final SynNode child : this) {
      if (child.isTerminal()) {
        checkState(child.span().isSingleToken());
        result.append(child.tag().toString());
      } else {
        result.append(child.toCasedTextString());
      }
      if (idx != nChildren() - 1) {
        result.append(' ');
      }
      ++idx;
    }

    if (this.children.size() == 0) {
      return this.tag.toString();
    }

    return result.toString();
  }


  /**
   * This method is meant to be called only during deserialization.  Users should not call this
   * message.  An attempt to call this on a SynNode which already has a mention will result in a
   * RuntimeException.
   *
   * This method is deprecated and should be removed at the next major version bump, prefer the
   * version specifying an external_id.
   */
  @Deprecated
  public Mention setMention(final Mention.Type mentionType, final EntityType entityType,
      final EntitySubtype entitySubtype,
      final MetonymyInfo metonymyInfo, final double confidence, final double linkConfidence) {
    return setMention(mentionType, entityType, entitySubtype, metonymyInfo, confidence,
        linkConfidence, null);
  }

  /**
   * This method is meant to be called while constructing DocTheories. Users should not call this
   * method otherwise.  An attempt to call this on a SynNode which already has a mention will result
   * in a RuntimeException.
   */
  @SuppressWarnings("deprecation")
  public Mention setMention(final Mention.Type mentionType, final EntityType entityType,
      final EntitySubtype entitySubtype,
      final MetonymyInfo metonymyInfo, final double confidence, final double linkConfidence,
      final Symbol external_id) {
    checkNotNull(mentionType);
    checkNotNull(entityType);
    checkNotNull(entitySubtype);


    Mention mention = new Mention(this, mentionType, entityType,
            entitySubtype, metonymyInfo, confidence, linkConfidence, external_id);
    this.mentions.add(mention);
    return mention;
  }

  /**
   * Returns a list of nodes between this node and the root (inclusive on both ends).
   */
  public List<SynNode> pathToRoot() {
    final List<SynNode> ret = Lists.newArrayList();

    Optional<SynNode> curNode = Optional.of(this);

    while (curNode.isPresent()) {
      ret.add(curNode.get());
      curNode = curNode.get().parent();
    }

    return ret;
  }

  /**
   * Returns the minimal common dominator of two nodes.  Let A dominate B if the path from B to the
   * root passes through A (a node dominates itself). C is a common dominator of A and B is it
   * dominates both A and B.  C is the minimal common dominator of A and B if every common dominator
   * of A and B dominates C. If the tree for a sentence is multirooted or the SynNodes are from
   * different sentences, this will return Optional.absent().
   */
  public static Optional<SynNode> minimalCommonDominator(final SynNode a, final SynNode b) {
    final List<SynNode> pathToRoot = a.pathToRoot();

    for (final SynNode n : b.pathToRoot()) {
      if (pathToRoot.contains(n)) {
        return Optional.of(n);
      }
    }

    return Optional.absent();
  }

  /**
   * Returns the path from A to B in a syntax tree. If A and B have no common dominator, returns
   * Optional.absent(). Otherwise, returns an UpDownPath object (see its Javadoc) representing the
   * path between A and B.
   */
  public static Optional<UpDownPath> pathBetweenNodes(final SynNode a, final SynNode b) {
    final Optional<SynNode> commonDominatorOpt = minimalCommonDominator(a, b);

    if (!commonDominatorOpt.isPresent()) {
      return Optional.absent();
    } else {
      final SynNode commonDominator = commonDominatorOpt.get();
      final ImmutableList.Builder<SynNode> upPath = ImmutableList.builder();
      final ImmutableList.Builder<SynNode> downPath = ImmutableList.builder();

      for (final SynNode node : a.pathToRoot()) {
        if (node == commonDominator) {
          break;
        } else {
          upPath.add(node);
        }
      }

      for (final SynNode node : b.pathToRoot()) {
        if (node == commonDominator) {
          break;
        } else {
          downPath.add(node);
        }
      }

      return Optional.of(new UpDownPath(upPath.build(), commonDominator,
          downPath.build()));
    }
  }

  public List<SynNode> dominatedPreterminals() {
    if (isTerminal()) {
      return ImmutableList.of();
    }

    final ImmutableList.Builder<SynNode> ret = ImmutableList.builder();

    dominatedPreterminals(ret);

    return ret.build();
  }

  private void dominatedPreterminals(ImmutableList.Builder<SynNode> ret) {
    if (isPreterminal()) {
      ret.add(this);
    } else {
      for (final SynNode kid : children()) {
        kid.dominatedPreterminals(ret);
      }
    }
  }

  /**
   * Represents the path between two nodes in a syntax tree.  The pivot is the minimal common
   * dominator of the two nodes.  The upPath is the path from the first node to the pivot and the
   * downPath is the path from the second node to the pivot.  If one of the two nodes is the pivot,
   * the upPath or downPath, as appropriate, will be empty.
   *
   * @author rgabbard
   */
  public static final class UpDownPath {

    public UpDownPath(final List<SynNode> upPath, final SynNode pivot,
        final List<SynNode> downPath) {
      this.upPath = ImmutableList.copyOf(upPath);
      this.pivot = checkNotNull(pivot);
      this.downPath = ImmutableList.copyOf(downPath);
    }

    public List<SynNode> upPath() {
      return upPath;
    }

    public List<SynNode> downPath() {
      return downPath;
    }

    public SynNode pivot() {
      return pivot;
    }

    private final List<SynNode> upPath;
    private final SynNode pivot;
    private final List<SynNode> downPath;
  }

  public static abstract class Builder {

    private Builder(final Symbol tag) {
      this.tag = checkNotNull(tag);
    }

    public Symbol tag() {
      return tag;
    }

    public abstract SynNode build(TokenSequence ts);

    protected final Symbol tag;
    protected boolean done = false;
  }

  public static class TerminalBuilder extends Builder {

    private TerminalBuilder(final Symbol tag) {
      super(tag);
    }

    public int tokenIndex() {
      return tokenIndex;
    }

    public Builder tokenIndex(final int idx) {
      tokenIndex = idx;
      return this;
    }

    @Override
    public SynNode build(final TokenSequence ts) {
      if (done) {
        throw new UnsupportedOperationException("This builder has already been built!");
      }

      checkNotNull(ts);
      checkState(tokenIndex >= 0);

      final TokenSequence.Span tokenSpan = ts.span(tokenIndex, tokenIndex);
      final SynNode ret =
          new SynNode(tag, ImmutableList.<SynNode>of(), Optional.<Integer>absent(), tokenSpan);

      done = true;

      return ret;
    }

    private int tokenIndex = -1;
  }

  public static class NonterminalBuilder extends Builder {

    private NonterminalBuilder(final Symbol tag) {
      super(tag);
    }

    public Builder appendNonHead(final SynNode kid) {
      children.add(kid);
      return this;
    }

    public Builder appendHead(final SynNode kid) {
      checkState(headIdx == null, "Cannot set head twice for %s", tag);
      headIdx = children.size();
      children.add(kid);
      return this;
    }

    @Override
    public SynNode build(final TokenSequence ts) {
      if (done) {
        throw new UnsupportedOperationException("This builder has already been built!");
      }

      final int startTokenIndex = getStartTokenIndex();
      final int endTokenIndex = getEndTokenIndex();

      checkNotNull(ts);
      checkState(startTokenIndex >= 0);
      checkState(endTokenIndex >= 0);

      final TokenSequence.Span tokenSpan = ts.span(startTokenIndex, endTokenIndex);
      final List<SynNode> kids = ImmutableList.copyOf(children);
      final SynNode ret = new SynNode(tag, kids, Optional.fromNullable(headIdx), tokenSpan);

      for (final SynNode kid : kids) {
        kid.setParent(ret);
      }
      done = true;

      return ret;
    }

    private int getStartTokenIndex() {
      checkState(!children.isEmpty());
      return children.get(0).span().startIndex();
    }

    private int getEndTokenIndex() {
      checkState(!children.isEmpty());
      return Iterables.getLast(children, null).span().endIndex();
    }

    private final List<SynNode> children = Lists.newArrayList();
    private Integer headIdx = null;
  }

  public static TerminalBuilder terminalBuilder(final Symbol tag) {
    return new TerminalBuilder(tag);
  }

  public static NonterminalBuilder nonterminalBuilder(final Symbol tag) {
    return new NonterminalBuilder(tag);
  }

  public SynNode headPreterminalOrName() {
    if (isPreterminal()) {
      return this;
    }
    boolean isBaseName = false;
    for(Mention mention: this.mentions){
      if(mention.isBaseName()){
        isBaseName = true;
        break;
      }
    }
    if (isBaseName) {
      final SynNode preTerm = headPreterminal();

      if (preTerm.parent().isPresent()) {
        return preTerm.parent().get();
      }
    }

    return head().headPreterminalOrName();
  }

  public static final Function<SynNode, Symbol> HeadWord = new Function<SynNode, Symbol>() {
    @Override
    public Symbol apply(SynNode input) {
      return input.headWord();
    }
  };

  public static Function<SynNode, Symbol> tagFunction() {
    return TagFunction.INSTANCE;
  }


  private enum TagFunction implements Function<SynNode, Symbol> {
    INSTANCE {
      @Override
      public Symbol apply(final SynNode input) {
        return input.tag();
      }
    };
  }


  /**
   * Prefer {@link #tagFunction()}
   */
  @Deprecated
  public static Function<SynNode, Symbol> Tag = new Function<SynNode, Symbol>() {
    @Override
    public Symbol apply(SynNode input) {
      return input.tag();
    }
  };

}
