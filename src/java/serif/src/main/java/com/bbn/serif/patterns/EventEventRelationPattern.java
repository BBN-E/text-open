package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.matching.EventEventRelationMatchingPattern;
import com.bbn.serif.patterns.matching.EventEventRelationPatternMatch;
import com.bbn.serif.patterns.matching.PatternMatch;
import com.bbn.serif.patterns.matching.PatternMatchState;
import com.bbn.serif.patterns.matching.PatternReturns;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Set;

public final class EventEventRelationPattern extends ExtractionPattern implements
        EventEventRelationMatchingPattern {

    private final Set<Symbol> relationTypes;
    private final Set<Symbol> triggers;
    private final Set<Symbol> blockedTriggers;

    /**
     * getter method for relationTypes
     */
    public Set<Symbol> getRelationTypes() {
        return this.relationTypes;
    }

    /**
     * getter method for triggers
     */
    public Set<Symbol> getTriggers() {
        return this.triggers;
    }

    /**
     * getter method for blockedTriggers
     */
    public Set<Symbol> getBlockedTriggers() {
        return this.blockedTriggers;
    }

    @Override
    public EventEventRelationPattern.Builder modifiedCopyBuilder() {
        EventEventRelationPattern.Builder b = new EventEventRelationPattern.Builder();
        super.modifiedCopyBuilder(b);
        b.withRelationTypes(relationTypes);
        b.withArgs(args);
        b.withOptArgs(optArgs);
        b.withBlockedArgs(blockedArgs);
        b.withTriggers(triggers);
        b.withBlockedTriggers(blockedTriggers);
        return b;
    }

    private EventEventRelationPattern(final EventEventRelationPattern.Builder builder) {
        super(builder);
        this.relationTypes = builder.relationTypes;
        this.triggers = builder.triggers;
        this.blockedTriggers = builder.blockedTriggers;
    }

    public static final class Builder extends ExtractionPattern.Builder {

        private Set<Symbol> relationTypes;
        private Set<Symbol> triggers;
        private Set<Symbol> blockedTriggers;

        public Builder() {
            this.relationTypes = new HashSet<Symbol>();
            this.triggers = new HashSet<Symbol>();
            this.blockedTriggers = new HashSet<Symbol>();
        }

        @Override
        public EventEventRelationPattern build() {
            return new EventEventRelationPattern(this);
        }


        public EventEventRelationPattern.Builder withRelationTypes(final Set<Symbol> relationTypes) {
            this.relationTypes = relationTypes;
            return this;
        }

        public EventEventRelationPattern.Builder withTriggers(final Set<Symbol> triggers) {
            this.triggers = triggers;
            return this;
        }

        public EventEventRelationPattern.Builder withBlockedTriggers(final Set<Symbol> blockedTriggers) {
            this.blockedTriggers = triggers;
            return this;
        }
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();

        // Relationtypes
        StringBuilder relationTypeSb = new StringBuilder();
        if (relationTypes.size() > 0)
            relationTypeSb.append("[");
        boolean first = true;
        for (Symbol relationType : relationTypes) {
            if (!first) relationTypeSb.append(" ");
            first = false;
            relationTypeSb.append(relationType.asString());
        }
        if (relationTypes.size() > 0)
            relationTypeSb.append("]");

        // Triggers
        StringBuilder triggerSb = new StringBuilder();
        if (triggers.size() > 1)
            triggerSb.append("[");
        first = true;
        for (Symbol trigger : triggers) {
            if (!first) triggerSb.append(" ");
            first = false;
            triggerSb.append(trigger.asString());
        }
        if (triggers.size() > 1)
            triggerSb.append("]");

        // Args
        StringBuilder argsSb = new StringBuilder();
        for (ArgumentPattern ap : args) {
            argsSb.append(" ");
            argsSb.append(ap.toPrettyString());
        }

        sb.append(relationTypeSb.toString());
        sb.append(triggerSb.toString());
        sb.append(argsSb.toString());
        sb.append(getPrettyReturnLabel());

        String prettyString = sb.toString();
        if (prettyString.trim().length() > 0)
            return prettyString;
        else
            return "[event_event_relation]";
    }

    @Override
    public int stableHashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result;
        result = prime * result
                + ((relationTypes == null) ? 0 : Pattern.stableHashCode(relationTypes));
        result = prime * result
                + ((triggers == null) ? 0 : Pattern.stableHashCode(triggers));
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((relationTypes == null) ? 0 : relationTypes.hashCode());
        result = prime * result
                + ((triggers == null) ? 0 : triggers.hashCode());
        result = prime
                * result
                + ((blockedTriggers == null) ? 0 : blockedTriggers.hashCode());
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
        EventEventRelationPattern other = (EventEventRelationPattern) obj;
        if (relationTypes == null) {
            if (other.relationTypes != null) {
                return false;
            }
        } else if (!relationTypes.equals(other.relationTypes)) {
            return false;
        }
        if (blockedTriggers == null) {
            if (other.blockedTriggers != null) {
                return false;
            }
        } else if (!blockedTriggers.equals(other.blockedTriggers)) {
            return false;
        }
        if (triggers == null) {
            if (other.triggers != null) {
                return false;
            }
        } else if (!triggers.equals(other.triggers)) {
            return false;
        }
        return true;
    }


    // This is the match function for EventEventRelationMatchingPattern
    @Override
    public PatternReturns match(DocTheory dt, EventEventRelationMention eer,
                                PatternMatchState matchState, boolean fallThroughChildren) {
        final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, eer);
        if (cachedMatch.isPresent()) {
            return cachedMatch.get();
        }

        final boolean matches = passesRelationTypeConstraints(eer)
                && passesTriggerConstraints(eer)
                && passesArgumentConstraints(eer, dt, matchState, fallThroughChildren)
                && !isExplicitlyBlocked(dt, eer, matchState);

        if (matches) {
            // Hack: get the correct sentence theory from the left event mention
            SentenceTheory st = ((EventEventRelationMention.EventMentionArgument) eer.leftEventMention()).eventMention().sentenceTheory(dt);
            final PatternMatch match = EventEventRelationPatternMatch.of(this, dt, st, eer);
            return matchState.registerPatternMatch(this, eer, match);
        } else {
            return matchState.registerUnmatched(this, eer);
        }
    }

    private boolean isExplicitlyBlocked(final DocTheory dt, final EventEventRelationMention eer,
                                        final PatternMatchState matchState) {

        // TODO: does this do anything to handle casing?
        final Optional<String> triggerWord = eer.triggerText();
        if (triggerWord.isPresent() && blockedTriggers.contains(Symbol.from(triggerWord.get()))) {
            return true;
        }
        return false;
    }

    private boolean passesRelationTypeConstraints(final EventEventRelationMention eer) {
        return (relationTypes.isEmpty() || relationTypes.contains(eer.relationType()));
    }

    private boolean passesTriggerConstraints(EventEventRelationMention eer) {
        // TODO: trigger below is always lower case, should we be loading in trigger list to
        // always be lowercase?
        Optional<String> trigger = eer.triggerText();
        return (triggers.isEmpty() ||
                (trigger.isPresent() && triggers.contains(Symbol.from(trigger.get()))));

        // TODO prefixes
    }

    private boolean passesArgumentConstraints(EventEventRelationMention eer, DocTheory dt,
                                              PatternMatchState matchState, boolean fallThroughChildren)
    {
        boolean allEventArgPatternsMatch = true;

        EventEventRelationMention.Argument leftArg = eer.leftEventMention();
        EventEventRelationMention.Argument rightArg = eer.rightEventMention();

        if (leftArg instanceof EventEventRelationMention.EventMentionArgument && rightArg instanceof EventEventRelationMention.EventMentionArgument) {
            EventMention leftMention = ((EventEventRelationMention.EventMentionArgument) leftArg).eventMention();
            EventMention rightMention = ((EventEventRelationMention.EventMentionArgument) rightArg).eventMention();

//            System.out.println("LeftMention: " + leftMention.toString());
//            System.out.println("RightMention: " + rightMention.toString());

            SentenceTheory leftSt = leftMention.sentenceTheory(dt);
            SentenceTheory rightSt = rightMention.sentenceTheory(dt);

            for (ArgumentPattern ap : args) {
                PatternReturns leftPatternReturns = ap.match(dt, leftSt, leftArg, matchState, fallThroughChildren);
                PatternReturns rightPatternReturns = ap.match(dt, rightSt, rightArg, matchState, fallThroughChildren);
/*                if (leftPatternReturns.matched()) {
                    System.out.println("Matched left pattern: " + ap.toPrettyString() );
                }
                else if (rightPatternReturns.matched()) {
                    System.out.println("Matched right pattern: " + ap.toPrettyString());
                } else {
*/
                if (!leftPatternReturns.matched() && !rightPatternReturns.matched()) {
//                    System.out.println("Didn't match arg pattern: " + ap.toPrettyString());
                    allEventArgPatternsMatch = false;
                    break;
                }
            }
        } else {
            // TODO: Add implementations for other types of Arguments
            if (!args.isEmpty()) {
                allEventArgPatternsMatch = false;
            }
        }


        return allEventArgPatternsMatch;
    }

}
