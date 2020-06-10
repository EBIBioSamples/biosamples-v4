package uk.ac.ebi.biosamples.ena;

import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.ebi.biosamples.client.BioSamplesClient;

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
    @Ignore
    public void test_over_all_samples() {
        RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test", false, false, null);
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
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test", false, false, null);
                try {
                    enaCallable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        eraProDao.getSingleSample("SAMEA100000168", rowCallbackHandler);
    }

    @Test
    public void test_with_suppressed() {
        RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test", false, false, null);
                try {
                    enaCallable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        eraProDao.getSingleSample("SAMEA1930638", rowCallbackHandler);
    }

    @Test
    public void test_with_killed() {
        RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test", false, false, null);
                try {
                    enaCallable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        eraProDao.getSingleSample("SAMEA1935107", rowCallbackHandler);
    }

    @Test
    public void test_with_failing() {
        RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) throws SQLException {
                String sampleAccession = resultSet.getString("BIOSAMPLE_ID");
                EnaCallable enaCallable = new EnaCallable(sampleAccession, bioSamplesClient, enaXmlEnhancer, enaElementConverter, eraProDao, "test", false, false, null);
                try {
                    enaCallable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
        eraProDao.getSingleSample("SAMEA104371999", rowCallbackHandler);
    }
}
