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
package uk.ac.ebi.biosamples.utils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailSender {
  private static final String BODY_PART_FAIL = " failed execution on ";
  private static final String MAILX = "mailx";
  private static final String SUBJECT = "-s Email from pipeline ";
  private static final String RECIPIENT = "biosamples-dev@ebi.ac.uk";
  private static final String BODY_PART_SUCCESS = " pipeline execution successful for ";
  public static final String FAILED_FILES_ARE = " Failed files are: ";
  private static Logger log = LoggerFactory.getLogger("MailSender");

  public static void sendEmail(
      final String pipelineName, final String failures, final boolean isPassed) {
    try {
      final String[] cmd = {MAILX, SUBJECT + pipelineName, RECIPIENT};
      final Process p = Runtime.getRuntime().exec(cmd);
      final OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream());

      if (isPassed) {
        if (failures != null && !failures.isEmpty())
          osw.write(pipelineName + BODY_PART_SUCCESS + new Date() + FAILED_FILES_ARE + failures);
        else osw.write(pipelineName + BODY_PART_SUCCESS + new Date());
      } else osw.write(pipelineName + BODY_PART_FAIL + new Date());

      osw.close();
    } catch (final IOException ioe) {
      log.error("Email send failed");
    }
  }
}
