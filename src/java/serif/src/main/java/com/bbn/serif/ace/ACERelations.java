package com.bbn.serif.ace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ACERelations {

  private ACERelations() {
    throw new UnsupportedOperationException();
  }

  public static final ACERelationType ART_UserOwnerInventorManufacturer
      = new ACERelationType("ART", "User-Owner-Inventor-Manufacturer",
      ImmutableList.of("PER", "ORG", "GPE"), ImmutableList.of("FAC"));

  public static final ACERelationType GEN_AFF_CitizenResidentReligionEthnicity
      =
      new ACERelationType("GEN-AFF", "Citizen-Resident-Religion-Ethnicity", ImmutableList.of("PER"),
          ImmutableList.of("PER", "LOC", "GPE", "ORG"));
  public static final ACERelationType GEN_AFF_OrgLocation
      = new ACERelationType("GEN-AFF", "Org-Location", ImmutableList.of("ORG"),
      ImmutableList.of("LOC", "GPE"));

  public static final ACERelationType ORG_AFF_Employment
      = new ACERelationType("ORG-AFF", "Employment", ImmutableList.of("PER"),
      ImmutableList.of("ORG", "GPE"));
  public static final ACERelationType ORG_AFF_Founder
      = new ACERelationType("ORG-AFF", "Founder", ImmutableList.of("PER", "ORG"),
      ImmutableList.of("ORG", "GPE"));
  public static final ACERelationType ORG_AFF_InvestorShareholder
      =
      new ACERelationType("ORG-AFF", "Investor-Shareholder", ImmutableList.of("PER", "ORG", "GPE"),
          ImmutableList.of("ORG", "GPE"));
  public static final ACERelationType ORG_AFF_Membership
      = new ACERelationType("ORG-AFF", "Membership", ImmutableList.of("PER", "ORG", "GPE"),
      ImmutableList.of("ORG"));
  public static final ACERelationType ORG_AFF_Ownership
      =
      new ACERelationType("ORG-AFF", "Ownership", ImmutableList.of("PER"), ImmutableList.of("ORG"));
  public static final ACERelationType ORG_AFF_SportsAffiliation
      = new ACERelationType("ORG-AFF", "Sports-Affiliation", ImmutableList.of("PER"),
      ImmutableList.of("ORG"));
  public static final ACERelationType ORG_AFF_StudentAlum
      = new ACERelationType("ORG-AFF", "Student-Alum", ImmutableList.of("PER"),
      ImmutableList.of("ORG"));

  public static final ACERelationType PART_WHOLE_Artifact
      = new ACERelationType("PART-WHOLE", "Artifact", ImmutableList.of("FAC", "VEH", "WEA"),
      ImmutableList.of("VEH", "WEA"));
  public static final ACERelationType PART_WHOLE_Geographical
      = new ACERelationType("PART-WHOLE", "Geographical", ImmutableList.of("FAC", "LOC", "GPE"),
      ImmutableList.of("FAC", "LOC", "GPE"));
  public static final ACERelationType PART_WHOLE_Subsidiary
      = new ACERelationType("PART-WHOLE", "Subsidiary", ImmutableList.of("ORG"),
      ImmutableList.of("ORG", "GPE"));

  public static final ACERelationType PER_SOC_Business
      =
      new ACERelationType("PER-SOC", "Business", ImmutableList.of("PER"), ImmutableList.of("PER"));
  public static final ACERelationType PER_SOC_Family
      = new ACERelationType("PER-SOC", "Family", ImmutableList.of("PER"), ImmutableList.of("PER"));
  public static final ACERelationType PER_SOC_LastingPersonal
      = new ACERelationType("PER-SOC", "Lasting-Personal", ImmutableList.of("PER"),
      ImmutableList.of("PER"));

  public static final ACERelationType PHYS_Located
      = new ACERelationType("PHYS", "Located", ImmutableList.of("PER"),
      ImmutableList.of("FAC", "LOC", "GPE"));
  public static final ACERelationType PHYS_Near
      = new ACERelationType("PHYS", "Near", ImmutableList.of("PER", "FAC", "GPE", "LOC"),
      ImmutableList.of("FAC", "LOC", "GPE"));

  public static final List<ACERelationType> ACERelationTypes =
      ImmutableList.<ACERelationType>builder()
          .add(ART_UserOwnerInventorManufacturer)
          .add(GEN_AFF_CitizenResidentReligionEthnicity).add(GEN_AFF_OrgLocation)
          .add(ORG_AFF_Employment).add(ORG_AFF_Founder).add(ORG_AFF_InvestorShareholder)
          .add(ORG_AFF_Membership).add(ORG_AFF_Ownership).add(ORG_AFF_SportsAffiliation)
          .add(ORG_AFF_StudentAlum)
          .add(PART_WHOLE_Artifact).add(PART_WHOLE_Geographical).add(PART_WHOLE_Subsidiary)
          .add(PER_SOC_Business).add(PER_SOC_Family).add(PER_SOC_LastingPersonal)
          .add(PHYS_Located).add(PHYS_Near)
          .build();

  private static final Map<String, ACERelationType> ACERelationTypesByName =
      Maps.uniqueIndex(ACERelationTypes, ACERelationType.Name);

  public static ACERelationType byName(String name) {
    final ACERelationType ret = ACERelationTypesByName.get(checkNotNull(name));
    if (ret != null) {
      return ret;
    } else {
      throw new RuntimeException(String.format("%s is not a known ACE relation type", name));
    }
  }
}
