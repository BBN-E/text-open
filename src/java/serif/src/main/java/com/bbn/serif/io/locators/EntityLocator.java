package com.bbn.serif.io.locators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jdeyoung on 4/29/15.
 */
public final class EntityLocator {

  @JsonProperty("docID")
  private final String docID;
  @JsonProperty("index")
  private final int idx;

  private EntityLocator(String docID, int idx) {
    this.docID = docID;
    this.idx = idx;
  }

  @JsonCreator
  public static EntityLocator create(String docID, int idx) {
    return new EntityLocator(docID, idx);
  }

  @JsonProperty("index")
  public int getIdx() {
    return idx;
  }

  @JsonProperty("docID")
  public String getDocID() {
    return docID;
  }
}
