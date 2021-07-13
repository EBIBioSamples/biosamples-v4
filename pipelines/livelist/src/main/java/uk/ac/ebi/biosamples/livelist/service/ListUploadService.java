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
package uk.ac.ebi.biosamples.livelist.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/*Test it to work and then trun to Service */
public class ListUploadService {
  private static final int BUFFER_SIZE = 4096;

  public static void main(String[] args) {
    String ftpUrl = " ftp://embldata:28kHVfsw@ftp-private.ebi.ac.uk:21/biosamples";
    String host = "ftp-private.ebi.ac.uk";
    String user = "embldata";
    String pass = "28kHVfsw";
    String filePath = "C:\\Users\\dgupta\\test.txt";
    String uploadPath = "test.txt";

    ftpUrl = String.format(ftpUrl, user, pass, host, uploadPath);
    System.out.println("Upload URL: " + ftpUrl);

    try {
      URL url = new URL(ftpUrl);
      URLConnection conn = url.openConnection();
      OutputStream outputStream = conn.getOutputStream();
      FileInputStream inputStream = new FileInputStream(filePath);

      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead = -1;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      inputStream.close();
      outputStream.close();

      System.out.println("File uploaded");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
