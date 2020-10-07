package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.temporal.Timex2Time;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.types.ValueType;

import com.google.common.base.Optional;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Value implements Spanning {

  //public int id() { return id; }
  public ValueType fullType() {
    return valueMention.fullType();
  }

  public Symbol type() {
    return valueMention.fullType().baseTypeSymbol();
  }

  public Symbol subtype() {
    return valueMention.fullType().baseTypeSymbol();
  }

  public ValueMention valueMention() {
    return valueMention;
  }

  @Override
  public Span span() {
    return valueMention.span();
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  public Optional<Symbol> timexVal() {
    return Optional.fromNullable(timexVal);
  }

  public Optional<Symbol> timexAnchorVal() {
    return Optional.fromNullable(timexAnchorVal);
  }

  public Optional<Symbol> timexAnchorDir() {
    return Optional.fromNullable(timexAnchorDir);
  }

  public Optional<Symbol> timexSet() {
    return Optional.fromNullable(timexSet);
  }

  public Optional<Symbol> timexMod() {
    return Optional.fromNullable(timexMod);
  }

  public Optional<Symbol> timexNonSpecific() {
    return Optional.fromNullable(timexNonSpecific);
  }

  public boolean isSpecificDate() {
    if (timexVal == null) {
      return false;
    }
    final String valString = timexVal.toString();
    return Y_EXACT_REGEX.matcher(valString).matches()
        || Y_HYPHEN.matcher(valString).matches();
  }

  /**
   * Returns the time this values represents as a Timex2Time, if applicable. Otherwise, returns
   * absent.
   */
  @SuppressWarnings("deprecation")
  public Optional<Timex2Time> asTimex2() {
    if (timexVal().isPresent()) {
      Timex2Time.Builder builder = Timex2Time.builderWithValue(timexVal);
      if (timexMod != null) {
        builder = builder.withModifierFromString(timexMod.toString());
      }
      if (timexSet != null) {
        builder = builder.withIsSetFromTimexBoolean(timexSet);
      }
      if (timexAnchorVal != null) {
        builder = builder.withAnchorValue(timexAnchorVal);
      }
      if (timexAnchorDir != null) {
        builder = builder.withAnchorDirection(timexAnchorDir);
      }
      if (timexNonSpecific != null) {
        builder = builder.setNonSpecificFromTimexBoolean(timexNonSpecific);
      }
      return Optional.of(builder.build());
    } else {
      return Optional.absent();
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("[Value: vm=").append(valueMention.toStringNoValue());
    if (timexVal != null) {
      sb.append(";timex=").append(timexVal);
    }
    if (timexAnchorVal != null) {
      sb.append(";timexAnchor=").append(timexAnchorVal);
    }
    if (timexAnchorDir != null) {
      sb.append(";timexAnchorDir=").append(timexAnchorDir);
    }
    if (timexSet != null) {
      sb.append(";timexSet=").append(timexSet);
    }
    if (timexMod != null) {
      sb.append(";timexMod=").append(timexMod);
    }
    if (timexNonSpecific != null) {
      sb.append(";timexNonSpecific=").append(timexNonSpecific);
    }
    sb.append("]");
    return sb.toString();
  }

  public String toStringNoValueMention() {
    final StringBuilder sb = new StringBuilder();
    sb.append("[Value: ");
    if (timexVal != null) {
      sb.append(";timex=").append(timexVal);
    }
    if (timexAnchorVal != null) {
      sb.append(";timexAnchor=").append(timexAnchorVal);
    }
    if (timexAnchorDir != null) {
      sb.append(";timexAnchorDir=").append(timexAnchorDir);
    }
    if (timexSet != null) {
      sb.append(";timexSet=").append(timexSet);
    }
    if (timexMod != null) {
      sb.append(";timexMod=").append(timexMod);
    }
    if (timexNonSpecific != null) {
      sb.append(";timexNonSpecific=").append(timexNonSpecific);
    }
    sb.append("]");
    return sb.toString();
  }

  //private final int id;
  private final ValueMention valueMention;

  private final Symbol timexVal;
  private final Symbol timexAnchorVal;
  private final Symbol timexAnchorDir;
  private final Symbol timexSet;
  private final Symbol timexMod;
  private final Symbol timexNonSpecific;


  /**
   * @deprecated Prefer {@link #createTimexValueForValueMention(ValueMention, Symbol, Symbol,
   * Symbol, Symbol, Symbol, Symbol)}.
   */
  @Deprecated
  public Value(final ValueMention valueMention, final Symbol timexVal, final Symbol timexAnchorVal,
      final Symbol timexAnchorDir, final Symbol timexSet, final Symbol timexMod,
      final Symbol timexNonSpecific) {
    this.valueMention = checkNotNull(valueMention);
    this.timexVal = timexVal;
    this.timexAnchorVal = timexAnchorVal;
    this.timexAnchorDir = timexAnchorDir;
    this.timexSet = timexSet;
    this.timexMod = timexMod;
    this.timexNonSpecific = timexNonSpecific;
  }

  @SuppressWarnings("deprecation")
  public static Value createTimexValueForValueMention(final ValueMention valueMention,
      final Symbol timexVal, final Symbol timexAnchorVal,
      final Symbol timexAnchorDir, final Symbol timexSet, final Symbol timexMod,
      final Symbol timexNonSpecific) {
    return new Value(valueMention, timexVal, timexAnchorVal, timexAnchorDir, timexSet, timexMod,
        timexNonSpecific);
  }

  private static final Pattern Y_EXACT_REGEX =
      Pattern.compile("^([12][0-9][0-9][0-9])$");
  private static final Pattern Y_HYPHEN =
      Pattern.compile("^([12][0-9][0-9][0-9])-.*");
}
