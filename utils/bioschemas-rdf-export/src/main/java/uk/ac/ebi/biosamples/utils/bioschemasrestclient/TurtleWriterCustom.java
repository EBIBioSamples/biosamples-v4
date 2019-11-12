package uk.ac.ebi.biosamples.utils.bioschemasrestclient;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.Writer;

public class TurtleWriterCustom extends TurtleWriter {
    public TurtleWriterCustom(Writer out) {
        super(out);
    }

    public void writeValue(Value val, boolean canShorten) throws IOException {
        super.writeValue(val, canShorten);
    }

    public void writeNameSpace(String prefix, String name) throws IOException {
        super.writeNamespace(prefix, name);
    }
}
