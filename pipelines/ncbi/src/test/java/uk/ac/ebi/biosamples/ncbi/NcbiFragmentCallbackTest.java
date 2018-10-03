package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Element;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;

import java.io.InputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class NcbiFragmentCallbackTest {

    @Autowired
    private XmlFragmenter xmlFragmenter;

    @Autowired
    NcbiFragmentCallback sampleCallback;


    @MockBean
    NcbiElementCallableFactory ncbiElementCallableFactory;

    @Test
    public void is_able_to_extract_extract_proper_fragmenter() throws Exception {
        when(ncbiElementCallableFactory.build(any(Element.class))).thenReturn(
                new DoNothingElementCallable(null, null, null, null)
        );

        Resource resource = new ClassPathResource("ncbi-import-issue.xml");
        InputStream inputStream = resource.getInputStream();

        sampleCallback.setFromDate(LocalDate.of(2018, 9, 1));
        sampleCallback.setToDate(LocalDate.of(3000, 1,1));

        xmlFragmenter.handleStream(inputStream, "UTF-8", sampleCallback);
        assertThat(sampleCallback.getAccessions()).containsExactly("SAMN10081740");

    }

    private class DoNothingElementCallable extends NcbiElementCallable {
        public DoNothingElementCallable(TaxonomyService taxonomyService, BioSamplesClient bioSamplesClient, Element sampleElem, String domain) {
            super(taxonomyService, bioSamplesClient, sampleElem, domain);
        }

        @Override
        public Void call() throws Exception {
           return null;
        }
    }
}
