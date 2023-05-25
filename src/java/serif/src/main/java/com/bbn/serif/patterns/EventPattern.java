package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.HasStableHashCode;

import com.bbn.serif.patterns.matching.EventMatchingPattern;
import com.bbn.serif.patterns.matching.EventPatternMatch;
import com.bbn.serif.patterns.matching.PatternMatch;
import com.bbn.serif.patterns.matching.PatternMatchState;
import com.bbn.serif.patterns.matching.PatternReturns;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Set;

public final class EventPattern extends ExtractionPattern implements
        EventMatchingPattern, HasStableHashCode {

    private final Set<Symbol> eventTypes;
    private final Set<Symbol> anchors;
    private final Set<Symbol> blockedAnchors;

    /**
     * getter method for eventTypes
     */
    public Set<Symbol> getEventTypes() {
        return this.eventTypes;
    }

    /**
     * getter method for anchors
     */
    public Set<Symbol> getAnchors() {
        return this.anchors;
    }

    /**
     * getter method for blockedAnchors
     */
    public Set<Symbol> getBlockedAnchors() {
        return this.blockedAnchors;
    }

    @Override
    public EventPattern.Builder modifiedCopyBuilder() {
        EventPattern.Builder b = new EventPattern.Builder();
        super.modifiedCopyBuilder(b);
        b.withEventTypes(eventTypes);
        b.withAnchors(anchors);
        b.withBlockedAnchors(blockedAnchors);
        return b;
    }

    private EventPattern(final EventPattern.Builder builder) {
        super(builder);
        this.eventTypes = builder.eventTypes;
        this.anchors = builder.anchors;
        this.blockedAnchors = builder.blockedAnchors;
    }

    public static final class Builder extends ExtractionPattern.Builder {
        private Set<Symbol> eventTypes;
        private Set<Symbol> anchors;
        private Set<Symbol> blockedAnchors;

        public Builder() {
            this.eventTypes = new HashSet<Symbol>();
            this.anchors = new HashSet<Symbol>();
            this.blockedAnchors = new HashSet<Symbol>();
        }

        @Override
        public EventPattern build() {
            return new EventPattern(this);
        }

        public EventPattern.Builder withEventTypes(final Set<Symbol> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        public EventPattern.Builder withAnchors(final Set<Symbol> anchors) {
            this.anchors = anchors;
            return this;
        }

        public EventPattern.Builder withBlockedAnchors(final Set<Symbol> blockedAnchors) {
            this.blockedAnchors = blockedAnchors;
            return this;
        }

    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();

        // Regex pattern
        StringBuilder regexSb = new StringBuilder();
        if (regexPattern != null) {
            regexSb.append(regexPattern.toPrettyString());
        }

        // Eventtypes
        StringBuilder eventTypeSb = new StringBuilder();
        if (eventTypes.size() > 0)
            eventTypeSb.append("[");
        boolean first = true;
        for (Symbol eventType : eventTypes) {
            if (!first) eventTypeSb.append(" ");
            first = false;
            eventTypeSb.append(eventType.asString());
        }
        if (eventTypes.size() > 0)
            eventTypeSb.append("]");

        // Anchors
        StringBuilder anchorSb = new StringBuilder();
        if (anchors.size() > 1)
            anchorSb.append("[");
        first = true;
        for (Symbol anchor : anchors) {
            if (!first) anchorSb.append(" ");
            first = false;
            anchorSb.append(anchor.asString());
        }
        if (anchors.size() > 1)
            anchorSb.append("]");

        sb.append(regexSb);
        sb.append(" ");
        sb.append(eventTypeSb.toString());
        sb.append(anchorSb.toString());
        sb.append(getPrettyReturnLabel());

        String prettyString = sb.toString();
        if (prettyString.trim().length() > 0)
            return prettyString;
        else
            return "[event]";
    }

    @Override
    public int stableHashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result;
        result = prime * result
                + ((eventTypes == null) ? 0 : Pattern.stableHashCode(eventTypes));
        result = prime * result
                + ((anchors == null) ? 0 : Pattern.stableHashCode(anchors));
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((eventTypes == null) ? 0 : eventTypes.hashCode());
        result = prime
                * result
                + ((blockedAnchors == null) ? 0 : blockedAnchors.hashCode());
        result = prime * result
                + ((anchors == null) ? 0 : anchors.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EventPattern other = (EventPattern) obj;
        if (eventTypes == null) {
            if (other.eventTypes != null) {
                return false;
            }
        } else if (!eventTypes.equals(other.eventTypes)) {
            return false;
        }
        if (blockedAnchors == null) {
            if (other.blockedAnchors != null) {
                return false;
            }
        } else if (!blockedAnchors.equals(other.blockedAnchors)) {
            return false;
        }
        if (anchors == null) {
            if (other.anchors != null) {
                return false;
            }
        } else if (!anchors.equals(other.anchors)) {
            return false;
        }
        return true;
    }

    // This is the match function for EventMentionMatchingPattern
    @Override
    public PatternReturns match(DocTheory dt, SentenceTheory st, EventMention em,
                                PatternMatchState matchState, boolean fallThroughChildren) {
        final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, em);
        if (cachedMatch.isPresent()) {
            return cachedMatch.get();
        }

        final boolean matches = passesEventTypeConstraints(em)
                && passesAnchorConstraints(dt, em)
                && !isExplicitlyBlocked(dt, em, matchState)
                && matchesRegexConstraint(dt, st, em, matchState);

        if (matches) {
            final PatternMatch match = EventPatternMatch.of(this, dt, st, em);
            return matchState.registerPatternMatch(this, em, match);
        } else {
            return matchState.registerUnmatched(this, em);
        }
    }

    private boolean isExplicitlyBlocked(final DocTheory dt, final EventMention em,
                                        final PatternMatchState matchState) {

        // TODO: does this do anything to handle casing?
        final Symbol anchorText = getAnchorText(em, dt);
        if (blockedAnchors.contains(anchorText)) {
            return true;
        }
        return false;
    }

    private boolean passesEventTypeConstraints(final EventMention em) {
        return (eventTypes.isEmpty() || eventTypes.contains(em.type()));
    }

    private boolean passesAnchorConstraints(final DocTheory dt, final EventMention em) {
        // TODO: anchor below is always lower case, should we be loading in anchor list to
        // always be lowercase?
        final Symbol anchorText = getAnchorText(em, dt);
        return (anchors.isEmpty() || anchors.contains(anchorText));

        // TODO prefixes
    }

    private boolean matchesRegexConstraint(final DocTheory dt, final SentenceTheory st,
                                           final EventMention em, final PatternMatchState matchState)
    {
        if (regexPattern == null)
            return true;

        PatternReturns regexPatternReturns = ((RegexPattern) regexPattern).match(dt, st, em, matchState, false);
        return regexPatternReturns.matched();
    }

    private Symbol getAnchorText(EventMention em, DocTheory dt) {
        Symbol anchorText = Symbol.from("");
        SentenceTheory st = em.sentenceTheory(dt);
        if (em.semanticPhraseStart().isPresent() && em.semanticPhraseEnd().isPresent()) {
            anchorText = Symbol.from(st.tokenSequence().span(
                    em.semanticPhraseStart().get(), em.semanticPhraseEnd().get()).tokenizedText().utf16CodeUnits());
        }
        else if (em.anchors().size() > 0) {
            int earliestOffset = st.tokenSequence().size() - 1;
            int latestOffset = 0;
            for (EventMention.Anchor anchor : em.anchors()) {
                if (anchor.anchorNode() != null) {
                    earliestOffset = Math.min(earliestOffset, anchor.anchorNode().tokenSpan().startTokenIndexInclusive());
                    latestOffset = Math.max(latestOffset, anchor.anchorNode().tokenSpan().endTokenIndexInclusive());
                }
            }
            anchorText = Symbol.from(st.tokenSequence().span(earliestOffset, latestOffset).tokenizedText().utf16CodeUnits());
        }
        else {
            if (em.anchorNode() != null) {
                anchorText = Symbol.from(em.anchorNode().tokenSpan().tokenizedText(dt).utf16CodeUnits());
            }
        }
        return anchorText;
    }
}
