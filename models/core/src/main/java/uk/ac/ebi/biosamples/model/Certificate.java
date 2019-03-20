package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Certificate {

    private CertificateSample certificateSample;

    private CertificateChecklist certificateChecklist;

    private List<CertificateCuration> certificateCurations;

    @JsonProperty("sample")
    public CertificateSample getCertificateSample() {
        return certificateSample;
    }

    public void setCertificateSample(CertificateSample certificateSample) {
        this.certificateSample = certificateSample;
    }

    @JsonProperty("checklist")
    public CertificateChecklist getCertificateChecklist() {
        return certificateChecklist;
    }

    public void setCertificateChecklist(CertificateChecklist certificateChecklist) {
        this.certificateChecklist = certificateChecklist;
    }

    @JsonProperty("curations")
    public List<CertificateCuration> getCertificateCurations() {
        return certificateCurations;
    }

    public void setCertificateCurations(List<CertificateCuration> certificateCurations) {
        this.certificateCurations = certificateCurations;
    }

    static class CertificateSample {

        private String accession;

        private String hash;

        public CertificateSample() {
        }

        public String getAccession() {
            return accession;
        }

        public void setAccession(String accession) {
            this.accession = accession;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        @Override
        public String toString() {
            return "CertificateSample{" +
                    "accession='" + accession + '\'' +
                    ", hash='" + hash + '\'' +
                    '}';
        }
    }

    static class CertificateChecklist {

        private String name;

        private String version;

        private String file;

        public CertificateChecklist() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        @Override
        public String toString() {
            return "CertificateChecklist{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", file='" + file + '\'' +
                    '}';
        }
    }

    public static class CertificateCuration {

        private String characteristic;

        private String before;

        private String after;

        private boolean applied;

        public CertificateCuration() {
        }

        public String getCharacteristic() {
            return characteristic;
        }

        public void setCharacteristic(String characteristic) {
            this.characteristic = characteristic;
        }

        public String getBefore() {
            return before;
        }

        public void setBefore(String before) {
            this.before = before;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }

        public boolean isApplied() {
            return applied;
        }

        public void setApplied(boolean applied) {
            this.applied = applied;
        }

        @Override
        public String toString() {
            return "CertificateCuration{" +
                    "characteristic='" + characteristic + '\'' +
                    ", before='" + before + '\'' +
                    ", after='" + after + '\'' +
                    ", applied=" + applied +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "certificateSample=" + certificateSample +
                ", certificateChecklist=" + certificateChecklist +
                ", certificateCurations=" + certificateCurations +
                '}';
    }
}
