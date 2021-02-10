package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

@Service
public class FileDownloadService {

    public ZipOutputStream downloadCompressed() {
        return null;
    }

    private void writeToStream(OutputStream stream) {

    }

    private OutputStream getDownloadStream() {
        return null;
    }

    private ZipOutputStream compressStream() {
        return null;
    }
}
