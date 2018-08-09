package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonDeserialize(builder = AMREntry.Builder.class)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AMREntry implements Comparable<AMREntry>{

    private final String antibiotic;
    private final String resistancePhenotype;
    private final String measurementSign;
    private final String measurementValue;
    private final String measurementUnits;
    private final String vendor;
    private final String laboratoryTypingMethod;
    private final String laboratoryTypingPlatform;
    private final String laboratoryTypingMethodVersionOrReagent;
    private final String testingStandard;
    private final String dstMedia;
    private final String dstMethod;
    private final String criticalConcentration;


    private AMREntry(String antibiotic, String resistancePhenotype, String measurementSign, String measurementValue, String measurementUnits, String vendor, String laboratoryTypingMethod, String laboratoryTypingPlatform, String laboratoryTypingMethodVersionOrReagent, String testingStandard, String dstMedia, String dstMethod, String criticalConcentration) {
        this.antibiotic = antibiotic;
        this.resistancePhenotype = resistancePhenotype;
        this.measurementSign = measurementSign;
        this.measurementValue = measurementValue;
        this.measurementUnits = measurementUnits;
        this.vendor = vendor;
        this.laboratoryTypingMethod = laboratoryTypingMethod;
        this.laboratoryTypingPlatform = laboratoryTypingPlatform;
        this.laboratoryTypingMethodVersionOrReagent = laboratoryTypingMethodVersionOrReagent;
        this.testingStandard = testingStandard;
        this.dstMedia = dstMedia;
        this.dstMethod = dstMethod;
        this.criticalConcentration = criticalConcentration;
    }


    public String getAntibiotic() {
        return antibiotic;
    }

    public String getResistancePhenotype() {
        return resistancePhenotype;
    }

    public String getMeasurementSign() {
        return measurementSign;
    }

    @JsonProperty("measurement")
    public String getMeasurementValue() {
        return measurementValue;
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

    public String getLaboratoryTypingPlatform() {
        return laboratoryTypingPlatform;
    }

    public String getLaboratoryTypingMethodVersionOrReagent() {
        return laboratoryTypingMethodVersionOrReagent;
    }

    public String getTestingStandard() {
        return testingStandard;
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

    @Override
    public int compareTo(AMREntry other) {
        if (other == null) {
            return 1;
        }



        int comparison = nullSafeStringComparison(this.antibiotic, other.antibiotic);
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

        comparison = nullSafeStringComparison(this.measurementValue, other.measurementValue);
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

        comparison = nullSafeStringComparison(this.laboratoryTypingPlatform, other.laboratoryTypingPlatform);
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

        }


        return nullSafeStringComparison(this.testingStandard, other.testingStandard);
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
        return getMeasurementValue().equals(amrEntry.getMeasurementValue()) &&
                Objects.equals(getAntibiotic(), amrEntry.getAntibiotic()) &&
                Objects.equals(getResistancePhenotype(), amrEntry.getResistancePhenotype()) &&
                Objects.equals(getMeasurementSign(), amrEntry.getMeasurementSign()) &&
                Objects.equals(getMeasurementUnits(), amrEntry.getMeasurementUnits()) &&
                Objects.equals(getVendor(), amrEntry.getVendor()) &&
                Objects.equals(getLaboratoryTypingMethod(), amrEntry.getLaboratoryTypingMethod()) &&
                Objects.equals(getLaboratoryTypingPlatform(), amrEntry.getLaboratoryTypingPlatform()) &&
                Objects.equals(getLaboratoryTypingMethodVersionOrReagent(), amrEntry.getLaboratoryTypingMethodVersionOrReagent()) &&
                Objects.equals(getTestingStandard(), amrEntry.getTestingStandard()) &&
                Objects.equals(getDstMedia(), amrEntry.getDstMedia()) &&
                Objects.equals(getDstMethod(), amrEntry.getDstMethod()) &&
                Objects.equals(getCriticalConcentration(), amrEntry.getCriticalConcentration());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getAntibiotic(), getResistancePhenotype(), getMeasurementSign(), getMeasurementValue(), getMeasurementUnits(), getVendor(), getLaboratoryTypingMethod(), getTestingStandard());
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class Builder {
        private String antibiotic;
        private String resistancePhenotype;
        private String measurementSign;
        private String measurementValue;
        private String measurementUnits;
        private String vendor;
        private String laboratoryTypingMethod;
        private String laboratoryTypingPlatform = "";
        private String laboratoryTypingMethodVersionOrReagent = "";
        private String testingStandard;
        private String dstMedia;
        private String dstMethod;
        private String criticalConcentration;

        @JsonCreator
        public Builder() { }

        @JsonProperty
        public Builder withAntibiotic(String antibiotic) {
            this.antibiotic = antibiotic;
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
            this.measurementValue = value;
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

        @JsonProperty("measurement")
        public Builder withMeasurementValue(String value) {
            this.measurementValue = value;
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
        public Builder withLaboratoryTypingPlatform(String laboratoryTypingPlatform) {
            this.laboratoryTypingPlatform = laboratoryTypingPlatform;
            return this;
        }

        @JsonProperty
        public Builder withLaboratoryTypingMethodVersionOrReagent(String laboratoryTypingMethodVersionOrReagent) {
            this.laboratoryTypingMethodVersionOrReagent = laboratoryTypingMethodVersionOrReagent;
            return this;
        }

        @JsonProperty
        public Builder withTestingStandard(String standard) {
            this.testingStandard = standard;
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

        public AMREntry build() {
//            if (this.antibiotic == null || this.antibiotic.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("antibiotic");
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
//            if (this.testingStandard == null || this.testingStandard.isEmpty()) {
//                throw AMREntryBuldingException.createForMissingField("testing standard");
//            }


            return new AMREntry(this.antibiotic, this.resistancePhenotype, this.measurementSign, this.measurementValue,
                    this.measurementUnits, this.vendor, this.laboratoryTypingMethod, this.laboratoryTypingPlatform,
                    this.laboratoryTypingMethodVersionOrReagent, this.testingStandard,
                    this.dstMedia, this.dstMethod, this.criticalConcentration);
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
