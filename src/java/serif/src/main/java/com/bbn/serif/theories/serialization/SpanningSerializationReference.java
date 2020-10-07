package com.bbn.serif.theories.serialization;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Spanning;

public interface SpanningSerializationReference {

  public Spanning toSpanning(DocTheory dt);
}
