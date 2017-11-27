package uk.ac.ebi.biosamples.service;

import java.time.Instant;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class XmlSampleToSampleConverter implements Converter<Element, Sample>  {

	@Override
	public Sample convert(Element doc) {
				
		Instant release = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(XmlPathBuilder.of(doc).attribute("submissionReleaseDate")));
		Instant update = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(XmlPathBuilder.of(doc).attribute("submissionUpdateDate")));
		String accession = null;
		if (XmlPathBuilder.of(doc).attributeExists("id")) {
			accession = XmlPathBuilder.of(doc).attribute("id");
		}
		String name = null;
		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		
		for (Element property : XmlPathBuilder.of(doc).elements("Property")){
			if ("Sample Name".equals(XmlPathBuilder.of(property).attribute("class"))) {
				name = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
			} else if ("Sample Description".equals(XmlPathBuilder.of(property).attribute("class"))) {
				String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
				attributes.add(Attribute.build("description", value));	
			} else {
				String type = XmlPathBuilder.of(property).attribute("class");
				String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
				Collection<String> iri = Lists.newArrayList();
				String unit = null;
				
				if (XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF").exists()
						&& XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "TermSourceID").exists()) {
					iri.add(XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "TermSourceID").text());
				}
				
				if (XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF").exists()
						&& XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "Unit").exists()) {
					unit = XmlPathBuilder.of(property).path("QualifiedValue", "TermSourceREF", "Unit").text();
				}
				
				attributes.add(Attribute.build(type, value, iri, unit));				
			}
			//TODO relationships?
		}
		for (Element derivedFrom : XmlPathBuilder.of(doc).elements("derivedFrom")) {
			relationships.add(Relationship.build(accession, "derived from", derivedFrom.getTextTrim()));
		}

		for (Element database : XmlPathBuilder.of(doc).elements("Database")) {
			if (XmlPathBuilder.of(database).path("URI").exists()) {
				externalReferences.add(ExternalReference.build(XmlPathBuilder.of(database).path("URI").text()));
			}
		}
		
		
		return Sample.build(name, accession, null, release, update, attributes, relationships, externalReferences, null, null, null);
	}

}
