package com.bbn.serif.theories;

public class DocumentActorInfo {

  private final long defaultCountryActorId;

  private DocumentActorInfo(long defaultCountryActorId) {
    this.defaultCountryActorId = defaultCountryActorId;
  }

  public long defaultCountryActorId() {
    return defaultCountryActorId;
  }

  public static DocumentActorInfo create(long defaultCountryActorId) {
    return new DocumentActorInfo(defaultCountryActorId);
  }

}
