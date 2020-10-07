package com.bbn.serif.events.utilities;

import com.bbn.bue.common.symbols.Symbol;

public interface Stemmer {

  Symbol stem(Symbol word, Symbol POS);
}


