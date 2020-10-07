package com.bbn.serif.theories;


import com.google.common.collect.ImmutableList;

import java.util.Iterator;

public final class DocumentEvents implements Iterable<DocumentEvent>, PotentiallyAbsentSerifTheory {
  private final boolean absent;
  private final ImmutableList<DocumentEvent> documentEvents;

  private DocumentEvents(final boolean absent,
      final Iterable<DocumentEvent> documentEvents) {
    this.absent = absent;
    this.documentEvents = ImmutableList.copyOf(documentEvents);
  }

  public static DocumentEvents of(Iterable<DocumentEvent> documentEvents) {
    return new DocumentEvents(false, documentEvents);
  }

  public static DocumentEvents absent() {
    return new DocumentEvents(true, ImmutableList.<DocumentEvent>of());
  }

  public ImmutableList<DocumentEvent> asList() {
    return documentEvents;
  }

  @Override
  public Iterator<DocumentEvent> iterator() {
    return documentEvents.iterator();
  }

  @Override
  public boolean isAbsent() {
    return absent;
  }
}

