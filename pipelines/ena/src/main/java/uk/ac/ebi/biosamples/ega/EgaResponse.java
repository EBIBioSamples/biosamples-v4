package uk.ac.ebi.biosamples.ega;

import lombok.Data;

import java.util.List;

@Data
public class EgaResponse {
    private Response response;
}

@Data
class Response {
    private long numTotalResults;
    private List<Result> result;
}

@Data
class Result {
    private String egaStableId;
    private String centerName;
    private String title;
    private String bioSampleId;
    private String subjectId;
    private String gender;
    private String phenotype;
}