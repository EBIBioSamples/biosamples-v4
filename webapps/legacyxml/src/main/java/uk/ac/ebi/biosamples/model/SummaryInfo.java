package uk.ac.ebi.biosamples.model;

import org.springframework.hateoas.PagedResources;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="SummaryInfo")
@XmlType( propOrder = {"total", "from", "to", "pageNumber", "pageSize"})
public class SummaryInfo {

    private PagedResources.PageMetadata pageMetadata;

    @XmlElement(name = "Total")
    public Long getTotal() {
        return pageMetadata.getTotalElements();
    }

    @XmlElement(name = "From")
    long getFrom() {
        return this.pageMetadata.getNumber() * getPageSize() + 1;
    }
    @XmlElement(name = "To")
    long getTo() {
        if (this.getTotal() < this.getPageEnd() ) {
            return getTotal();
        } else {
            return getPageEnd();
        }
    }
    @XmlElement(name = "PageNumber")
    long getPageNumber() {
        return this.pageMetadata.getNumber() + 1;
    }
    @XmlElement(name = "PageSize")
    long getPageSize() {
        return this.pageMetadata.getSize();
    }

    private long getPageEnd() {
        return getPageNumber() * getPageSize();
    }

    public void setPageMetadata(PagedResources.PageMetadata pageMetadata) {
        this.pageMetadata = pageMetadata;
    }

    public static SummaryInfo fromPageMetadata(PagedResources.PageMetadata pageMetadata) {
        SummaryInfo info = new SummaryInfo();
        info.setPageMetadata(pageMetadata);
        return info;
    }

}
