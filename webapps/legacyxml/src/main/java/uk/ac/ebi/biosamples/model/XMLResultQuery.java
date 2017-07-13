package uk.ac.ebi.biosamples.model;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "ResultQuery")//, namespace = "http://www.ebi.ac.uk/biosamples/ResultQuery/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"summaryInfo", "sampleList"})
public class XMLResultQuery {

    public XMLResultQuery() {}

    @XmlElement(name="BioSample")
    private Collection<ResultEntry> sampleList;

    @XmlElement(name="SummaryInfo")
    private SummaryInfo summaryInfo;

    private void setSampleList(Collection<ResultEntry> sampleList) {
        this.sampleList = sampleList;
    }

    private void setSummaryInfo(PagedResources.PageMetadata pageMetadata) {
        this.summaryInfo = SummaryInfo.fromPageMetadata(pageMetadata);
    }

    public Collection<ResultEntry> getSampleList() {
        return this.sampleList;
    }

    public static XMLResultQuery fromPagedResource(PagedResources<Resource<Sample>> resources, BioSampleEntity entity) {

        XMLResultQuery resultQuery = new XMLResultQuery();
        resultQuery.setSummaryInfo(resources.getMetadata());
        List<ResultEntry> sampleList =resources.getContent().stream().map((Resource<Sample> s) -> {
            String accession = s.getContent().getAccession();
            if (entity.equals(BioSampleEntity.GROUP)) {
                return new BioSampleGroupResultEntry(accession);
            } else {
                return new BioSampleResultEntry(accession);
            }
        }).collect(Collectors.toList());
        resultQuery.setSampleList(sampleList);
        return resultQuery;
    }
}

