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
package uk.ac.ebi.biosamples.model.ga4gh.phenopacket;

import com.google.protobuf.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.Biosample;
import org.phenopackets.schema.v1.core.Disease;
import org.phenopackets.schema.v1.core.Individual;
import org.phenopackets.schema.v1.core.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

// this class to replace complete ga4gh model and phenopacket conversion, for now this is not used
public class SampleToPhenopacketConverter {
  private static final Logger LOG = LoggerFactory.getLogger(SampleToPhenopacketConverter.class);

  public Phenopacket convert(Sample bioSample) {
    Map<String, Attribute> attributes = normalizeAttributes(bioSample);

    return Phenopacket.newBuilder()
        .setMetaData(getMetadataFromSample(bioSample))
        .setSubject(getSubjectFromSample(bioSample))
        .addBiosamples(getBiosampleFromSample(bioSample))
        .addDiseases(getDiseaseFromSample(bioSample))
        .build();
  }

  private MetaData getMetadataFromSample(Sample bioSample) {
    return MetaData.newBuilder().setCreated(Timestamp.newBuilder()).build();
  }

  private Individual getSubjectFromSample(Sample bioSample) {
    return null;
  }

  private Biosample getBiosampleFromSample(Sample bioSample) {
    return null;
  }

  private Disease getDiseaseFromSample(Sample bioSample) {
    return null;
  }

  private Map<String, Attribute> normalizeAttributes(Sample bioSample) {
    Map<String, Attribute> attributeMap = new HashMap<>();

    for (Attribute attribute : bioSample.getAttributes()) {
      if (attribute.getType().equalsIgnoreCase("organism")) {
        attributeMap.put("organism", attribute);
      } else if (attribute.getType().equalsIgnoreCase("age")) {
        attributeMap.put("age", attribute);
      } else if (attribute.getType().equalsIgnoreCase("sex")) {
        attributeMap.put("sex", attribute);
      } else if (attribute.getType().equalsIgnoreCase("disease")
          || attribute.getType().equalsIgnoreCase("disease state")) {
        attributeMap.put("disease", attribute);
      }
    }

    return attributeMap;
  }
}
