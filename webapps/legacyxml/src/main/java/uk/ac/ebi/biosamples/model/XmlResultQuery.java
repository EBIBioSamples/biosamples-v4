package uk.ac.ebi.biosamples.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "ResultQuery", namespace = "http://www.ebi.ac.uk/biosamples/ResultQuery/1.0")
public class XmlResultQuery {
    List<Sample> sampleList;
}
