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
package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.biosamples.model.Sample;

public class ExceptionAsTextHttpMessageConverter extends AbstractHttpMessageConverter<Exception> {

  private final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = Arrays.asList(MediaType.TEXT_PLAIN);

  private Logger log = LoggerFactory.getLogger(getClass());

  public ExceptionAsTextHttpMessageConverter() {
    this.setSupportedMediaTypes(this.DEFAULT_SUPPORTED_MEDIA_TYPES);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Sample.class.isAssignableFrom(clazz);
  }

  @Override
  protected Exception readInternal(Class<? extends Exception> clazz, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {
    throw new HttpMessageNotReadableException("Cannot read exceptions");
  }

  @Override
  protected void writeInternal(Exception exception, HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    log.trace("Writing message");
    PrintWriter printWritter = new PrintWriter(outputMessage.getBody());
    exception.printStackTrace(printWritter);
    // don't close the writer, underlying outputstream will be closed elsewhere
  }
}
