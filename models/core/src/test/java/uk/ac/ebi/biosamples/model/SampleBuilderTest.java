package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import uk.ac.ebi.biosamples.model.structured.AbstractData;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class SampleBuilderTest {

    @Test
    public void sample_build_even_if_null_data_is_provided() {
        Sample sample = new Sample.Builder("TestSample").withData(null).build();

        assertThat(sample.getName()).isEqualTo("TestSample");
    }

}
