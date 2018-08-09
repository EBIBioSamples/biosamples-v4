package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NcbiSampleConversionService {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final TaxonomyService taxonomyService;
    private final NcbiAmrConversionService amrConversionService;

    public NcbiSampleConversionService(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
        this.amrConversionService = new NcbiAmrConversionService();
    }

    public Sample convertNcbiXmlElementToSample(Element sampleElem) {
        String accession = sampleElem.attributeValue("accession");


        SortedSet<Attribute> attrs = new TreeSet<>();
        SortedSet<Relationship> rels = new TreeSet<>();
        Set<ExternalReference> externalReferences = new TreeSet<>();
        Set<AbstractData> structuredData = new HashSet<>();

        String alias = null; //this will be the ENA alias of the sample
        String geoAlias = null;
        String centreName = null; //this will be the ENA centre name of the sample.
        for (Element idElem : XmlPathBuilder.of(sampleElem).path("Ids").elements("Id")) {
            if ("BioSample".equals(idElem.attributeValue("db"))) {
                //ignore ids from BioSample
            } else if ("SRA".equals(idElem.attributeValue("db"))) {
                //INSDC SRA IDs get special treatment
                attrs.add(Attribute.build("INSDC secondary accession", idElem.getTextTrim()));
            } else if ("Sample name".equals(idElem.attributeValue("db_label"))) {
                //original submitter identifier is stored as the alias to be used as the name
                alias = idElem.getTextTrim();
                centreName = idElem.attributeValue("db");
            } else if ("GEO".equals(idElem.attributeValue("db"))) {
                //GEO IDs get special treatment
                geoAlias = idElem.getTextTrim();
            } else if (!accession.equals(idElem.getTextTrim())) {
                attrs.add(Attribute.build("synonym", idElem.getTextTrim()));
            }
        }
        if (alias == null && geoAlias != null) {
            //if theres no alias but there is a geo alias, then use the geo alias as the alias
            alias = geoAlias;
            geoAlias = null;
        }
        if (geoAlias != null) {
            //if we still have a geo alias, store it as a synonym
            attrs.add(Attribute.build("synonym", geoAlias));
        }

        if (alias == null) {
            log.warn("Unable to determine sample alias for " + accession + ", falling back to accession");
            alias = accession;
        }

        //override any existing centre name with this, if present
        if (XmlPathBuilder.of(sampleElem).path("Owner", "Name").exists()) {
            if (XmlPathBuilder.of(sampleElem).path("Owner", "Name").text().trim().length() > 0) {
                centreName = XmlPathBuilder.of(sampleElem).path("Owner", "Name").text().trim();
            }
        }

        if (centreName == null) {
            //throw new RuntimeException("Unable to determine centre name for "+accession);
            log.warn("Unable to determine centre name for " + accession);
        } else {
            //Note US spelling because NCBI
            attrs.add(Attribute.build("INSDC center name", centreName));
        }

        if (XmlPathBuilder.of(sampleElem).path("Description", "Title").exists()) {
            String value = XmlPathBuilder.of(sampleElem).path("Description", "Title").text();
            attrs.add(Attribute.build("description title", value));
        }

        if (XmlPathBuilder.of(sampleElem).path("Description", "Comment", "Paragraph").exists()) {
            String value = XmlPathBuilder.of(sampleElem).path("Description", "Comment", "Paragraph").text().trim();
			/*
			if (value.length() > 255) {
				log.warn("Truncating attribute "+key+" for length on "+accession);
				value = value.substring(0, 252)+"...";
			}
			*/
            attrs.add(Attribute.build("description", value));
        }

        // handle the organism
        String organismIri = null;
        String organismValue = null;
        if (XmlPathBuilder.of(sampleElem).path("Description", "Organism").attributeExists("taxonomy_id")) {
            int taxonId = getTaxId(XmlPathBuilder.of(sampleElem).path("Description", "Organism").attribute("taxonomy_id"));
            organismIri = taxonomyService.getUriForTaxonId(taxonId);
        }
        if (XmlPathBuilder.of(sampleElem).path("Description", "Organism").attributeExists("taxonomy_name")) {
            organismValue = XmlPathBuilder.of(sampleElem).path("Description", "Organism").attribute("taxonomy_name");
        }

        if (organismValue != null) {
            attrs.add(Attribute.build("organism", organismValue, organismIri, null));
        }


        // handle attributes
        for (Element attrElem : XmlPathBuilder.of(sampleElem).path("Attributes").elements("Attribute")) {
            String key = attrElem.attributeValue("display_name");
            if (key == null || key.length() == 0) {
                key = attrElem.attributeValue("attribute_name");
            }
            String value = attrElem.getTextTrim();
			/*
			if (value.length() > 255) {
				log.warn("Truncating attribute "+key+" for length on "+accession);
				value = value.substring(0, 252)+"...";
			}
			*/
            //if its a gap accession add an external reference too
            if (value.matches("phs[0-9]+")) {
                externalReferences.add(ExternalReference.build("https://www.ncbi.nlm.nih.gov/projects/gap/cgi-bin/study.cgi?study_id=" + value));
            }

            //value is a sample accession, assume its a relationship
            if (value.matches("SAM[END]A?[0-9]+")) {
                //if its a self-relationship, then don't add it
                //otherwise add it
                if (!value.equals(accession)) {
                    rels.add(Relationship.build(accession, key, value));
                }
            } else {
                //its an attribute
                attrs.add(Attribute.build(key, value));
            }
        }

        // handle model and packages
//disabled for the moment, do they really add anything? faulcon@2017/01/25
//yes, ENA want them. But we can name them better. faulcon@2018/02/14
        //TODO safetly access these - shouldn't ever be missing but....
        for (Element modelElem : XmlPathBuilder.of(sampleElem).path("Models").elements("Model")) {
            attrs.add(Attribute.build("NCBI submission model", modelElem.getTextTrim()));
        }
        attrs.add(Attribute.build("NCBI submission package", XmlPathBuilder.of(sampleElem).path("Package").text()));

        //handle dates
        Instant lastUpdate = Instant.parse(sampleElem.attributeValue("last_update") + "Z");
        Instant publicationDate = Instant.parse(sampleElem.attributeValue("publication_date") + "Z");

        Instant latestDate = lastUpdate;
        if (publicationDate.isAfter(latestDate)) {
            latestDate = publicationDate;
        }

        //add some INSDC things for standardisation with ENA import
        attrs.add(Attribute.build("INSDC first public",
                DateTimeFormatter.ISO_INSTANT.format(publicationDate)));
        attrs.add(Attribute.build("INSDC last update",
                DateTimeFormatter.ISO_INSTANT.format(lastUpdate)));

        if (XmlPathBuilder.of(sampleElem).path("Status").attributeExists("status")) {
            String status = XmlPathBuilder.of(sampleElem).path("Status").attribute("status").trim();
            attrs.add(Attribute.build("INSDC status", status));
            if (!"live".equals(status.toLowerCase())) {
                //not a live sample, hide
                publicationDate = publicationDate.atZone(ZoneOffset.UTC).plus(1000, ChronoUnit.YEARS).toInstant();
            }
        }

        //handle amr data
        for (Element element: XmlPathBuilder.of(sampleElem).path("Description", "Comment").elements("Table")) {
            String antibiogramClass = element.attributeValue("class");
            if (antibiogramClass != null && antibiogramClass.equalsIgnoreCase("Antibiogram.1.0")) {
                // AMR table found
                try {
                    AMRTable amrTable = amrConversionService.convertElementToAmrTable(element);
                    structuredData.add(amrTable);
                } catch (NcbiAmrConversionService.AmrParsingException ex) {
                    log.error("An error occurred while parsing AMR table", ex);
                }
            }

        }

        return new Sample.Builder(alias, accession)
                .withRelease(publicationDate).withUpdate(lastUpdate)
                .withAttributes(attrs)
                .withRelationships(rels)
                .withData(structuredData)
                .withExternalReferences(externalReferences).build();

//        return Sample.build(alias, accession, domain, publicationDate, lastUpdate, attrs, rels, externalReferences);
    }

    private int getTaxId(String value) {
        if (value == null) {
            throw new RuntimeException("Unable to extract tax id from a null value");
        }
        return Integer.parseInt(value.trim());
    }

}
