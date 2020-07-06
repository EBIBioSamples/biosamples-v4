package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Certificate implements Comparable<Certificate> {
    private String name;
    private String version;
    private String fileName;

    public Certificate(String name, String version, String fileName) {
        this.name = name;
        this.version = version;
        this.fileName = fileName;
    }

    public Certificate() {

    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Certificate)) return false;
        Certificate that = (Certificate) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getVersion(), that.getVersion()) &&
                Objects.equals(getFileName(), that.getFileName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getVersion(), getFileName());
    }

    @Override
    public int compareTo(Certificate o) {
        return 0;
    }


    @JsonCreator
    public static Certificate build(@JsonProperty("name") String name, @JsonProperty("version") String version, @JsonProperty("fileName") String fileName) {
        //check for nulls
        if (name == null) {
            throw new IllegalArgumentException("Certificate name must not be null");
        }

        if (version == null) {
            version = "";
        }

        if (fileName == null) {
            throw new IllegalArgumentException("Certificate file name must not be null");
        }

        if (name != null) name = name.trim();
        if (version != null) version = version.trim();

        Certificate cert = new Certificate();
        cert.name = name;
        cert.fileName = fileName;
        cert.version = version;

        return cert;
    }
}
