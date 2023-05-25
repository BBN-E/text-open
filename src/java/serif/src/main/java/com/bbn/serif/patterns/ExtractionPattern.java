package com.bbn.serif.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ExtractionPattern extends LanguageVariantSwitchingPattern {

    protected final Pattern regexPattern;
    protected final List<ArgumentPattern> args;
    protected final List<ArgumentPattern> optArgs;
    protected final List<ArgumentPattern> blockedArgs;

    /**
     * getter method for regexPattern
     */
    public Pattern getRegexPattern() {
        return this.regexPattern;
    }

    /**
     * getter method for args
     */
    public List<ArgumentPattern> getArgs() {
        return this.args;
    }

    /**
     * getter method for optArgs
     */
    public List<ArgumentPattern> getOptArgs() {
        return this.optArgs;
    }

    /**
     * getter method for blockedArgs
     */
    public List<ArgumentPattern> getBlockedArgs() {
        return this.blockedArgs;
    }

    protected Builder modifiedCopyBuilder(Builder b) {
        super.modifiedCopyBuilder(b);
        b.withRegexPattern(regexPattern);
        b.withArgs(args);
        b.withOptArgs(optArgs);
        b.withBlockedArgs(blockedArgs);
        return b;
    }

    @Override
    public abstract Builder modifiedCopyBuilder();

    protected ExtractionPattern(final Builder builder) {
        super(builder);
        this.regexPattern = builder.regexPattern;
        this.args = builder.args;
        this.optArgs = builder.optArgs;
        this.blockedArgs = builder.blockedArgs;
    }

    public static abstract class Builder extends LanguageVariantSwitchingPattern.Builder {
        private Pattern regexPattern;
        private List<ArgumentPattern> args;
        private List<ArgumentPattern> optArgs;
        private List<ArgumentPattern> blockedArgs;

        public Builder() {
            this.args = new ArrayList<ArgumentPattern>();
            this.optArgs = new ArrayList<ArgumentPattern>();
            this.blockedArgs = new ArrayList<ArgumentPattern>();
        }

        @Override
        public abstract ExtractionPattern build();

        public ExtractionPattern.Builder withRegexPattern(final Pattern regexPattern) {
            this.regexPattern = regexPattern;
            return this;
        }

        public ExtractionPattern.Builder withArgs(final List<ArgumentPattern> args) {
            List<ArgumentPattern> modifiableList = new ArrayList<>(args);
            Collections.sort(modifiableList);
            this.args = modifiableList;
            return this;
        }

        public ExtractionPattern.Builder withOptArgs(final List<ArgumentPattern> optArgs) {
            this.optArgs = optArgs;
            return this;
        }

        public ExtractionPattern.Builder withBlockedArgs(final List<ArgumentPattern> blockedArgs) {
            this.blockedArgs = blockedArgs;
            return this;
        }
    }

    @Override
    public int stableHashCode() {
        final int prime = 31;
        int result = super.stableHashCode();
        result = prime * result + ((args == null) ? 0 : Pattern.stableHashCode(args));
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((args == null) ? 0 : args.hashCode());
        result = prime * result
                + ((blockedArgs == null) ? 0 : blockedArgs.hashCode());
        result = prime * result + ((optArgs == null) ? 0 : optArgs.hashCode());
        result = prime * result
                + ((regexPattern == null) ? 0 : regexPattern.hashCode());
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
        ExtractionPattern other = (ExtractionPattern) obj;
        if (args == null) {
            if (other.args != null) {
                return false;
            }
        } else if (!args.equals(other.args)) {
            return false;
        }
        if (blockedArgs == null) {
            if (other.blockedArgs != null) {
                return false;
            }
        } else if (!blockedArgs.equals(other.blockedArgs)) {
            return false;
        }
        if (optArgs == null) {
            if (other.optArgs != null) {
                return false;
            }
        } else if (!optArgs.equals(other.optArgs)) {
            return false;
        }
        if (regexPattern == null) {
            if (other.regexPattern != null) {
                return false;
            }
        } else if (!regexPattern.equals(other.regexPattern)) {
            return false;
        }
        return true;
    }
}
