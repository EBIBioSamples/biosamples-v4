package uk.ac.ebi.biosamples.exceptions;

public class DuplicateDomainSampleException  extends SampleTabException{

    private static final long serialVersionUID = -3469688972274912777L;
    public final String domain;
    public final String name;

    public DuplicateDomainSampleException(String domain, String name) {
        super("Multiple existing accessions of domain '"+domain+"' sample name '"+name+"'");
        this.domain = domain;
        this.name = name;
    }

}
