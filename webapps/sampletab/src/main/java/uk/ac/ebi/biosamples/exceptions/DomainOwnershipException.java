package uk.ac.ebi.biosamples.exceptions;

public class DomainOwnershipException extends SampleTabException{

    public final String domain;
    public final String accession;

    public DomainOwnershipException(String domain, String accession) {
        super("Sample with accession "+ accession +" is not part of the domain "+domain);
        this.domain = domain;
        this.accession = accession;
    }
}
