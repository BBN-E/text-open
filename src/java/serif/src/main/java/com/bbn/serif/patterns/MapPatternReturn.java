package com.bbn.serif.patterns;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MapPatternReturn implements PatternReturn {

  private final Map<String, String> returnMap;

  public String get(String key) {
    return returnMap.get(key);
  }

  public boolean hasValue(String key) {
    return returnMap.containsKey(key);
  }

  public Set<String> keySet() {
    return returnMap.keySet();
  }

  private MapPatternReturn(Map<String, String> returnMap) {
    this.returnMap = ImmutableMap.copyOf(returnMap);
  }

  public static class Builder {

    private final Map<String, String> returnMap;

    public Builder() {
      returnMap = new HashMap<String, String>();
    }

    public MapPatternReturn build() {
      return new MapPatternReturn(this.returnMap);
    }

    public Builder withReturnAdd(String key, String value) {
      returnMap.put(key, value);
      return this;
    }

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((returnMap == null) ? 0 : returnMap.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MapPatternReturn other = (MapPatternReturn) obj;
    if (returnMap == null) {
      if (other.returnMap != null) {
        return false;
      }
    } else if (!returnMap.equals(other.returnMap)) {
      return false;
    }
    return true;
  }
}
