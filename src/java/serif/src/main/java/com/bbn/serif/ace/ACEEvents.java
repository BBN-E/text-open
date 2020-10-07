package com.bbn.serif.ace;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.bbn.serif.ace.ACEEventRole.entityOnly;
import static com.bbn.serif.ace.ACEEventRole.valueOnly;
import static com.google.common.base.Preconditions.checkNotNull;


public final class ACEEvents {

  private ACEEvents() {
    throw new UnsupportedOperationException();
  }

  public static final ACEEventRole PERSON = entityOnly("Person", ImmutableList.of("PER"));
  public static final ACEEventRole PLACE =
      entityOnly("Place", ImmutableList.of("GPE", "LOC", "FAC"));
  public static final ACEEventRole TIME = valueOnly("Time", ImmutableList.of("TIME"));
  public static final ACEEventRole AGENT =
      entityOnly("Agent", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole VICTIM = entityOnly("Victim", ImmutableList.of("PER"));
  public static final ACEEventRole INSTRUMENT =
      entityOnly("Instrument", ImmutableList.of("WEA", "VEH"));
  public static final ACEEventRole VEHICLE = entityOnly("Vehicle", ImmutableList.of("VEH"));
  public static final ACEEventRole PRICE = valueOnly("Price", ImmutableList.of("NUM"));
  public static final ACEEventRole ORIGIN =
      entityOnly("Origin", ImmutableList.of("GPE", "LOC", "FAC"));
  public static final ACEEventRole DESTINATION =
      entityOnly("Destination", ImmutableList.of("GPE", "LOC", "FAC"));
  public static final ACEEventRole MOVE_ARTIFACT =
      entityOnly("Artifact", ImmutableList.of("PER", "WEA", "VEH"));
  public static final ACEEventRole BUYER =
      entityOnly("Buyer", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole SELLER =
      entityOnly("Seller", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole BENEFICIARY =
      entityOnly("Beneficiary", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole SALE_ARTIFACT =
      entityOnly("Artifact", ImmutableList.of("ORG", "FAC", "WEA", "VEH"));
  public static final ACEEventRole MONEY = valueOnly("Money", ImmutableList.of("MONEY"));
  public static final ACEEventRole GIVER =
      entityOnly("Giver", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole RECIPIENT =
      entityOnly("Recipient", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole ORG_ROLE = entityOnly("Org", ImmutableList.of("ORG"));
  public static final ACEEventRole ORG_BANKRUPT =
      entityOnly("Org", ImmutableList.of("ORG", "GPE", "PER"));
  public static final ACEEventRole ATTACKER =
      entityOnly("Attacker", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole TARGET =
      entityOnly("Target", ImmutableList.of("PER", "ORG", "VEH", "FAC", "WEA"));
  public static final ACEEventRole ENTITY_NO_GPE =
      entityOnly("Entity", ImmutableList.of("PER", "ORG"));
  public static final ACEEventRole ENTITY =
      entityOnly("Entity", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole POSITION = valueOnly("Position", ImmutableList.of("JOB"));
  public static final ACEEventRole AGENT_NOMINATE =
      entityOnly("Agent", ImmutableList.of("PER", "ORG", "GPE", "FAC"));
  public static final ACEEventRole CRIME = valueOnly("Crime", ImmutableList.of("CRIME"));
  public static final ACEEventRole SENTENCE = valueOnly("Sentence", ImmutableList.of("SENTENCE"));
  public static final ACEEventRole DEFENDANT =
      entityOnly("Defendant", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole PROSECUTOR =
      entityOnly("Prosecutor", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole ADJUDICATOR =
      entityOnly("Adjudicator", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole PLAINTIFF =
      entityOnly("Plaintiff", ImmutableList.of("PER", "ORG", "GPE"));
  public static final ACEEventRole TIME_STARTING =
      valueOnly("Time-Starting", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_ENDING = valueOnly("Time-Ending", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_WITHIN = valueOnly("Time-Within", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_HOLDS = valueOnly("Time-Holds", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_AFTER = valueOnly("Time-After", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_BEFORE = valueOnly("Time-Before", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_AT_BEGINNING =
      valueOnly("Time-At-Beginning", ImmutableList.of("TIME"));
  public static final ACEEventRole TIME_AT_END = valueOnly("Time-At-End", ImmutableList.of("TIME"));

  public static final List<ACEEventRole> ACEEventRoles = ImmutableList.<ACEEventRole>builder()
      .add(TIME).add(AGENT).add(VICTIM)
      .add(INSTRUMENT).add(VEHICLE)
      .add(PRICE).add(ORIGIN)
      .add(DESTINATION).add(MOVE_ARTIFACT).add(BUYER)
      .add(SELLER).add(BENEFICIARY).add(SALE_ARTIFACT)
      .add(MONEY).add(GIVER).add(RECIPIENT)
      .add(ORG_ROLE)
      .add(ORG_BANKRUPT).add(ATTACKER).add(TARGET)
      .add(ENTITY_NO_GPE).add(ENTITY).add(POSITION)
      .add(AGENT_NOMINATE).add(CRIME).add(SENTENCE).add(DEFENDANT)
      .add(PROSECUTOR).add(ADJUDICATOR)
      .add(PLAINTIFF).add(TIME_STARTING).add(TIME_ENDING).add(TIME_WITHIN).add(TIME_HOLDS)
      .add(TIME_AFTER).add(TIME_BEFORE)
      .add(TIME_AT_BEGINNING).add(TIME_AT_END)
      .add(PERSON).add(PLACE)
      .build();

  private static final ImmutableSet<ACEEventRole> ACE_EVENT_ROLES_SIMPLE_TIME =
      ImmutableSet.<ACEEventRole>builder()
      .add(TIME).add(AGENT).add(VICTIM)
      .add(INSTRUMENT).add(VEHICLE)
      .add(PRICE).add(ORIGIN)
      .add(DESTINATION).add(MOVE_ARTIFACT).add(BUYER)
      .add(SELLER).add(BENEFICIARY).add(SALE_ARTIFACT)
      .add(MONEY).add(GIVER).add(RECIPIENT)
      .add(ORG_ROLE)
      .add(ORG_BANKRUPT).add(ATTACKER).add(TARGET)
      .add(ENTITY_NO_GPE).add(ENTITY).add(POSITION)
      .add(AGENT_NOMINATE).add(CRIME).add(SENTENCE).add(DEFENDANT)
      .add(PROSECUTOR).add(ADJUDICATOR)
      .add(PLAINTIFF)
      .add(PERSON).add(PLACE)
      .build();

  private static final ImmutableSet<Symbol> ACE_EVENT_ROLES_SIMPLE_NAMES =
      FluentIterable.from(ACE_EVENT_ROLES_SIMPLE_TIME)
      .transform(ACEEventRole.nameFunction()).toSet();

  public static final ACEEventType LifeBeBorn = new ACEEventType("Life", "Be-Born",
      ImmutableList.of(PERSON, PLACE, TIME));
  public static final ACEEventType LifeMarry = new ACEEventType("Life", "Marry",
      ImmutableList.of(PERSON, PLACE, TIME));
  public static final ACEEventType LifeDivorce = new ACEEventType("Life", "Divorce",
      ImmutableList.of(PERSON, PLACE, TIME));
  public static final ACEEventType LifeInjure = new ACEEventType("Life", "Injure",
      ImmutableList.of(AGENT, VICTIM, INSTRUMENT, PLACE, TIME));
  public static final ACEEventType LifeDie = new ACEEventType("Life", "Die",
      ImmutableList.of(AGENT, VICTIM, INSTRUMENT, PLACE, TIME));

  public static final ACEEventType MovementTransport =
      new ACEEventType("Movement", "Transport", ImmutableList.of(AGENT,
          MOVE_ARTIFACT, VEHICLE, PRICE, ORIGIN, DESTINATION, TIME));

  public static final ACEEventType TransactionTransferOwnership =
      new ACEEventType("Transaction", "Transfer-Ownership", ImmutableList.of(
          BUYER, SELLER, BENEFICIARY, SALE_ARTIFACT, PRICE, TIME, PLACE));
  public static final ACEEventType TransactionTransferMoney =
      new ACEEventType("Transaction", "Transfer-Money", ImmutableList.of(
          GIVER, RECIPIENT, BENEFICIARY, MONEY, TIME, PLACE));

  public static final ACEEventType BusinessStartOrg =
      new ACEEventType("Business", "Start-Org", ImmutableList.of(
          AGENT, ORG_ROLE, TIME, PLACE));
  public static final ACEEventType BusinessMergeOrg =
      new ACEEventType("Business", "Merge-Org", ImmutableList.of(
          ORG_ROLE, TIME, PLACE));
  public static final ACEEventType BusinessEndOrg =
      new ACEEventType("Business", "End-Org", ImmutableList.of(
          ORG_ROLE, TIME, PLACE));
  public static final ACEEventType BusinessDeclareBankruptcy =
      new ACEEventType("Business", "Declare-Bankruptcy", ImmutableList.of(
          ORG_BANKRUPT, TIME, PLACE));

  public static final ACEEventType ConflictAttack = new ACEEventType("Conflict",
      "Attack", ImmutableList.of(ATTACKER, TARGET, INSTRUMENT, PLACE, TIME));
  public static final ACEEventType ConflictDemonstrate = new ACEEventType("Conflict",
      "Demonstrate", ImmutableList.of(ENTITY_NO_GPE, PLACE, TIME));

  public static final ACEEventType ContactMeet = new ACEEventType("Contact", "Meet",
      ImmutableList.of(ENTITY, TIME, PLACE));
  public static final ACEEventType ContactPhoneWrite = new ACEEventType("Contact", "Phone-Write",
      ImmutableList.of(ENTITY, TIME));

  public static final ACEEventType PersonnelStartPosition = new ACEEventType("Personnel",
      "Start-Position", ImmutableList.of(PERSON, ENTITY_NO_GPE, POSITION,
      TIME, PLACE));
  public static final ACEEventType PersonnelEndPosition = new ACEEventType("Personnel",
      "End-Position", ImmutableList.of(PERSON, ENTITY_NO_GPE, POSITION,
      TIME, PLACE));
  public static final ACEEventType PersonnelNominate = new ACEEventType("Personnel",
      "Nominate", ImmutableList.of(PERSON, AGENT_NOMINATE, POSITION,
      TIME, PLACE));
  public static final ACEEventType PersonnelElect = new ACEEventType("Personnel",
      "Elect", ImmutableList.of(PERSON, ENTITY, POSITION,
      TIME, PLACE));

  public static final ACEEventType JusticeArrestJail = new ACEEventType("Justice",
      "Arrest-Jail", ImmutableList.of(PERSON, AGENT, CRIME,
      TIME, PLACE));
  public static final ACEEventType JusticeReleaseParole = new ACEEventType("Justice",
      "Release-Parole", ImmutableList.of(PERSON, ENTITY, CRIME,
      TIME, PLACE));
  public static final ACEEventType JusticeTrialHearing = new ACEEventType("Justice",
      "Trial-Hearing", ImmutableList.of(DEFENDANT, PROSECUTOR, ADJUDICATOR,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeChargeIndict = new ACEEventType("Justice",
      "Charge-Indict", ImmutableList.of(DEFENDANT, PROSECUTOR, ADJUDICATOR,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeSue = new ACEEventType("Justice",
      "Sue", ImmutableList.of(DEFENDANT, PROSECUTOR, ADJUDICATOR,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeConvict = new ACEEventType("Justice",
      "Convict", ImmutableList.of(DEFENDANT, ADJUDICATOR,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeSentence = new ACEEventType("Justice",
      "Sentence", ImmutableList.of(DEFENDANT, ADJUDICATOR,
      CRIME, SENTENCE, TIME, PLACE));
  public static final ACEEventType JusticeFine = new ACEEventType("Justice",
      "Fine", ImmutableList.of(ENTITY, ADJUDICATOR, MONEY,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeExecute = new ACEEventType("Justice",
      "Execute", ImmutableList.of(PERSON, AGENT,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeExtradite = new ACEEventType("Justice",
      "Extradite", ImmutableList.of(PERSON, AGENT, DESTINATION, ORIGIN,
      CRIME, TIME));
  public static final ACEEventType JusticeAcquit = new ACEEventType("Justice",
      "Acquit", ImmutableList.of(DEFENDANT, ADJUDICATOR,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticePardon = new ACEEventType("Justice",
      "Pardon", ImmutableList.of(DEFENDANT, ADJUDICATOR,
      CRIME, TIME, PLACE));
  public static final ACEEventType JusticeAppeal = new ACEEventType("Justice",
      "Appeal", ImmutableList.of(DEFENDANT, PROSECUTOR, ADJUDICATOR,
      CRIME, TIME, PLACE));

  public static final List<ACEEventType> ACEEvents = ImmutableList.<ACEEventType>builder()
      .add(LifeBeBorn).add(LifeMarry).add(LifeDivorce).add(LifeInjure).add(LifeDie)
      .add(MovementTransport).add(TransactionTransferOwnership)
      .add(TransactionTransferMoney).add(BusinessStartOrg)
      .add(BusinessMergeOrg).add(BusinessEndOrg).add(BusinessDeclareBankruptcy)
      .add(ConflictAttack).add(ConflictDemonstrate).add(ContactMeet)
      .add(ContactPhoneWrite).add(PersonnelStartPosition).add(PersonnelEndPosition)
      .add(PersonnelNominate)
      .add(PersonnelElect).add(JusticeArrestJail).add(JusticeReleaseParole)
      .add(JusticeTrialHearing).add(JusticeChargeIndict).add(JusticeSue)
      .add(JusticeConvict).add(JusticeSentence).add(JusticeFine).add(JusticeExecute)
      .add(JusticeExtradite).add(JusticeAcquit).add(JusticePardon).add(JusticeAppeal)
      .build();

  private static final ImmutableSet<Symbol> NON_VIOLENT_LIFE_EVENTS = SymbolUtils.setFrom(
      LifeBeBorn.name(), LifeMarry.name(), LifeDivorce.name());

  public static boolean isNonViolentLifeEvent(Symbol eventType) {
    return NON_VIOLENT_LIFE_EVENTS.contains(eventType);
  }

  private static final ImmutableSet<Symbol> VIOLENT_LIFE_EVENTS = SymbolUtils.setFrom(
      LifeInjure.name(), LifeDie.name());

  public static boolean isViolentLifeEvent(Symbol eventType) {
    return VIOLENT_LIFE_EVENTS.contains(eventType);
  }

  private static final ImmutableSet<Symbol> BUSINESS_EVENTS = SymbolUtils.setFrom(
      BusinessStartOrg.name(), BusinessEndOrg.name(), BusinessDeclareBankruptcy.name(),
      BusinessMergeOrg.name());

  public static boolean isBusinessEvent(Symbol eventType) {
    return BUSINESS_EVENTS.contains(eventType);
  }

  private static final ImmutableSet<Symbol> CONTACT_EVENTS = SymbolUtils.setFrom(
      ContactMeet.name(), ContactPhoneWrite.name());

  public static boolean isContactEvent(Symbol eventType) {
    return CONTACT_EVENTS.contains(eventType);
  }

  private static final ImmutableSet<Symbol> PERSONNEL_EVENTS = SymbolUtils.setFrom(
      PersonnelStartPosition.name(), PersonnelEndPosition.name(),
      PersonnelNominate.name(), PersonnelElect.name());

  public static boolean isPersonnelEvent(Symbol eventType) {
    return PERSONNEL_EVENTS.contains(eventType);
  }

  private static final ImmutableSet<Symbol> CUSTODY_EVENTS = SymbolUtils.setFrom(
      JusticeArrestJail.name(), JusticeReleaseParole.name(),
      JusticeExtradite.name(), JusticeExecute.name());

  public static boolean isCustodyEvent(Symbol eventType) {
    return CUSTODY_EVENTS.contains(eventType);
  }

  private static final ImmutableSet<Symbol> TRIAL_EVENTS = SymbolUtils.setFrom(
      JusticeTrialHearing.name(), JusticeChargeIndict.name(),
      JusticeConvict.name(), JusticeSentence.name(), JusticeAcquit.name(),
      JusticeAppeal.name(), JusticePardon.name());

  public static boolean isTrialEvent(Symbol eventType) {
    return TRIAL_EVENTS.contains(eventType);
  }

  private static final Map<String, ACEEventType> ACEEventsByName =
      Maps.uniqueIndex(ACEEvents, ACEEventType.Name);

  public static ACEEventType byName(String name) {
    final ACEEventType ret = ACEEventsByName.get(checkNotNull(name));
    if (ret != null) {
      return ret;
    } else {
      throw new NoSuchElementException(String.format("%s is not a known ACE event type", name));
    }
  }

  public static Set<Symbol> eventNames() {
    return SymbolUtils.setFrom(ACEEventsByName.keySet());
  }
  public static Set<Symbol> eventRoles() {
    return ACE_EVENT_ROLES_SIMPLE_NAMES;
  }

  private static final ImmutableSet<Symbol> TEMPORAL_ROLE_SYMBOLS = ImmutableSet.of(
      TIME.symbol(), TIME_AFTER.symbol(), TIME_AT_BEGINNING.symbol(), TIME_AT_END.symbol(),
      TIME_BEFORE.symbol(),
      TIME_ENDING.symbol(), TIME_HOLDS.symbol(), TIME_STARTING.symbol(), TIME_WITHIN.symbol());

  public static ImmutableSet<Symbol> temporalRoleSymbols() {
    return TEMPORAL_ROLE_SYMBOLS;
  }
}
