/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dom4j.Element;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class XmlSampleToSampleConverter implements Converter<Element, Sample> {

  @Override
  public Sample convert(Element doc) {

    Instant release = Instant.now();
    if (XmlPathBuilder.of(doc).attributeExists("submissionReleaseDate")) {
      release =
          Instant.from(
              DateTimeFormatter.ISO_DATE_TIME.parse(
                  XmlPathBuilder.of(doc).attribute("submissionReleaseDate")));
    }
    Instant update = Instant.now();
    if (XmlPathBuilder.of(doc).attributeExists("submissionUpdateDate")) {
      update =
          Instant.from(
              DateTimeFormatter.ISO_DATE_TIME.parse(
                  XmlPathBuilder.of(doc).attribute("submissionUpdateDate")));
    }
    String accession = null;
    if (XmlPathBuilder.of(doc).attributeExists("id")) {
      accession = XmlPathBuilder.of(doc).attribute("id");
    }
    String name = null;
    SortedSet<Attribute> attributes = new TreeSet<>();
    SortedSet<Relationship> relationships = new TreeSet<>();
    SortedSet<ExternalReference> externalReferences = new TreeSet<>();

    for (Element property : XmlPathBuilder.of(doc).elements("Property")) {
      if ("Sample Name".equals(XmlPathBuilder.of(property).attribute("class"))) {
        name = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
      } else if ("Sample Description".equals(XmlPathBuilder.of(property).attribute("class"))) {
        String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
        attributes.add(Attribute.build("description", value));
        // relationships
      } else if ("Child Of".equals(XmlPathBuilder.of(property).attribute("class"))) {
        String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
        relationships.add(Relationship.build(accession, "child of", value));
      } else if ("recurated from".equals(XmlPathBuilder.of(property).attribute("class"))) {
        String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
        relationships.add(Relationship.build(accession, "recurated from", value));
      } else if ("Same As".equals(XmlPathBuilder.of(property).attribute("class"))) {
        String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
        relationships.add(Relationship.build(accession, "same as", value));
        // everything else
      } else {
        String type = XmlPathBuilder.of(property).attribute("class");
        String value = XmlPathBuilder.of(property).path("QualifiedValue", "Value").text();
        Collection<String> iri = Lists.newArrayList();
        String unit = null;

        if (XmlPathBuilder.of(property)
            .path("QualifiedValue", "TermSourceREF", "TermSourceID")
            .exists()) {
          iri.add(
              XmlPathBuilder.of(property)
                  .path("QualifiedValue", "TermSourceREF", "TermSourceID")
                  .text());
        }

        if (XmlPathBuilder.of(property).path("QualifiedValue", "Unit").exists()) {
          unit = XmlPathBuilder.of(property).path("QualifiedValue", "Unit").text();
        }

        attributes.add(Attribute.build(type, value, null, iri, unit));
      }
    }
    for (Element derivedFrom : XmlPathBuilder.of(doc).elements("derivedFrom")) {
      relationships.add(Relationship.build(accession, "derived from", derivedFrom.getTextTrim()));
    }

    for (Element database : XmlPathBuilder.of(doc).elements("Database")) {
      if (XmlPathBuilder.of(database).path("URI").exists()
          && XmlPathBuilder.of(database).path("URI").text().trim().length() > 0) {
        externalReferences.add(
            ExternalReference.build(XmlPathBuilder.of(database).path("URI").text()));
      }
    }

    //		return Sample.build(name, accession, null, release, update, attributes, relationships,
    // externalReferences, null, null, null);
    return new Sample.Builder(name, accession)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .build();
  }
}
