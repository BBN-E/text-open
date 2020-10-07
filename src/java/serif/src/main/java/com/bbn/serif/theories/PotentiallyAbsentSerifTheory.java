package com.bbn.serif.theories;

/**
 * @author dkolas
 */
public interface PotentiallyAbsentSerifTheory {

  /**
   * Indicates whether or not this theory was not present in the processed SerifXML.
   *
   * @return true if theory was not present
   */
  public boolean isAbsent();
}
