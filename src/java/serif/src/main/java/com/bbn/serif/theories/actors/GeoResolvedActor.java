package com.bbn.serif.theories.actors;

import com.bbn.bue.common.symbols.Symbol;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class GeoResolvedActor {

  private final Symbol geoText;
  private final Symbol geoCountry;
  private final Long geoID; // Optional because countries don't have a geoID
  private final Double geoLatitude;
  private final Double geoLongitude;
  private final CountryInfo countryInfo;
  private static final double minLatitude = -90.0;
  private static final double maxLatitude = 90.0;
  private static final double minLongitude = -180.0;
  private static final double maxLongitude = 180.0;


  public Optional<Symbol> geoText() {
	  return Optional.fromNullable(geoText);
  }

  public Optional<Symbol> geoCountry() {
	  return Optional.fromNullable(geoCountry);
  }

  public Optional<Long> geoID() {
	  return Optional.fromNullable(geoID);
  }

  public Optional<Double> geoLatitude() {
	  return Optional.fromNullable(geoLatitude);
  }

  public Optional<Double> geoLongitude() {
	  return Optional.fromNullable(geoLongitude);
  }

  public Optional<CountryInfo> countryInfo(){
	  return Optional.fromNullable(countryInfo);
  }

  private GeoResolvedActor(Symbol geoText, Symbol geoCountry, Optional<Long> geoID,
      Optional<Double> geoLatitude, Optional<Double> geoLongitude, Optional<CountryInfo> countryInfo) {
    this.geoText = geoText;
    this.geoCountry = geoCountry;
    this.geoID = geoID.orNull();
    if (geoLatitude.isPresent()) {
      checkArgument(geoLatitude.get() <= maxLatitude && geoLatitude.get() >= minLatitude);
    }
    this.geoLatitude = geoLatitude.orNull();
    if (geoLongitude.isPresent()) {
      checkArgument(geoLongitude.get() <= maxLongitude && geoLongitude.get() >= minLongitude);
    }
    this.geoLongitude = geoLongitude.orNull();
    this.countryInfo = countryInfo.orNull();
  }

  public static GeoResolvedActor create(Symbol geoText, Symbol geoCountry, Optional<Long> geoID,
      Optional<Double> geoLatitude, Optional<Double> geoLongitude, Optional<CountryInfo> countryInfo) {
    return new GeoResolvedActor(geoText, geoCountry, geoID, geoLatitude, geoLongitude, countryInfo);
  }

  public static final class CountryInfo{
	  private final long countryID;
	  private final Symbol isoCode;
	  private final CountryInfoActor countryInfoActor;
	  private final Symbol countryInfoActorCode;

	  public long countryID(){
		  return countryID;
	  }

	  public Symbol isoCode(){
		  return isoCode;
	  }

	  public CountryInfoActor countryInfoActor(){
		  return countryInfoActor;
	  }

	  public Optional<Symbol> countryInfoActorCode(){
		  return Optional.fromNullable(countryInfoActorCode);
	  }

	  private CountryInfo(long countryID,Symbol isoCode, CountryInfoActor countryInfoActor,
			  Optional<Symbol> countryInfoActorCode){
		  this.countryID=countryID;
		  this.isoCode=checkNotNull(isoCode);
		  this.countryInfoActor=checkNotNull(countryInfoActor);
		  this.countryInfoActorCode=countryInfoActorCode.orNull();
	  }

	  public static CountryInfo create(long countryID,Symbol isoCode, CountryInfoActor countryInfoActor,
			  Optional<Symbol> countryInfoActorCode){
		  return new CountryInfo(countryID,isoCode,countryInfoActor,countryInfoActorCode);
	  }

	  @Override
	  public String toString(){
		   final StringBuilder sb = new StringBuilder();
		    sb.append("[");
		    sb.append("countryID=").append(String.valueOf(countryID)).append(";");
		    sb.append("isoCode=").append(isoCode.toString()).append(";");
		    sb.append("countryInfoActor=").append(countryInfoActor.toString()).append(";");
		    if (countryInfoActorCode!=null) {
		      sb.append("countryInfoActorCode=").append(countryInfoActorCode.toString()).append(";");
		    }
		    sb.append("]");
		    return sb.toString();
	  }

	  @Override
	  public boolean equals(Object o){
		    if (this == o) {
		        return true;
		      }
		      if (o == null || getClass() != o.getClass()) {
		        return false;
		      }
		      GeoResolvedActor.CountryInfo that = (GeoResolvedActor.CountryInfo) o;

		      return Objects.equal(this.countryID, that.countryID) && Objects.equal(this.isoCode, 
		    		  that.isoCode) && Objects.equal(this.countryInfoActor, that.countryInfoActor) &&
		    		  Objects.equal(this.countryInfoActorCode,that.countryInfoActorCode);
	  }

	  @Override
	  public int hashCode() {
		 return Objects.hashCode(this.countryID,this.isoCode,this.countryInfoActor,this.countryInfoActorCode); 
	  }

	  public static final class CountryInfoActor{
		  private final long actorID;
		  private final Symbol databaseKey;

		  public long actorID(){
			  return actorID;
		  }

		  public Optional<Symbol> databaseKey(){
			  return Optional.fromNullable(databaseKey);
		  }

		  private CountryInfoActor(long actorID, Optional<Symbol> databaseKey){
			  this.actorID=actorID;
			  this.databaseKey=databaseKey.orNull();
		  }

		  public static CountryInfoActor create(long actorID, Optional<Symbol> databaseKey){
			  return new CountryInfoActor(actorID,databaseKey);
		  }

		  @Override
		  public String toString(){
			   final StringBuilder sb = new StringBuilder();
			    sb.append("[");
			    sb.append("actorID=").append(String.valueOf(actorID)).append(";");
			    if (databaseKey!=null) {
			      sb.append("databaseKey=").append(databaseKey.toString()).append(";");
			    }
			    sb.append("]");
			    return sb.toString();
		  }

		  @Override
		  public boolean equals(Object o){
			    if (this == o) {
			        return true;
			      }
			      if (o == null || getClass() != o.getClass()) {
			        return false;
			      }
			      GeoResolvedActor.CountryInfo.CountryInfoActor that =
			    		  (GeoResolvedActor.CountryInfo.CountryInfoActor) o;

			      return Objects.equal(this.actorID,that.actorID) && Objects.equal(this.databaseKey, 
			    		  that.databaseKey);
		  }
		  
		  @Override
		  public int hashCode(){
			  return Objects.hashCode(actorID,databaseKey);
		  }
	  }
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("[");
    sb.append("geoText=").append(geoText == null ? "" : geoText.toString()).append(";");
    sb.append("geoCountry=").append(geoCountry == null ? "" : geoCountry.toString()).append(";");
    sb.append("geoID=").append(String.valueOf(geoID)).append(";");
    if (geoLatitude!=null) {
      sb.append("geoLatitude=").append(String.valueOf(geoLatitude)).append(";");
    }
    if (geoLongitude!=null) {
      sb.append("geoLongitude=").append(String.valueOf(geoLongitude)).append(";");
    }
    if (countryInfo!=null) {
    	sb.append("countryInfo=").append(countryInfo.toString()).append(";");
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GeoResolvedActor that = (GeoResolvedActor) o;
    return Objects.equal(this.geoID, that.geoID) && Objects.equal(this.geoCountry, that.geoCountry) &&
    		Objects.equal(this.geoText, that.geoText) && Objects.equal(this.geoLatitude, that.geoLatitude) &&
    		Objects.equal(this.geoLongitude, that.geoLongitude) && Objects.equal(this.countryInfo, 
    				that.countryInfo);
  }
  
  @Override
  public int hashCode(){
	  return Objects.hashCode(geoText, geoCountry, geoID, geoLatitude, geoLongitude,countryInfo);
  }
}
