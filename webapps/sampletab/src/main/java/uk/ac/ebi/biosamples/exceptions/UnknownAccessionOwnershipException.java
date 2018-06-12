package uk.ac.ebi.biosamples.exceptions;

import java.util.List;

public class UnknownAccessionOwnershipException extends SampleTabException{

    private List<String> owners;
    private final String accession;

    public UnknownAccessionOwnershipException(List<String> accessionOwners, String accession) {
        super("Impossible to determine the owner of accession "+accession);
        this.owners = accessionOwners;
        this.accession = accession;
    }
}
