package com.bbn.serif.theories.flexibleevents;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.temporal.Timex2Time;
import com.bbn.serif.theories.HasExternalID;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSpan;
import com.bbn.serif.theories.TokenSpanning;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.actors.GeoResolvedActor;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FlexibleEventMentionArgument implements TokenSpanning, HasExternalID {

  private final Symbol role;
  private final TokenSpan tokenSpan;
  @Nullable
  private final SynNode synNode;
  @Nullable
  private final Mention mention;
  @Nullable
  private final ValueMention valueMention;
  @Nullable
  private final Timex2Time temporalResolution;
  @Nullable
  private final GeoResolvedActor geographicalResolution;
  @Nullable
  private final Symbol externalID;

  @Override
  public String toString() {
    final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
    helper.add("role", role);
    if (tokenSpan != null) {
      helper.add("tokSpan", tokenSpan);
    }
    if (synNode != null) {
      helper.add("synNode", synNode);
    }
    if (mention != null) {
      helper.add("mention", mention);
    }
    if (valueMention != null) {
      helper.add("valueMention", valueMention);
    }
    if (temporalResolution != null) {
      helper.add("temporal", temporalResolution);
    }
    if (geographicalResolution != null) {
      helper.add("geo", geographicalResolution);
    }
    return helper.toString();
  }


  public Symbol role() {
    return role;
  }

  @Override
  public TokenSpan tokenSpan() {
    return tokenSpan;
  }

  public Optional<SynNode> synNode() {
    return Optional.fromNullable(synNode);
  }

  public Optional<Mention> mention() {
    return Optional.fromNullable(mention);
  }

  public Optional<ValueMention> valueMention() {
    return Optional.fromNullable(valueMention);
  }

  public Optional<Timex2Time> temporalResolution() {
    return Optional.fromNullable(temporalResolution);
  }

  public Optional<GeoResolvedActor> geographicalResolution() {
    return Optional.fromNullable(geographicalResolution);
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(externalID);
  }

  // nullable for use in line with existing from() factories
  public FlexibleEventMentionArgument withExternalID(@Nullable final Symbol externalID) {
    return new FlexibleEventMentionArgument(role, tokenSpan, synNode, mention, valueMention,
        temporalResolution, geographicalResolution, externalID);
  }

  private FlexibleEventMentionArgument(final Symbol role, final TokenSpan tokenSpan,
      @Nullable final SynNode synNode,
      @Nullable final Mention mention,
      @Nullable final ValueMention valueMention,
      @Nullable final Timex2Time temporalResolution,
      @Nullable final GeoResolvedActor geographicalResolution,
      @Nullable final Symbol externalID) {
    this.role = role;
    this.tokenSpan = tokenSpan;
    this.synNode = synNode;
    this.mention = mention;
    this.valueMention = valueMention;
    this.temporalResolution = temporalResolution;
    this.geographicalResolution = geographicalResolution;
    this.externalID = externalID;
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final TokenSpan tokenSpan) {
    final Builder builder = Builder.builderFromRole(role).tokenSpan(tokenSpan);
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final TokenSpan tokenSpan, final Timex2Time temporalResolution) {
    final Builder builder = Builder.builderFromRole(role).tokenSpan(tokenSpan);
    if (temporalResolution != null) {
      builder.temporalResolution(temporalResolution);
    }
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final TokenSpan tokenSpan, final GeoResolvedActor geographicalResolution) {
    final Builder builder = Builder.builderFromRole(role).tokenSpan(tokenSpan);
    if (geographicalResolution != null) {
      builder.geoGraphicalResolution(geographicalResolution);
    }
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final SynNode synNode) {
    final Builder builder = Builder.builderFromRole(role).synNode(synNode);
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final SynNode synNode, final Timex2Time temporalResolution) {
    final Builder builder = Builder.builderFromRole(role).synNode(synNode);
    if (temporalResolution != null) {
      builder.temporalResolution(temporalResolution);
    }
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final SynNode synNode, final GeoResolvedActor geographicalResolution) {
    final Builder builder = Builder.builderFromRole(role).synNode(synNode);
    if (geographicalResolution != null) {
      builder.geoGraphicalResolution(geographicalResolution);
    }
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final Mention mention,
      final Optional<GeoResolvedActor> geographicalResolution) {
    final Builder builder = Builder.builderFromRole(role).mention(mention);
    if (geographicalResolution.isPresent()) {
      builder.geoGraphicalResolution(geographicalResolution.get());
    }
    return builder.build();
  }

  public static FlexibleEventMentionArgument from(final Symbol role,
      final ValueMention valueMention,
      final Optional<Timex2Time> temporalResolution) {
    final Builder builder = Builder.builderFromRole(role).valueMention(valueMention);
    if (temporalResolution.isPresent()) {
      builder.temporalResolution(temporalResolution.get());
    }
    return builder.build();
  }

  private static final class Builder {

    private final Symbol role;
    private TokenSpan tokenSpan;
    private SynNode synNode;
    private Mention mention;
    private ValueMention valueMention;
    private Timex2Time temporalResolution;
    private GeoResolvedActor geographicalResolution;
    @Nullable
    private Symbol externalID = null;

    private Builder(final Symbol role) {
      this.role = checkNotNull(role);
    }

    Builder tokenSpan(final TokenSpan tokenSpan) {
      this.tokenSpan = checkNotNull(tokenSpan);
      return this;
    }

    Builder synNode(final SynNode synNode) {
      this.synNode = checkNotNull(synNode);
      this.tokenSpan = synNode.tokenSpan();
      return this;
    }

    Builder mention(final Mention mention) {
      this.mention = checkNotNull(mention);
      this.tokenSpan = mention.tokenSpan();
      return this;
    }

    Builder valueMention(final ValueMention valueMention) {
      this.valueMention = checkNotNull(valueMention);
      this.tokenSpan = valueMention.tokenSpan();
      return this;
    }

    Builder temporalResolution(final Timex2Time temporalResolution) {
      this.temporalResolution = checkNotNull(temporalResolution);
      return this;
    }

    Builder geoGraphicalResolution(final GeoResolvedActor geographicalResolution) {
      this.geographicalResolution = checkNotNull(geographicalResolution);
      return this;
    }

    Builder externalID(final Symbol externalID) {
      this.externalID = externalID;
      return this;
    }

    static Builder builderFromRole(final Symbol role) {
      return new Builder(role);
    }

    FlexibleEventMentionArgument build() {
      return new FlexibleEventMentionArgument(role, tokenSpan, synNode,
          mention, valueMention, temporalResolution,
          geographicalResolution, externalID);
    }
  }

}
