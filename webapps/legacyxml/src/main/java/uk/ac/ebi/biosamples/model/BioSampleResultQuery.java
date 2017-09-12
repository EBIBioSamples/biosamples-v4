package uk.ac.ebi.biosamples.model;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "ResultQuery")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"summaryInfo", "sampleList"})
public class BioSampleResultQuery {

    public BioSampleResultQuery() {}

    @XmlElement(name="BioSample")
    protected Collection<ResultEntry> sampleList;

    @XmlElement(name="SummaryInfo")
    protected SummaryInfo summaryInfo;

    protected void setSampleList(Collection<ResultEntry> sampleList) {
        this.sampleList = sampleList;
    }

    protected void setSummaryInfo(PagedResources.PageMetadata pageMetadata) {
        this.summaryInfo = SummaryInfo.fromPageMetadata(pageMetadata);
    }

    public Collection<ResultEntry> getSampleList() {
        return this.sampleList;
    }


    public SummaryInfo getSummaryInfo() {
        return summaryInfo;
    }

    public static BioSampleResultQuery fromPagedResource(PagedResources<Resource<Sample>> resources) {

        BioSampleResultQuery resultQuery = new BioSampleResultQuery();
        resultQuery.setSummaryInfo(resources.getMetadata());
        List<ResultEntry> sampleList =resources.getContent().stream().map((Resource<Sample> s) -> {
            String accession = s.getContent().getAccession();
            return new ResultEntry(accession);
        }).collect(Collectors.toList());
        resultQuery.setSampleList(sampleList);
        return resultQuery;
    }

}

