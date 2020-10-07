/**
 * Constrained Serif
 *
 * Constrained Serif runs Serif over documents subject to some sort of constraints, similar to
 * CASerif for CSerif. Currently, there is no support for running trained models, so this
 * is mostly useful when your constraints fully define your expected output. This is useful
 * when you need to convert from e.g. LDC ERE annotation to SerifXML.
 *
 * We have broken the standard Serif processing pipeline (as implemented in CSerif) into two main
 * parts, a structural one which performs Region Finding, Sentence Segmenting, and Tokenization, and
 * a set of annotation stages which build on each other, such as name finding, parsing, coref, etc.
 * Both parts require implementations of corresponding interfaces, where the structural components
 * vary a little more widely in their semantics than the annotation components. Each of the
 * annotation components is described as an XFinder, where X is annotations to add.
 *
 * The interfaces provide no specification for whether or not a stage also runs any additional
 * algorithms besides annotation conversion, e.g. one might want to run an actual tokenizer subject
 * to knowing that certain boundaries are defined.
 *
 * TODO update this list Rule based implementations of varying sophistication (none to some) are
 * provided for:
 * <pre>
 *   * RegionFinding - see {@code SingletonRegionFinder}
 *   * SentenceSegmenting - see {@code ExtremelySimpleSentenceSegmenter}
 *   * Tokenization - see {@code SimpleTokenizer}
 *   * NameFinding - see {@code NameFinderFromExactConstraints}
 *   * ValueFinding - see {@code ValueMentionFinderFromExactConstraints}
 *   * Parsing - see {@code DummyParserAdder}
 *   * Mentions - see {@code MentionsFinderFromExactConstraints}
 *   * Entities (coref) - see {@code EntityFinderFromExternalIDs}
 *   * Events - see {@code EventMentionFinderFromExternalIDs} and {@code NoOpEventFinder}
 *   * Relations - see {@code RelationMentionFinderFromExactConstraints} and {@code
 * NoOpRelationFinder}
 * </pre>
 *
 * This is a partial implementation of the functionality of CASerif. It allows arbitrary Constraint
 * sources (up to you to implement) which are injected by Guice at runtime. This was implemented to
 * deliver a QC tool to the LDC.
 *
 * All of this code is highly experimental and subject to change at any time. Do not use it
 * without consulting rgabbard or jdeyoung.
 */
package com.bbn.serif.constraints;

