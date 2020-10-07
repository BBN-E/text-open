package com.bbn.serif.patterns;


public final class TextPattern extends Pattern {

  private final String text;
  private final boolean addSpaces;
  private final boolean rawText;

  /**
   * getter method for text
   */
  public String getText() {
    return this.text;
  }

  /**
   * getter method for addSpaces
   */
  public boolean isAddSpaces() {
    return this.addSpaces;
  }

  /**
   * getter method for rawText
   */
  public boolean isRawText() {
    return this.rawText;
  }

  private TextPattern(final Builder builder) {
    super(builder);
    this.text = builder.text;
    this.addSpaces = builder.addSpaces;
    this.rawText = builder.rawText;
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();
    sb.append(" ");
    if (text != null)
      sb.append(text);
    sb.append(getPrettyReturnLabel());
    return sb.toString();
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withText(text);
    b.withAddSpaces(addSpaces);
    b.withRawText(rawText);
    return b;
  }

  public static class Builder extends Pattern.Builder {

    private String text;
    private boolean addSpaces = true;
    private boolean rawText;

    public Builder() {
    }

    @Override
    public TextPattern build() {
      return new TextPattern(this);
    }

    public Builder withText(final String text) {
      this.text = text;
      return this;
    }

    public Builder withAddSpaces(final boolean addSpaces) {
      this.addSpaces = addSpaces;
      return this;
    }

    public Builder withRawText(final boolean rawText) {
      this.rawText = rawText;
      return this;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (addSpaces ? 1231 : 1237);
    result = prime * result + (rawText ? 1231 : 1237);
    result = prime * result + ((text == null) ? 0 : text.hashCode());
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
    TextPattern other = (TextPattern) obj;
    if (addSpaces != other.addSpaces) {
      return false;
    }
    if (rawText != other.rawText) {
      return false;
    }
    if (text == null) {
      if (other.text != null) {
        return false;
      }
    } else if (!text.equals(other.text)) {
      return false;
    }
    return true;
  }


}
