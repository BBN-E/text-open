package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.types.EntityType;
import com.bbn.serif.types.ValueType;

import java.util.ArrayList;
import java.util.List;

public class ValueMentionPattern extends Pattern {

  private final boolean mustBeSpecificDate;
  private final List<ValueType> valueTypes;
  private final boolean mustBeRecentDate;
  private final boolean mustBeFutureDate;
  private final Pattern regexPattern;
  private static final int RECENT_DAYS_CONSTRAINT = 30;
  private final List<ComparisonConstraint> comparisonConstraints;
  private final DateStatus activityDateStatus;

  /**
   * getter method for mustBeSpecificDate
   */
  public boolean isMustBeSpecificDate() {
    return this.mustBeSpecificDate;
  }

  /**
   * getter method for valueType
   */
  public List<ValueType> getValueTypes() {
    return this.valueTypes;
  }

  /**
   * getter method for mustBeRecentDate
   */
  public boolean isMustBeRecentDate() {
    return this.mustBeRecentDate;
  }

  /**
   * getter method for mustBeFutureDate
   */
  public boolean isMustBeFutureDate() {
    return this.mustBeFutureDate;
  }

  /**
   * getter method for regexPattern
   */
  public Pattern getRegexPattern() {
    return this.regexPattern;
  }

  /**
   * getter method for comparisonConstraints
   */
  public List<ComparisonConstraint> getComparisonConstraints() {
    return this.comparisonConstraints;
  }

  /**
   * getter method for activityDateStatus
   */
  public DateStatus getActivityDateStatus() {
    return this.activityDateStatus;
  }

  private ValueMentionPattern(final Builder builder) {
    super(builder);
    this.mustBeSpecificDate = builder.mustBeSpecificDate;
    this.valueTypes = builder.valueTypes;
    this.mustBeRecentDate = builder.mustBeRecentDate;
    this.mustBeFutureDate = builder.mustBeFutureDate;
    this.regexPattern = builder.regexPattern;
    this.comparisonConstraints = builder.comparisonConstraints;
    this.activityDateStatus = builder.activityDateStatus;
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withMustBeSpecificDate(mustBeSpecificDate);
    b.withValueTypes(valueTypes);
    b.withMustBeRecentDate(mustBeRecentDate);
    b.withMustBeFutureDate(mustBeFutureDate);
    b.withComparisonConstraints(comparisonConstraints);
    b.withActivityDateStatus(activityDateStatus);
    b.withRegexPattern(regexPattern);
    return b;
  }

  public static enum DateStatus {
    IN_RANGE, OUT_OF_RANGE, NOT_SPECIFIC, TOO_BROAD
  }

  public static class Builder extends Pattern.Builder {

    private boolean mustBeSpecificDate;
    private List<ValueType> valueTypes;
    private boolean mustBeRecentDate;
    private boolean mustBeFutureDate;
    private Pattern regexPattern;
    private List<ComparisonConstraint> comparisonConstraints;
    private DateStatus activityDateStatus;

    public Builder() {
      this.comparisonConstraints = new ArrayList<ComparisonConstraint>();
      this.valueTypes = new ArrayList<ValueType>();
    }

    @Override
    public ValueMentionPattern build() {
      return new ValueMentionPattern(this);
    }

    public Builder withMustBeSpecificDate(final boolean mustBeSpecificDate) {
      this.mustBeSpecificDate = mustBeSpecificDate;
      return this;
    }

    public Builder withValueTypes(final List<ValueType> valueTypes) {
      this.valueTypes = valueTypes;
      return this;
    }

    public Builder withMustBeRecentDate(final boolean mustBeRecentDate) {
      this.mustBeRecentDate = mustBeRecentDate;
      return this;
    }

    public Builder withMustBeFutureDate(final boolean mustBeFutureDate) {
      this.mustBeFutureDate = mustBeFutureDate;
      return this;
    }

    public Builder withRegexPattern(final Pattern regexPattern) {
      this.regexPattern = regexPattern;
      return this;
    }

    public Builder withComparisonConstraints(
        final List<ComparisonConstraint> comparisonConstraints) {
      this.comparisonConstraints = comparisonConstraints;
      return this;
    }

    public Builder withComparisonConstraintsAdd(final ComparisonConstraint comparisonConstraint) {
      this.comparisonConstraints.add(comparisonConstraint);
      return this;
    }

    public Builder withActivityDateStatus(final DateStatus activityDateStatus) {
      this.activityDateStatus = activityDateStatus;
      return this;
    }
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();

    // Acetypes
    StringBuilder typesSb = new StringBuilder();
    if (valueTypes.size() > 0)
      typesSb.append("[");
    boolean first = true;
    for (ValueType type : valueTypes) {
      if (!first) typesSb.append(" ");
      first = false;
      typesSb.append(type.name());
    }
    if (valueTypes.size() > 0)
      typesSb.append("]");

    sb.append(typesSb.toString());
    sb.append(getPrettyReturnLabel());

    String prettyString = sb.toString();
    if (prettyString.length() > 0)
      return prettyString;
    else
      return "[valuemention]";
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime
        * result
        + ((activityDateStatus == null) ? 0 : activityDateStatus
        .hashCode());
    result = prime
        * result
        + ((comparisonConstraints == null) ? 0 : comparisonConstraints
        .hashCode());
    result = prime * result + (mustBeFutureDate ? 1231 : 1237);
    result = prime * result + (mustBeRecentDate ? 1231 : 1237);
    result = prime * result + (mustBeSpecificDate ? 1231 : 1237);
    result = prime * result
        + ((regexPattern == null) ? 0 : regexPattern.hashCode());
    result = prime * result
        + ((valueTypes == null) ? 0 : valueTypes.hashCode());
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
    ValueMentionPattern other = (ValueMentionPattern) obj;
    if (activityDateStatus != other.activityDateStatus) {
      return false;
    }
    if (comparisonConstraints == null) {
      if (other.comparisonConstraints != null) {
        return false;
      }
    } else if (!comparisonConstraints.equals(other.comparisonConstraints)) {
      return false;
    }
    if (mustBeFutureDate != other.mustBeFutureDate) {
      return false;
    }
    if (mustBeRecentDate != other.mustBeRecentDate) {
      return false;
    }
    if (mustBeSpecificDate != other.mustBeSpecificDate) {
      return false;
    }
    if (regexPattern == null) {
      if (other.regexPattern != null) {
        return false;
      }
    } else if (!regexPattern.equals(other.regexPattern)) {
      return false;
    }
    if (valueTypes == null) {
      if (other.valueTypes != null) {
        return false;
      }
    } else if (!valueTypes.equals(other.valueTypes)) {
      return false;
    }
    return true;
  }
}
