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
package uk.ac.ebi.biosamples.neoexport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.neo4j.model.NeoExternalEntity;
import uk.ac.ebi.biosamples.neo4j.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;

@Component
public class NeoCsvExporter {
  private static final Logger LOG = LoggerFactory.getLogger(NeoCsvExporter.class);

  private static final String EXPORT_PATH = "./export/";
  private static final String REL_SOURCE_HEADER = "source";
  private static final String REL_TARGET_HEADER = "target";
  private static final int PERSIST_THRESHOLD = 1000000;

  private static final String[] SAMPLES_HEADER = {
    "accession",
    "name",
    "cellType",
    "sex",
    "taxId",
    "project",
    "material",
    "cellLine",
    "organismPart",
    "organism"
  };
  private static final String[] EXTERNAL_ENTITY_HEADER = {"name", "archive", "ref", "url"};
  private static final String[] REL_HEADER = {REL_SOURCE_HEADER, REL_TARGET_HEADER};

  private List<Map<String, String>> samples = new ArrayList<>();
  private List<Map<String, String>> externalEntity = new ArrayList<>();

  private List<Map<String, String>> relsDerivedFrom = new ArrayList<>();
  private List<Map<String, String>> relsSameAs = new ArrayList<>();
  private List<Map<String, String>> relsHasMember = new ArrayList<>();
  private List<Map<String, String>> relsChildOf = new ArrayList<>();
  private List<Map<String, String>> relsOther = new ArrayList<>();
  private List<Map<String, String>> relsExternalRef = new ArrayList<>();

  private int sampleIndex = 1;
  private int externalEntityIndex = 1;
  private int relsExternelRefIndex = 1;
  private int mockIndex = 1;

  public void addToCSVFile(Sample sample) {
    NeoSample neoSample = NeoSample.build(sample);
    addSample(neoSample);
  }

  public void flush() {
    writeCSV(samples, "samples-" + sampleIndex + ".csv", SAMPLES_HEADER, false);
    writeCSV(
        externalEntity,
        "ex_reference-" + externalEntityIndex + ".csv",
        EXTERNAL_ENTITY_HEADER,
        false);

    writeCSV(
        relsExternalRef, "external_reference-" + relsExternelRefIndex + ".csv", REL_HEADER, false);
    writeCSV(relsDerivedFrom, "derived_from-" + mockIndex + ".csv", REL_HEADER, true);
    writeCSV(relsSameAs, "same_as-" + mockIndex + ".csv", REL_HEADER, true);
    writeCSV(relsHasMember, "has_member-" + mockIndex + ".csv", REL_HEADER, true);
    writeCSV(relsChildOf, "child_of-" + mockIndex + ".csv", REL_HEADER, true);
    writeCSV(relsOther, "other-" + mockIndex + ".csv", REL_HEADER, true);
  }

  private void addSample(NeoSample sample) {
    Map<String, String> attributeMap = new HashMap<>();
    attributeMap.put("accession", sample.getAccession());
    attributeMap.put("name", sample.getName());
    attributeMap.put("organism", sample.getOrganism() == null ? "" : sample.getOrganism());
    attributeMap.put("taxId", sample.getTaxId() == null ? "" : sample.getTaxId());
    attributeMap.put("sex", sample.getSex() == null ? "" : sample.getSex());
    attributeMap.put("cellType", sample.getCellType() == null ? "" : sample.getCellType());
    attributeMap.put("material", sample.getMaterial() == null ? "" : sample.getMaterial());
    attributeMap.put("project", sample.getProject() == null ? "" : sample.getProject());
    attributeMap.put("cellLine", sample.getCellLine() == null ? "" : sample.getCellLine());
    attributeMap.put(
        "organismPart", sample.getOrganismPart() == null ? "" : sample.getOrganismPart());
    samples.add(attributeMap);

    for (NeoRelationship rel : sample.getRelationships()) {
      if (rel.getSource().equals(sample.getAccession())) {
        switch (rel.getType()) {
          case DERIVED_FROM:
            Map<String, String> e = new HashMap<>();
            e.put(REL_SOURCE_HEADER, rel.getSource());
            e.put(REL_TARGET_HEADER, rel.getTarget());
            relsDerivedFrom.add(e);
            break;
          case SAME_AS:
            Map<String, String> e1 = new HashMap<>();
            e1.put(REL_SOURCE_HEADER, rel.getSource());
            e1.put(REL_TARGET_HEADER, rel.getTarget());
            relsSameAs.add(e1);
            break;
          case HAS_MEMBER:
            Map<String, String> e2 = new HashMap<>();
            e2.put(REL_SOURCE_HEADER, rel.getSource());
            e2.put(REL_TARGET_HEADER, rel.getTarget());
            relsHasMember.add(e2);
            break;
          case CHILD_OF:
            Map<String, String> e3 = new HashMap<>();
            e3.put(REL_SOURCE_HEADER, rel.getSource());
            e3.put(REL_TARGET_HEADER, rel.getTarget());
            relsChildOf.add(e3);
            break;
          default:
            Map<String, String> e4 = new HashMap<>();
            e4.put(REL_SOURCE_HEADER, rel.getSource());
            e4.put(REL_TARGET_HEADER, rel.getTarget());
            relsOther.add(e4);
            break;
        }
      }
    }

    for (NeoExternalEntity ref : sample.getExternalRefs()) {
      String refId = ref.getArchive() + "_" + ref.getRef();
      Map<String, String> e = new HashMap<>();
      e.put("name", refId);
      e.put("archive", ref.getArchive());
      e.put("ref", ref.getRef());
      e.put("url", ref.getUrl());
      externalEntity.add(e);
      Map<String, String> e1 = new HashMap<>();
      e1.put(REL_SOURCE_HEADER, sample.getAccession());
      e1.put(REL_TARGET_HEADER, refId);
      relsExternalRef.add(e1);
    }

    checkWriteStatus();
  }

  private void checkWriteStatus() {
    if (samples.size() >= PERSIST_THRESHOLD) {
      writeCSV(samples, "samples-" + sampleIndex + ".csv", SAMPLES_HEADER, false);
      samples.clear();
      sampleIndex++;
    }
    if (externalEntity.size() >= PERSIST_THRESHOLD) {
      writeCSV(
          externalEntity,
          "ex_reference-" + externalEntityIndex + ".csv",
          EXTERNAL_ENTITY_HEADER,
          false);
      externalEntity.clear();
      externalEntityIndex++;
    }
    if (relsExternalRef.size() >= PERSIST_THRESHOLD) {
      writeCSV(
          relsExternalRef,
          "external_reference-" + relsExternelRefIndex + ".csv",
          REL_HEADER,
          false);
      relsExternalRef.clear();
      relsExternelRefIndex++;
    }

    if (relsDerivedFrom.size() >= PERSIST_THRESHOLD) {
      writeCSV(relsDerivedFrom, "derived_from-" + mockIndex + ".csv", REL_HEADER, true);
      relsDerivedFrom.clear();
    }
    if (relsSameAs.size() >= PERSIST_THRESHOLD) {
      writeCSV(relsSameAs, "same_as-" + mockIndex + ".csv", REL_HEADER, true);
      relsSameAs.clear();
    }
    if (relsHasMember.size() >= PERSIST_THRESHOLD) {
      writeCSV(relsHasMember, "has_member-" + mockIndex + ".csv", REL_HEADER, true);
      relsHasMember.clear();
    }
    if (relsChildOf.size() >= PERSIST_THRESHOLD) {
      writeCSV(relsChildOf, "child_of-" + mockIndex + ".csv", REL_HEADER, true);
      relsChildOf.clear();
    }
    if (relsOther.size() >= PERSIST_THRESHOLD) {
      writeCSV(relsOther, "other-" + mockIndex + ".csv", REL_HEADER, true);
      relsOther.clear();
    }
  }

  private void writeCSV(
      List<Map<String, String>> records, String fileName, String[] headerOrder, boolean append) {
    CsvSchema schema = null;
    CsvSchema.Builder schemaBuilder = CsvSchema.builder();
    if (records != null && !records.isEmpty()) {
      for (String col : records.get(0).keySet()) {
        schemaBuilder.addColumn(col);
      }
      schema = schemaBuilder.build();
      schema = schema.sortedBy(headerOrder);
    }

    CsvMapper mapper = new CsvMapper();
    File file = new File(EXPORT_PATH + fileName);
    try (Writer writer = new FileWriter(file, append)) {
      mapper.writer(schema).writeValues(writer).writeAll(records);
      writer.flush();
    } catch (IOException e) {
      LOG.error("Failed writing to csv file: {}", fileName, e);
    }
  }
}
