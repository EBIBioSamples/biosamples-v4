package uk.ac.ebi.biosamples.model;

import javax.xml.bind.annotation.XmlType;

@XmlType(name="BioSample")
public class BioSampleResultEntry extends ResultEntry{

    public BioSampleResultEntry() {}

    public BioSampleResultEntry(String id) {
        super(id);
    }

}
