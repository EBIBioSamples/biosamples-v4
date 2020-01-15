package uk.ac.ebi.biosamples.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

public class MailSender {
    private static final String BODY_PART_FAIL = " failed execution on ";
    private static final String MAILX = "mailx";
    private static final String SUBJECT = "-s Email from pipeline ";
    private static final String RECIPIENT = "biosamples-dev@ebi.ac.uk";
    private static final String BODY_PART_SUCCESS = " pipeline execution successful for ";
    public static final String FAILED_FILES_ARE = " Failed files are: ";
    private static Logger log = LoggerFactory.getLogger("MailSender");

    public static void sendEmail(final String pipelineName, final String failures, final boolean isPassed) {
        try {
            final String[] cmd = {MAILX, SUBJECT + pipelineName, RECIPIENT};
            final Process p = Runtime.getRuntime().exec(cmd);
            final OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream());

            if (isPassed) {
                if (failures != null && !failures.isEmpty())
                    osw.write(pipelineName + BODY_PART_SUCCESS + new Date() + FAILED_FILES_ARE + failures);
                else
                    osw.write(pipelineName + BODY_PART_SUCCESS + new Date());
            } else osw.write(pipelineName + BODY_PART_FAIL + new Date());

            osw.close();
        } catch (final IOException ioe) {
            log.error("Email send failed");
        }
    }
}
