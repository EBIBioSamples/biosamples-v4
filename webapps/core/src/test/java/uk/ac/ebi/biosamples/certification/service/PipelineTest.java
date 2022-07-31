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
package uk.ac.ebi.biosamples.certification.service;

/*@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      CertifyService.class,
      Interrogator.class,
      FileRecorder.class,
      Curator.class,
      Certifier.class,
      ConfigLoader.class,
      Validator.class,
      Applicator.class,
      Identifier.class,
      NullRecorder.class,
      CurationPersistService.class,
      MongoCurationLinkRepository.class
    },
    properties = {"job.autorun.enabled=false"})*/
public class PipelineTest {
  /*@Autowired
  private CertifyService pipeline;

  @Test
  public void given_ncbi_sample_run_pipeline_for_SAMN03894263() throws IOException {
      String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
      BioSamplesCertificationComplainceResult rr = pipeline.run(data);
      assertEquals(3, rr.getAllCertificates().size());
  }

  @Test
  public void given_ncbi_sample_run_pipeline_for_SAMN03894261() throws IOException {
      String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894261.json"), "UTF8");
      BioSamplesCertificationComplainceResult rr = pipeline.run(data);
      assertEquals(3, rr.getAllCertificates().size());
  }

  @Test
  public void given_ncbi_sample_run_pipeline_for_SAMD00141632() throws IOException {
      String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMD00141632.json"), "UTF8");
      BioSamplesCertificationComplainceResult rr = pipeline.run(data);
      assertEquals(3, rr.getAllCertificates().size());
  }

  @Test
  public void given_ncbi_sample_run_pipeline_for_SAMD00000001() throws IOException {
      String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMD00000001.json"), "UTF8");
      BioSamplesCertificationComplainceResult rr = pipeline.run(data);
      assertEquals(3, rr.getAllCertificates().size());
      assertNotEquals(rr.getAllCertificates().get(0).getChecklist(), rr.getAllCertificates().get(1).getChecklist());
  }

  @Test
  public void given_ncbi_sample_run_pipeline_for_SAMD00000001_non_pretty() throws IOException {
      String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMD00000001-non-pretty.json"), "UTF8");
      BioSamplesCertificationComplainceResult rr = pipeline.run(data);
      assertEquals(3, rr.getAllCertificates().size());
      assertNotEquals(rr.getAllCertificates().get(0).getChecklist(), rr.getAllCertificates().get(1).getChecklist());
  }*/
}
