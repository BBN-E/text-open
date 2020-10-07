package com.bbn.serif.io;

import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.serif.common.SerifException;

import org.w3c.dom.Element;

public class SerifXMLException extends SerifException {

  private static final long serialVersionUID = 1L;

  public SerifXMLException(String msg) {
    super(msg);
  }

  public SerifXMLException(String msg, Throwable t) {
    super(msg, t);
  }

  public SerifXMLException(Element e, Throwable t) {
    super(String.format("While processing element %s", XMLUtils.dumpXMLElement(e)), t);
  }

  public SerifXMLException(String msg, Element e, Throwable t) {
    super(String.format("While processing element %s, %s", XMLUtils.dumpXMLElement(e), msg), t);
  }
}
