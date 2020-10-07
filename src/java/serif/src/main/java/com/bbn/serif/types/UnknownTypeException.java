package com.bbn.serif.types;

import com.bbn.serif.common.SerifException;

public class UnknownTypeException extends SerifException {

  private static final long serialVersionUID = 1L;

  public UnknownTypeException(String msg) {
    super(msg);
  }
}
