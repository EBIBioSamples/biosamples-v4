package uk.ac.ebi.biosamples.ena;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.Application;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.service.SampleValidator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = {"job.autorun.enabled=false"})
public class TestConversion {

    @Autowired
    public BioSamplesClient bioSamplesClient;

    @Autowired
    private EraProDao eraProDao;

    @Autowired
    private EnaElementConverter enaElementConverter;

    @Autowired
    private EnaXmlEnhancer enaXmlEnhancer;

    @Test
    public void test_over_all_samples() {
        RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test");
                try {
                    enaCallable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        LocalDate fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
        eraProDao.doSampleCallback(fromDate, toDate, rowCallbackHandler);
    }

    @Test
    public void test_with_single() {
        RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test");
                try {
                    enaCallable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        eraProDao.getSingleSample("SAMEA100000168", rowCallbackHandler);
    }
}
