package uk.ac.ebi.biosamples.ena;

import java.sql.SQLException;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class EraProDaoTest {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private EraProDao eraPropDao;
	
	@Test
	public void testGetSampleXml() throws SQLException {
//		String eraXml = eraPropDao.getSampleXml("SAMEA4590046");
//		//String eraXml = eraPropDao.getSampleXml("SAMEA749881");
//		
//		log.info(eraXml);
////		Assert.assertNotNull(eraXml);
	}
}
