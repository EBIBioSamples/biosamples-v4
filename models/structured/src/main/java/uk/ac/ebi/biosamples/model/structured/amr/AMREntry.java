package uk.ac.ebi.biosamples.model.structured.amr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonDeserialize(builder = AMREntry.Builder.class)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonPropertyOrder({
        "species",
        "antibiotic_name",
        "ast_standard",
        "breakpoint_version",
        "laboratory_typing_method",
        "measurement",
        "measurement_units",
        "measurement_sign",
        "resistance_phenotype",
        "platform",
        "vendor",
        "laboratory_typing_method_version_or_reagent",
        "dst_media",
        "dst_method",
        "critical_concentration"
})
public class AMREntry implements Comparable<AMREntry>{
    private final AmrPair antibioticName;
    private final String resistancePhenotype;
    private final String measurementSign;
    private final String measurement;
    private final String measurementUnits;
    private final String vendor;
    private final String laboratoryTypingMethod;
    private final String platform;
    private final String laboratoryTypingMethodVersionOrReagent;
    private final String astStandard;
    private final String dstMedia;
    private final String dstMethod;
    private final String criticalConcentration;
    private final AmrPair species;
    private final String breakpointVersion;


    private AMREntry(AmrPair antibioticName, String resistancePhenotype, String measurementSign, String measurement, String measurementUnits, String vendor,
                     String laboratoryTypingMethod, String platform,
                     String laboratoryTypingMethodVersionOrReagent, String astStandard,
                     String dstMedia, String dstMethod, String criticalConcentration,
                     AmrPair species, String breakpointVersion) {
        this.antibioticName = antibioticName;
        this.resistancePhenotype = resistancePhenotype;
        this.measurementSign = measurementSign;
        this.measurement = measurement;
        this.measurementUnits = measurementUnits;
        this.vendor = vendor;
        this.laboratoryTypingMethod = laboratoryTypingMethod;
        this.platform = platform;
        this.laboratoryTypingMethodVersionOrReagent = laboratoryTypingMethodVersionOrReagent;
        this.astStandard = astStandard;
        this.dstMedia = dstMedia;
        this.dstMethod = dstMethod;
        this.criticalConcentration = criticalConcentration;
        this.species = (species != null) ? species : new AmrPair("");
        this.breakpointVersion = breakpointVersion;
    }


    //@JsonSerialize(using = AmrPairSerializer.class)
    //@JsonProperty("antibiotic_name")
    //use antiobiotic_name and not antibiotic
    public AmrPair getAntibioticName() {
        return antibioticName;
    }

    public String getResistancePhenotype() {
        return resistancePhenotype;
    }

    public String getMeasurementSign() {
        return measurementSign;
    }

    public String getMeasurement() {
        return measurement;
    }

    public String getMeasurementUnits() {
        return measurementUnits;
    }

    public String getVendor() {
        return vendor;
    }

    public String getLaboratoryTypingMethod() {
        return laboratoryTypingMethod;
    }

    public String getPlatform() {
        return platform;
    }

    public String getLaboratoryTypingMethodVersionOrReagent() {
        return laboratoryTypingMethodVersionOrReagent;
    }

    public String getAstStandard() {
        return astStandard;
    }

    public String getDstMedia() {
        return dstMedia;
    }

    public String getDstMethod() {
        return dstMethod;
    }

    public String getCriticalConcentration() {
        return criticalConcentration;
    }

    public AmrPair getSpecies() {
        return species;
    }

    public String getBreakpointVersion() {
        return breakpointVersion;
    }


    public int compareTo(AMREntry other) {
        if (other == null) {
            return 1;
        }

        int comparison = nullSafeStringComparison(this.antibioticName.getValue(), other.antibioticName.getValue());

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.resistancePhenotype, other.resistancePhenotype);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.measurementSign, other.measurementSign);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.measurement, other.measurement);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.measurementUnits, other.measurementUnits);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.laboratoryTypingMethod, other.laboratoryTypingMethod);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.platform, other.platform);

        if (comparison != 0) {
            return comparison;
        }
        comparison = nullSafeStringComparison(this.laboratoryTypingMethodVersionOrReagent, other.laboratoryTypingMethodVersionOrReagent);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.vendor, other.vendor);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.dstMedia, other.dstMedia);

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.species.getValue(), other.species.getValue());

        if (comparison != 0) {
            return comparison;
        }

        comparison = nullSafeStringComparison(this.breakpointVersion, other.breakpointVersion);

        if (comparison != 0) {
            return comparison;
        }

        return nullSafeStringComparison(this.astStandard, other.astStandard);
    }

    private int nullSafeStringComparison(String one, String two) {
        if (one == null && two != null) {
            return -1;
        }
        if (one != null && two == null) {
            return 1;
        }
        if (one != null && !one.equals(two)) {
            return one.compareTo(two);
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AMREntry)) return false;

        AMREntry amrEntry = (AMREntry) o;

        return Objects.equals(getMeasurement(), amrEntry.getMeasurement()) &&
                Objects.equals(getAntibioticName().getValue(), amrEntry.getAntibioticName().getValue()) &&
                Objects.equals(getResistancePhenotype(), amrEntry.getResistancePhenotype()) &&
                Objects.equals(getMeasurementSign(), amrEntry.getMeasurementSign()) &&
                Objects.equals(getMeasurementUnits(), amrEntry.getMeasurementUnits()) &&
                Objects.equals(getVendor(), amrEntry.getVendor()) &&
                Objects.equals(getLaboratoryTypingMethod(), amrEntry.getLaboratoryTypingMethod()) &&
                Objects.equals(getPlatform(), amrEntry.getPlatform()) &&
                Objects.equals(getLaboratoryTypingMethodVersionOrReagent(), amrEntry.getLaboratoryTypingMethodVersionOrReagent()) &&
                Objects.equals(getAstStandard(), amrEntry.getAstStandard()) &&
                Objects.equals(getDstMedia(), amrEntry.getDstMedia()) &&
                Objects.equals(getDstMethod(), amrEntry.getDstMethod()) &&
                Objects.equals(getCriticalConcentration(), amrEntry.getCriticalConcentration()) &&
                Objects.equals(getSpecies(), amrEntry.getSpecies()) &&
                Objects.equals(getBreakpointVersion(), amrEntry.getBreakpointVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAntibioticName().getValue(), getResistancePhenotype(), getMeasurementSign(), getMeasurement(), getMeasurementUnits(), getVendor(), getLaboratoryTypingMethod(), getAstStandard(), getDstMedia(), getDstMethod(), getCriticalConcentration(), getSpecies(), getBreakpointVersion());
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class Builder {
        private AmrPair antibioticName;
        private String resistancePhenotype;
        private String measurementSign;
        private String measurement;
        private String measurementUnits;
        private String vendor;
        private String laboratoryTypingMethod;
        private String platform = "";
        private String laboratoryTypingMethodVersionOrReagent = "";
        private String astStandard;
        private String dstMedia;
        private String dstMethod;
        private String criticalConcentration;
        private AmrPair species;
        private String breakpointVersion;

        @JsonCreator
        public Builder() { }

        @JsonProperty
        public Builder withAntibioticName(AmrPair antibioticName) {
            this.antibioticName = antibioticName;
            return this;
        }

        @JsonProperty
        public Builder withResistancePhenotype(String resistancePhenotype) {
            this.resistancePhenotype = resistancePhenotype;
            return this;
        }

        @JsonIgnore
        public Builder withMeasure(String sign, String value, String unit) {
            this.measurementSign = sign;
            this.measurement = value;
            this.measurementUnits = unit;
            return this;
        }

        @JsonProperty
        public Builder withMeasurementSign(String sign) {
            this.measurementSign = sign;
            return this;
        }

        @JsonProperty
        public Builder withMeasurementUnits(String unit) {
            this.measurementUnits = unit;
            return this;
        }

        public Builder withMeasurement(String value) {
            this.measurement = value;
            return this;
        }

        @JsonProperty
        public Builder withVendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        @JsonProperty
        public Builder withLaboratoryTypingMethod(String method) {
            this.laboratoryTypingMethod = method;
            return this;
        }

        @JsonProperty
        public Builder withPlatform(String laboratoryTypingPlatform) {
            this.platform = laboratoryTypingPlatform;
            return this;
        }

        @JsonProperty
        public Builder withLaboratoryTypingMethodVersionOrReagent(String laboratoryTypingMethodVersionOrReagent) {
            this.laboratoryTypingMethodVersionOrReagent = laboratoryTypingMethodVersionOrReagent;
            return this;
        }

        @JsonProperty
        public Builder withAstStandard(String standard) {
            this.astStandard = standard;
            return this;
        }

        @JsonProperty
        public Builder withDstMedia(String media) {
            this.dstMedia = media;
            return this;
        }

        @JsonProperty
        public Builder withDstMethod(String method) {
            this.dstMethod = method;
            return this;
        }

        @JsonProperty
        public Builder withCriticalConcentration(String concentration) {
            this.criticalConcentration = concentration;
            return this;
        }

        @JsonProperty
        public Builder withSpecies(AmrPair species) {
            this.species = species;
            return this;
        }

        @JsonProperty
        public Builder withBreakpointVersion(String breakpointVersion) {
            this.breakpointVersion = breakpointVersion;
            return this;
        }

        public AMREntry build() {
//            if (this.antibioticName == null || this.antibioticName.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("antibioticName");
//            }
//
//            if (this.resistancePhenotype == null || this.resistancePhenotype.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("resistance phenotype");
//            }
//
//            if (this.measurementValue == null) {
//                throw AMREntryBuldingException.createForMissingField("measurementValue sign");
//            }
//
//            if (this.measurementSign == null || this.measurementSign.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("measurementValue sign");
//            }
//            if (this.measurementUnits == null || this.measurementUnits.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("measurementValue unit");
//            }
//
//            if (this.vendor == null || this.vendor.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("vendor");
//            }
//            if (this.laboratoryTypingMethod == null || this.laboratoryTypingMethod.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("laboratory typing method");
//            }
//
//            if (this.astStandard == null || this.astStandard.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("testing standard");
//            }
            return new AMREntry(this.antibioticName, this.resistancePhenotype, this.measurementSign, this.measurement,
                    this.measurementUnits, this.vendor, this.laboratoryTypingMethod, this.platform,
                    this.laboratoryTypingMethodVersionOrReagent, this.astStandard,
                    this.dstMedia, this.dstMethod, this.criticalConcentration, this.species, this.breakpointVersion);
        }
    }

    public static class AMREntryBuldingException extends Exception {
        public AMREntryBuldingException(String message) {
            super(message);
        }

        public static RuntimeException createForMissingField(String field) {
            return new RuntimeException("You need to provide a non-empty  " + field);
        }
    }
}
