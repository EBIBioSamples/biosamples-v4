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
package uk.ac.ebi.biosamples.model;

import java.io.InputStream;

public class BioSamplesUploadFile {
  private final String fileName;
  private final InputStream stream;

  public BioSamplesUploadFile(final String fileName, final InputStream stream) {
    super();
    this.fileName = fileName;
    this.stream = stream;
  }

  public String getFileName() {
    return fileName;
  }

  public InputStream getStream() {
    return stream;
  }

  @Override
  public String toString() {
    return "BioSamplesUploadFile [title=" + fileName + "]";
  }
}
