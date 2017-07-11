package uk.ac.ebi.biosamples.model;

import javax.xml.bind.annotation.XmlAttribute;

public class ResultEntry {

    public ResultEntry() {}
    public ResultEntry(String id) {
        this.id = id;
    }

    private String id;

    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
