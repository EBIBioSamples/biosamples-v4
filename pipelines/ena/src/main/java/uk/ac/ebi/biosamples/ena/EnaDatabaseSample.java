package uk.ac.ebi.biosamples.ena;

public class EnaDatabaseSample {
    public String lastUpdated;
    public String firstPublic;
    public String brokerName;
    public String bioSamplesId;
    public String centreName;
    public String fixed;
    public String taxId;
    public String scientificName;
    public String fixedTaxId;
    public String fixedCommonName;
    public String fixedScientificName;

    @Override
    public String toString() {
        return "EnaDatabaseSample{" +
                "lastUpdated='" + lastUpdated + '\'' +
                ", firstPublic='" + firstPublic + '\'' +
                ", brokerName='" + brokerName + '\'' +
                ", bioSamplesId='" + bioSamplesId + '\'' +
                ", centreName='" + centreName + '\'' +
                ", fixed='" + fixed + '\'' +
                ", taxId='" + taxId + '\'' +
                ", scientificName='" + scientificName + '\'' +
                ", fixedTaxId='" + fixedTaxId + '\'' +
                ", fixedCommonName='" + fixedCommonName + '\'' +
                ", fixedScientificName='" + fixedScientificName + '\'' +
                '}';
    }
}