package uk.ac.ebi.biosamples.model;

import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Attribute;
import org.springframework.hateoas.PagedResources;

import java.util.ArrayList;
import java.util.List;

public class ResultQuery {

    private final Namespace XMLNS = Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/ResultQuery/1.0");

    public ResultQuery(PagedResources<Sample> sampleResources) {
        Document doc = startDocument();
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

    private Element getSummary(PagedResources<Sample> resources) {
        Element summary = new Element("SummaryInfo");
        return summary;
    }

    private List<Element> getAccessionList(PagedResources<Sample> resources) {
        List<Element> accessions = new ArrayList<>();
        return accessions;
    }

    private Element getDocumentRoot() {
        Element root = new Element("ResultQuery", XMLNS);

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


}
