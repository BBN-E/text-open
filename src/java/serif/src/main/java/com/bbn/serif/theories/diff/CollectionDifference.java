package com.bbn.serif.theories.diff;

import com.bbn.bue.common.StringUtils;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.Collection;

import static com.bbn.bue.common.StringUtils.PrefixWith;
import static com.google.common.base.Functions.toStringFunction;

/**
 * Represents the differences between two collections of objects.
 */
@Beta
final class CollectionDifference<CollType extends Collection<ItemType>, ItemType, DiffType extends Difference<ItemType>>
    implements Difference<CollType> {
  private final CollType left;
  private final CollType right;
  private final ImmutableList<ItemType> leftOnly;
  private final ImmutableList<ItemType> rightOnly;
  private final ImmutableList<DiffType> itemDiffs;

  CollectionDifference(final CollType left, final CollType right,
      final Iterable<? extends ItemType> leftOnly, final Iterable<? extends ItemType> rightOnly,
      final Iterable<? extends DiffType> itemDiffs) {
    this.left = left;
    this.right = right;
    this.leftOnly = ImmutableList.copyOf(leftOnly);
    this.rightOnly = ImmutableList.copyOf(rightOnly);
    this.itemDiffs = ImmutableList.copyOf(itemDiffs);
  }

  @Override
  public Optional<CollType> left() {
    return Optional.fromNullable(left);
  }

  @Override
  public Optional<CollType> right() {
    return Optional.fromNullable(right);
  }

  public ImmutableList<ItemType> leftOnly() {
    return leftOnly;
  }

  public ImmutableList<ItemType> rightOnly() {
    return rightOnly;
  }

  public ImmutableList<DiffType> differingItems() {
    return itemDiffs;
  }

  public static <CollType extends Collection<ItemType>, ItemType, DiffType extends Difference<ItemType>>
  CollectionDifference<CollType, ItemType, DiffType> from(
      final CollType left, final CollType right, final Iterable<? extends ItemType> leftOnly,
      final Iterable<? extends ItemType> rightOnly,
      final Iterable<? extends DiffType> itemDiffs) {
    return new CollectionDifference<>(left, right, leftOnly, rightOnly, itemDiffs);
  }

  @Override
  public void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    sb.append("\n");
    if (!leftOnly.isEmpty()) {
      appendCollection("leftOnly  = ", sb, indentSpaces, this.leftOnly);
    }
    if (!rightOnly.isEmpty()) {
      appendCollection("rightOnly = ", sb, indentSpaces, rightOnly);
    }
    if (!differingItems().isEmpty()) {
      sb.append(Strings.repeat(" ", indentSpaces)).append("Differing items:\n");
      for (final DiffType diff : differingItems()) {
        diff.writeTextReport(sb, indentSpaces+2);
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void appendCollection(final String msg, final StringBuilder sb, final int indentSpaces,
      final ImmutableList<ItemType> list) {
    if (!list.isEmpty()) {
      sb.append(Strings.repeat(" ", indentSpaces)).append(msg);

      if (list.size() == 1) {
        sb.append(list);
      } else {
        final String perItemPadding = Strings.repeat(" ", indentSpaces + msg.length());
        sb.append("\n").append(
            FluentIterable.from(list)
                .transform(toStringFunction())
                .transform(PrefixWith(perItemPadding))
                .join(StringUtils.unixNewlineJoiner()));
      }

      sb.append("\n");
    }
  }
}
