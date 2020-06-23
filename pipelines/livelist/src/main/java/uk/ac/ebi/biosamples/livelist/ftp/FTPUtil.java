package uk.ac.ebi.biosamples.livelist.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FTPUtil {
    public static void performFTP(String file) {
        FTPClient client = new FTPClient();
        FileInputStream fis = null;

        try {
            client.connect("ftp-private.ebi.ac.uk");
            client.login("embldata", "28kHVfsw");

            fis = new FileInputStream(file);

            client.storeFile(file, fis);
            client.logout();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
