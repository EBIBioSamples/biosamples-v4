package uk.ac.ebi.biosamples.model;

import org.jdom2.Attribute;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;

import java.util.ArrayList;
import java.util.List;

public class LegacyResultQuery {

    private final Namespace XMLNS = Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/LegacyResultQuery/1.0");

    private Document doc;

    private LegacyResultQuery(PagedResources<Resource<Sample>> sampleResources) {
        this.doc = startDocument();
        Element root = getDocumentRoot();

        root.addContent(getSummary(sampleResources));
        root.addContent(getAccessionList(sampleResources));

        doc.setRootElement(root);

    }


    private Document startDocument() {

        Document doc = new Document();
        doc.addContent(new Comment("BioSamples XML API - version 1.0"));
        return doc;
    }

    private Element getSummary(PagedResources<Resource<Sample>> results) {
        Element summary = new Element("SummaryInfo", XMLNS);

        PageMetadata pageMetadata = results.getMetadata();
        long totalElementsValue = pageMetadata.getTotalElements();
        long pageNumberValue = pageMetadata.getNumber();
        long pageSizeValue = pageMetadata.getSize();
        long resultsFromValue = (pageSizeValue * pageNumberValue) + 1;
        long pageEndValue = (pageNumberValue + 1) * pageSizeValue;
        long resultsToValue = totalElementsValue < pageEndValue ? totalElementsValue : pageEndValue;

        Element total = new Element("Total").setText(Long.toString(totalElementsValue));
        Element from = new Element("From").setText(Long.toString(resultsFromValue));
        Element to = new Element("To").setText(Long.toString(resultsToValue));
        Element pageNumber = new Element("PageNumber").setText(Long.toString(pageNumberValue + 1));
        Element pageSize = new Element("PageSize").setText(Long.toString(pageSizeValue));

        summary.addContent(total);
        summary.addContent(from);
        summary.addContent(to);
        summary.addContent(pageNumber);
        summary.addContent(pageSize);
        return summary;
    }

    private List<Element> getAccessionList(PagedResources<Resource<Sample>> results) {
        List<Element> accessions = new ArrayList<>();
        for (Resource<Sample> result : results.getContent()) {
            Element rqDocument = new Element("BioSample", XMLNS);
            rqDocument.setAttribute("id", result.getContent().getAccession());
            accessions.add(rqDocument);
        }
        return accessions;
    }

    private Element getDocumentRoot() {
        Element root = new Element("LegacyResultQuery", XMLNS);

        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.addNamespaceDeclaration(xsi);

        Attribute schemaLocation = new Attribute(
                "schemaLocation",
                "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 " +
                        "http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/ResultQuerySampleSchema.xsd",
                xsi);

        root.setAttribute(schemaLocation);
        return root;
    }

    public String renderDocument() {
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        return xmlOutput.outputString(this.doc);
    }

    public static LegacyResultQuery fromPagedResource(PagedResources<Resource<Sample>> pagedResources) {
        return new LegacyResultQuery(pagedResources);
    }

}
