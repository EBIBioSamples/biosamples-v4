package uk.ac.ebi.biosamples.ena.amr;

public class AccessionFtpUrlPair {
    String accession;
    String ftpUrl;

    public AccessionFtpUrlPair(String accession, String ftpUrl) {
        this.accession = accession;
        this.ftpUrl = ftpUrl;
    }

    public AccessionFtpUrlPair() {

    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public void setFtpUrl(String ftpUrl) {
        this.ftpUrl = ftpUrl;
    }

    public String getAccession() {
        return accession;
    }

    public String getFtpUrl() {
        return ftpUrl;
    }
}
