/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.curation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredCell;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.model.structured.StructuredEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredTable;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;

public class MigrationCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(MigrationCallable.class);
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  private final MongoSample mongoSample;
  private final MongoSampleRepository mongoSampleRepository;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;

  public MigrationCallable(
      MongoSample mongoSample,
      MongoSampleRepository mongoSampleRepository,
      MongoStructuredDataRepository mongoStructuredDataRepository) {
    this.mongoSample = mongoSample;
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
  }

  @Override
  public PipelineResult call() {
    int modifiedRecords = 0;
    boolean success = true;

    Set<AbstractData> abstractDataSet = mongoSample.getData();
    Set<StructuredDataTable> structuredDataTableSet = new HashSet<>(abstractDataSet.size());
    for (AbstractData d : abstractDataSet) {
      Set<Map<String, StructuredDataEntry>> dataEntrySet = new HashSet<>();
      if (d instanceof AMRTable) {
        for (AMREntry e : ((AMRTable) d).getStructuredData()) {
          Map<String, StructuredDataEntry> dataEntryMap = new HashMap<>();
          dataEntrySet.add(dataEntryMap);
          dataEntryMap.put(
              "antibioticName",
              StructuredDataEntry.build(
                  e.getAntibioticName().getValue(), e.getAntibioticName().getIri()));
          dataEntryMap.put(
              "species",
              StructuredDataEntry.build(e.getSpecies().getValue(), e.getSpecies().getIri()));
          dataEntryMap.put(
              "resistancePhenotype", StructuredDataEntry.build(e.getResistancePhenotype(), null));
          dataEntryMap.put(
              "measurementSign", StructuredDataEntry.build(e.getMeasurementSign(), null));
          dataEntryMap.put("measurement", StructuredDataEntry.build(e.getMeasurement(), null));
          dataEntryMap.put(
              "measurementUnits", StructuredDataEntry.build(e.getMeasurementUnits(), null));
          dataEntryMap.put("vendor", StructuredDataEntry.build(e.getVendor(), null));
          dataEntryMap.put(
              "laboratoryTypingMethod",
              StructuredDataEntry.build(e.getLaboratoryTypingMethod(), null));
          dataEntryMap.put("platform", StructuredDataEntry.build(e.getPlatform(), null));
          dataEntryMap.put(
              "laboratoryTypingMethodVersionOrReagent",
              StructuredDataEntry.build(e.getLaboratoryTypingMethodVersionOrReagent(), null));
          dataEntryMap.put("astStandard", StructuredDataEntry.build(e.getAstStandard(), null));
          dataEntryMap.put("dstMedia", StructuredDataEntry.build(e.getDstMedia(), null));
          dataEntryMap.put("dstMethod", StructuredDataEntry.build(e.getDstMethod(), null));
          dataEntryMap.put(
              "criticalConcentration",
              StructuredDataEntry.build(e.getCriticalConcentration(), null));
          dataEntryMap.put(
              "breakpointVersion", StructuredDataEntry.build(e.getBreakpointVersion(), null));
        }
      } else {
        for (StructuredEntry e : ((StructuredTable<StructuredEntry>) d).getStructuredData()) {
          Map<String, StructuredDataEntry> dataEntryMap = new HashMap<>();
          dataEntrySet.add(dataEntryMap);
          for (Entry<String, StructuredCell> mapEntry : e.getDataAsMap().entrySet()) {
            dataEntryMap.put(
                mapEntry.getKey(),
                StructuredDataEntry.build(
                    mapEntry.getValue().getValue(), mapEntry.getValue().getIri()));
          }
        }
      }
      StructuredDataTable structuredDataTable =
          StructuredDataTable.build(
              d.getDomain(),
              d.getWebinSubmissionAccountId(),
              d.getDataType().toString(),
              d.getSchema().toString(),
              dataEntrySet);
      structuredDataTableSet.add(structuredDataTable);
    }

    MongoStructuredData mongoStructuredData =
        MongoStructuredData.build(
            mongoSample.getAccession(),
            mongoSample.getCreate(),
            mongoSample.getUpdate(),
            structuredDataTableSet);

    mongoSample.getData().removeAll(mongoSample.getData());
    mongoSampleRepository.save(mongoSample);
    mongoStructuredDataRepository.save(mongoStructuredData);
    modifiedRecords++;

    return new PipelineResult(mongoSample.getAccession(), modifiedRecords, success);
  }
}
