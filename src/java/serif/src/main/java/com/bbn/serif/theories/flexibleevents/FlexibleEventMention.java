package com.bbn.serif.theories.flexibleevents;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.HasExternalID;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FlexibleEventMention implements HasExternalID {
  private final Symbol type;
  private final ImmutableMap<Symbol,Symbol> attributes;
  private final ImmutableList<FlexibleEventMentionArgument> arguments;
  @Nullable
  private final Symbol externalID;

  private FlexibleEventMention(final Symbol type,
      final Map<Symbol, Symbol> attributes,
      final Iterable<FlexibleEventMentionArgument> arguments,
      @Nullable Symbol externalID) {
    this.type = checkNotNull(type);
    this.attributes = ImmutableMap.copyOf(attributes);
    this.arguments = ImmutableList.copyOf(arguments);
    this.externalID = externalID;
  }

  public static FlexibleEventMention from(final Symbol type,
      final Map<Symbol,Symbol> attributes, final Iterable<FlexibleEventMentionArgument> arguments) {
    return new FlexibleEventMention(type, attributes, arguments, null);
  }

  public ImmutableList<FlexibleEventMentionArgument> arguments() {
    return arguments;
  }

  public ImmutableMap<Symbol, Symbol> attributes() {
    return attributes;
  }

  public Symbol type() {
    return type;
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(externalID);
  }

  public ImmutableSet<FlexibleEventMentionArgument> getArgumentsByRole(final Symbol role){
	  ImmutableSet.Builder<FlexibleEventMentionArgument> argumentsByRole = ImmutableSet.builder();
	  for(FlexibleEventMentionArgument arg: arguments){
		  if(arg.role().equalTo(role)){
			  argumentsByRole.add(arg);
		  }
	  }
	  return argumentsByRole.build();
  }

  public ImmutableList<FlexibleEventMentionArgument> getArgumentsByRoleAndSentenceIndex(
		  final Symbol role, final int sentIndex) {
    ImmutableList.Builder<FlexibleEventMentionArgument> builder = new ImmutableList.Builder<>();
    for (FlexibleEventMentionArgument arg : arguments) {
      if (arg.role().equalTo(role) && arg.tokenSpan().overlapsSentenceIndex(sentIndex)) {
        builder.add(arg);
      }
    }
    return builder.build();
  }

  public ImmutableList<FlexibleEventMentionArgument> getArgumentsBySentenceIndexExcludingRole(final int sentIndex, final Symbol roleToExclude) {
    ImmutableList.Builder<FlexibleEventMentionArgument> builder = new ImmutableList.Builder<>();
    for (FlexibleEventMentionArgument arg : arguments) {
      if (arg.tokenSpan().overlapsSentenceIndex(sentIndex) && !arg.role().equalTo(roleToExclude)) {
        builder.add(arg);
      }
    }
    return builder.build();
  }

  public static Builder builderForType(final Symbol type) {
    return new Builder(type);
  }

  public static final class Builder {

    Builder(final Symbol type) {
      this.type = checkNotNull(type);
    }

    public Builder withExternalID(Symbol externalID) {
      this.externalID = checkNotNull(externalID);
      return this;
    }

    public Builder withArguments(Iterable<? extends FlexibleEventMentionArgument> args) {
      this.arguments.addAll(args);
      return this;
    }

    public Builder withAttributes(Map<Symbol, Symbol> attributes) {
      this.attributes.putAll(attributes);
      return this;
    }

    public Builder withArgument(final FlexibleEventMentionArgument flexibleEventMentionArgument) {
      this.arguments.add(flexibleEventMentionArgument);
      return this;
    }

    public Builder withAttribute(final Symbol key, final Symbol value) {
      this.attributes.put(key, value);
      return this;
    }

    public FlexibleEventMention build() {
      return new FlexibleEventMention(type, attributes.build(), arguments.build(), externalID);
    }

    private final Symbol type;
    @Nullable
    private Symbol externalID = null;
    final ImmutableList.Builder<FlexibleEventMentionArgument> arguments = ImmutableList.builder();
    final ImmutableMap.Builder<Symbol, Symbol> attributes = ImmutableMap.builder();
  }

  @Override
  public String toString() {
    final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
    helper.add("type", type);
    if (!arguments().isEmpty()) {
      helper.add("args", arguments());
    }
    if (!attributes.isEmpty()) {
      helper.add("attributes", attributes);
    }
    if (externalID != null) {
      helper.add("extID", externalID);
    }
    return helper.toString();
  }
}
