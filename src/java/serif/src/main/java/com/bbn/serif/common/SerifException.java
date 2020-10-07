package com.bbn.serif.common;

public class SerifException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SerifException() {
    super();
  }

  public SerifException(Throwable t) {
    super(t);
  }

  public SerifException(String msg) {
    super(msg);
  }

  public SerifException(String msg, Throwable t) {
    super(msg, t);
  }
}
