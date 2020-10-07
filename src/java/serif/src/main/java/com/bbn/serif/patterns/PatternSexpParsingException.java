package com.bbn.serif.patterns;

import com.bbn.bue.sexp.Sexp;

public class PatternSexpParsingException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PatternSexpParsingException(String msg) {
    super(msg);
  }

  public PatternSexpParsingException(String msg, Throwable t) {
    super(msg, t);
  }

  public PatternSexpParsingException(Sexp sexp, Throwable t) {
    super(String.format("Error parsing sexp %s", sexp.toString()), t);
  }

  public PatternSexpParsingException(String msg, Sexp sexp) {
    super(String.format("Error parsing sexp %s, %s", msg, sexp.toString()));
  }

  public PatternSexpParsingException(String msg, Sexp sexp, Throwable t) {
    super(String.format("Error parsing sexp %s, %s", sexp.toString(), msg), t);
  }
}
