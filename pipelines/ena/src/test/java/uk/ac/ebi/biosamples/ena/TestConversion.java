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
package uk.ac.ebi.biosamples.ena;

import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {TestApplication.class},
    properties = {"job.autorun.enabled=false"})
public class TestConversion {
  @Qualifier("WEBINCLIENT")
  @Autowired
  public BioSamplesClient bioSamplesWebinClient;

  @Qualifier("AAPCLIENT")
  @Autowired
  public BioSamplesClient bioSamplesAapClient;

  @Autowired private EraProDao eraProDao;

  @Autowired private EnaElementConverter enaElementConverter;

  @Autowired private EgaSampleExporter egaSampleExporter;

  @Autowired private EnaXmlEnhancer enaXmlEnhancer;

  @Test
  @Ignore
  public void test_over_all_samples() {
    RowCallbackHandler rowCallbackHandler =
        resultSet -> {
          String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
          EnaCallable enaCallable =
              new EnaCallable(
                  sampleAccession,
                  null,
                  0,
                  bioSamplesWebinClient,
                  enaXmlEnhancer,
                  enaElementConverter,
                  egaSampleExporter,
                  eraProDao,
                  false,
                  false,
                  null);
          try {
            enaCallable.call();
          } catch (Exception e) {
            e.printStackTrace();
          }
        };
    LocalDate fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
    LocalDate toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
    eraProDao.doSampleCallback(fromDate, toDate, rowCallbackHandler);
  }

  @Test
  public void test_with_single() {
    RowCallbackHandler rowCallbackHandler =
        resultSet -> {
          String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
          EnaCallable enaCallable =
              new EnaCallable(
                  sampleAccession,
                  null,
                  0,
                  bioSamplesWebinClient,
                  enaXmlEnhancer,
                  enaElementConverter,
                  egaSampleExporter,
                  eraProDao,
                  false,
                  false,
                  null);
          try {
            enaCallable.call();
          } catch (Exception e) {
            e.printStackTrace();
            fail();
          }
        };
    eraProDao.getSingleSample("SAMEA100000168", rowCallbackHandler);
  }

  @Test
  public void test_with_suppressed() {
    RowCallbackHandler rowCallbackHandler =
        resultSet -> {
          String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
          EnaCallable enaCallable =
              new EnaCallable(
                  sampleAccession,
                  null,
                  0,
                  bioSamplesWebinClient,
                  enaXmlEnhancer,
                  enaElementConverter,
                  egaSampleExporter,
                  eraProDao,
                  false,
                  false,
                  null);
          try {
            enaCallable.call();
          } catch (Exception e) {
            e.printStackTrace();
            fail();
          }
        };
    eraProDao.getSingleSample("SAMEA1930638", rowCallbackHandler);
  }

  @Test
  public void test_with_killed() {
    RowCallbackHandler rowCallbackHandler =
        resultSet -> {
          String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
          EnaCallable enaCallable =
              new EnaCallable(
                  sampleAccession,
                  null,
                  0,
                  bioSamplesWebinClient,
                  enaXmlEnhancer,
                  enaElementConverter,
                  egaSampleExporter,
                  eraProDao,
                  false,
                  false,
                  null);
          try {
            enaCallable.call();
          } catch (Exception e) {
            e.printStackTrace();
            fail();
          }
        };
    eraProDao.getSingleSample("SAMEA1935107", rowCallbackHandler);
  }

  @Test
  public void test_with_failing() {
    RowCallbackHandler rowCallbackHandler =
        resultSet -> {
          String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
          EnaCallable enaCallable =
              new EnaCallable(
                  sampleAccession,
                  null,
                  0,
                  bioSamplesWebinClient,
                  enaXmlEnhancer,
                  enaElementConverter,
                  egaSampleExporter,
                  eraProDao,
                  false,
                  false,
                  null);
          try {
            enaCallable.call();
          } catch (Exception e) {
            e.printStackTrace();
            fail();
          }
        };
    eraProDao.getSingleSample("SAMEA104371999", rowCallbackHandler);
  }
}
