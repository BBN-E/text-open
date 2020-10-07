package com.bbn.serif.theories;

import java.util.Set;

/**
 * Any object which has a set of possible morphological token analyses associated with it.
 */
public interface HasMorphologicalAnalyses {

  Set<MorphTokenAnalysis> morphologicalAnalyses();
}
