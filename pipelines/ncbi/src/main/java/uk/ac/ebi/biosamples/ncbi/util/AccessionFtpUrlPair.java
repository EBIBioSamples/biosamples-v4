package uk.ac.ebi.biosamples.ncbi.util;

public class AccessionFtpUrlPair {
    String accession;
    String ftpUrl;

    public AccessionFtpUrlPair(final String accession, final String ftpUrl) {
        this.accession = accession;
        this.ftpUrl = ftpUrl;
    }

    public AccessionFtpUrlPair() {

    }

    public void setAccession(final String accession) {
        this.accession = accession;
    }

    public void setFtpUrl(final String ftpUrl) {
        this.ftpUrl = ftpUrl;
    }

    public String getAccession() {
        return accession;
    }

    public String getFtpUrl() {
        return ftpUrl;
    }
}
