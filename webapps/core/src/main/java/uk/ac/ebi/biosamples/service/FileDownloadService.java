package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileDownloadService {
    private final SamplePageService samplePageService;

    public FileDownloadService(SamplePageService samplePageService) {
        this.samplePageService = samplePageService;
    }

    public InputStream getDownloadStream(String text, Collection<Filter> filters, Collection<String> domains, String format) {
        FileDownloadSerializer serializer = FileDownloadSerializer.getSerializerFor(format);
        return new FileDownloadInputStream(samplePageService, text, filters, domains, serializer);
    }

    public void copyAndCompress(InputStream in, OutputStream out, boolean zip, String format) throws IOException {
        if (zip) {
            zip(in, out, format);
        } else {
            StreamUtils.copy(in, out);
        }
    }

    private void zip(InputStream in, OutputStream out, String format) throws IOException {
        try (ZipOutputStream zippedOut = new ZipOutputStream(out)) {
            ZipEntry zipEntry = new ZipEntry("samples." + format);
            zippedOut.putNextEntry(zipEntry);
            StreamUtils.copy(in, zippedOut);

            zippedOut.closeEntry();
            zippedOut.finish();
        }
    }
}
