package com.bbn.serif.coreference.representativementions;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.io.SerifIOUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Entity.RepresentativeMention;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class TestRepresentativeMentions {

  private static final Logger log = LoggerFactory.getLogger(TestRepresentativeMentions.class);

  private TestRepresentativeMentions() {
    throw new UnsupportedOperationException();
  }

  public static void main(final String[] argv) throws IOException {
    final SerifXMLLoader loader = SerifXMLLoader.builder().allowSloppyOffsets().build();
    final File inputRoot = new File(argv[0]);
    final Iterable<File> xmlFiles = Iterables.filter(
        Files.fileTreeTraverser().breadthFirstTraversal(inputRoot),
        FileUtils.EndsWith(".xml"));

    final Iterable<DocTheory> docs = SerifIOUtils.docTheoriesFromFiles(
        xmlFiles, loader);

    int idx = 0;
    for (final DocTheory doc : docs) {
      for (final Entity e : doc.entities()) {
        final Optional<RepresentativeMention> repName = e.representativeName();
        final RepresentativeMention repMention = e.representativeMention();

        log.info("{}: {}/{} <---- {}", idx, repMention.span().tokenizedText(),
            repName.isPresent() ? repName.get().span() : "NO_NAME",
            e);
        ++idx;
      }
    }
  }
}
