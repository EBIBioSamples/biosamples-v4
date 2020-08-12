package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Checklist {
    private String name;
    private String version;
    @JsonProperty(value = "file")
    private String fileName;
    private boolean block;

    public Checklist(String name, String version, String fileName, boolean block) {
        this.name = name;
        this.version = version;
        this.fileName = fileName;
        this.block = block;
    }

    private Checklist() {

    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @JsonIgnore
    public String getFileName() {
        return fileName;
    }

    @JsonIgnore
    public String getID() {
        return String.format("%s-%s", name, version);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    @Override
    public String toString() {
        return getID();
    }
}
