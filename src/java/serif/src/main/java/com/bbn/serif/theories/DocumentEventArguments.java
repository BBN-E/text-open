package com.bbn.serif.theories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Iterator;

/**
 * Represent document-level arguments of events.  Some systems, like a TAC KBP 2014 system, will
 * produce these on their own without linking them into document-level events.
 */
public final class DocumentEventArguments
    implements Iterable<DocumentEvent.Argument>, PotentiallyAbsentSerifTheory {

  /* Absent indicates document-level event finding was never run, as opposed to was run and found
  nothing. We could use a Optional<Set<DocumentEvent.Argument>> but it wouldn't fit the idiom
  otherwise employed in JSerif. Also, most of the time we don't care about distinguishing
  between the two, in which case it is cleaner just to iterate without making the distinction.*/
  private final boolean absent;
  private final ImmutableSet<DocumentEvent.Argument> documentEventArguments;

  private DocumentEventArguments(final boolean absent,
      final Iterable<DocumentEvent.Argument> documentEvents) {
    this.absent = absent;
    this.documentEventArguments = ImmutableSet.copyOf(documentEvents);
  }

  public static DocumentEventArguments of(Iterable<DocumentEvent.Argument> documentEvents) {
    return new DocumentEventArguments(false, documentEvents);
  }

  public static DocumentEventArguments absent() {
    return new DocumentEventArguments(true, ImmutableList.<DocumentEvent.Argument>of());
  }

  public ImmutableSet<DocumentEvent.Argument> asSet() {
    return documentEventArguments;
  }

  @Override
  public Iterator<DocumentEvent.Argument> iterator() {
    return documentEventArguments.iterator();
  }

  @Override
  public boolean isAbsent() {
    return absent;
  }
}
