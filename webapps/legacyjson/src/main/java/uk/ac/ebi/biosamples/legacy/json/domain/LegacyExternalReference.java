package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;

@JsonPropertyOrder(value = {"name", "acc", "url"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LegacyExternalReference {

    private String name;
    private String url;
    private String accession;

    public LegacyExternalReference() {}

    public LegacyExternalReference(ExternalReference externalReference) {
       ExternalReferenceService service = new ExternalReferenceService();
       this.name = service.getNickname(externalReference);
       this.accession = service.getDataId(externalReference).orElse("");
       this.url = externalReference.getUrl();
    }

    @JsonGetter
    public String getName() {
        return name;
    }

    @JsonGetter
    public String getUrl() {
        return url;
    }

    @JsonGetter("acc")
    public String getAccession() {
        return accession;
    }
}
