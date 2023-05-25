package com.bbn.serif.patterns.matching;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A match resulting from a {@link EventEventRelationMatchingPattern}.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class EventEventRelationPatternMatch extends AbstractPatternMatch implements PatternMatch {

    @Override
    public abstract DocTheory docTheory();

    @Override
    public abstract Optional<Pattern> pattern();

    @Override
    public abstract Optional<SentenceTheory> sentenceTheory();

    public abstract EventEventRelationMention eventEventRelationMention();

    @Override
    @Value.Check
    protected void check() {
        super.check();
        checkArgument(sentenceTheory().isPresent());
    }

    public static EventEventRelationPatternMatch of(Pattern pattern, DocTheory docTheory,
                                                           SentenceTheory sentenceTheory,
                                                           EventEventRelationMention eventEventRelationMention) {
        return ImmutableEventEventRelationPatternMatch.builder()
                .pattern(pattern)
                .docTheory(docTheory)
                .sentenceTheory(sentenceTheory)
                .eventEventRelationMention(eventEventRelationMention).build();
    }
}
