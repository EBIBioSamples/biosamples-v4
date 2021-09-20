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
package uk.ac.ebi.biosamples.service.structured;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;

public class AmrPairSerializer extends StdSerializer<AmrPair> {
  public AmrPairSerializer() {
    super(AmrPair.class);
  }

  public AmrPairSerializer(Class<AmrPair> t) {
    super(t);
  }

  @Override
  public void serialize(AmrPair amrPair, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("value", amrPair.getValue());
    gen.writeStringField("iri", amrPair.getIri());
    gen.writeEndObject();
  }
}
