package uk.ac.ebi.biosamples.model;

import java.io.InputStream;

public class BioSamplesSubmissionFile {
    private final String fileName;
    private final InputStream stream;

    public BioSamplesSubmissionFile(final String fileName, final InputStream stream) {
        super();
        this.fileName = fileName;
        this.stream = stream;
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getStream() {
        return stream;
    }

    @Override
    public String toString() {
        return "BioSamplesSubmissionFile [title=" + fileName + "]";
    }
}
