package uk.ac.ebi.biosamples.model;

import javax.xml.bind.annotation.XmlType;

@XmlType(name="BioSampleGroup")
public class BioSampleGroupResultEntry extends ResultEntry {


    public BioSampleGroupResultEntry(String id) {
        super(id);
    }
}
