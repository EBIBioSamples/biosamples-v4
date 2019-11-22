package uk.ac.ebi.biosamples.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

public class MailSender {
    public static final String BODY_PART_FAIL = " failed execution on ";
    private static Logger log = LoggerFactory.getLogger("MailSender");
    public static final String MAILX = "mailx";
    public static final String SUBJECT = "-s Email from copy-down pipeline";
    public static final String RECIPIENT = "biosamples-tech@ebi.ac.uk";
    public static final String BODY_PART_SUCCESS = " pipeline execution successful for ";

    public static void sendEmail(String pipelineName, String failures, boolean isPassed) {
        try {
            final String[] cmd = {MAILX, SUBJECT, RECIPIENT};
            final Process p = Runtime.getRuntime().exec(cmd);
            final OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream());

            if (isPassed) {
                if (failures != null && !failures.isEmpty())
                    osw.write(pipelineName + BODY_PART_SUCCESS + new Date() + " " + failures);
                else
                    osw.write(pipelineName + BODY_PART_SUCCESS + new Date());
            } else {
                osw.write(pipelineName + BODY_PART_FAIL + new Date());
            }

            osw.close();
        } catch (final IOException ioe) {
            log.error("Email send failed : ", ioe);
        }
    }
}
