package com.bbn.serif.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Segment {

  private final ImmutableMap<String, List<FieldEntry>> internalMap;
  private final ImmutableMap<String, String> attributes;

  public Map<String, String> attributes() {
    return attributes;
  }

  public boolean hasField(String field) {
    return internalMap.containsKey(field);
  }

  public List<FieldEntry> field(String field) {
    return internalMap.get(field);
  }

  public static class FieldEntry {

    private final String value;
    private final ImmutableMap<String, String> attributes;

    public FieldEntry(String value, Map<String, String> attributes) {
      this.value = value;
      this.attributes = ImmutableMap.copyOf(attributes);
    }

    public String value() {
      return value;
    }

    public Map<String, String> attributes() {
      return attributes;
    }
  }

  public Segment(Map<String, List<FieldEntry>> internalMap, Map<String, String> attributes) {
    this.attributes = ImmutableMap.copyOf(checkNotNull(attributes));
    ImmutableMap.Builder<String, List<FieldEntry>> mapBuilder = ImmutableMap.builder();
    for (final Map.Entry<String, List<FieldEntry>> entry : internalMap.entrySet()) {
      mapBuilder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
    }
    this.internalMap = mapBuilder.build();
  }
}


