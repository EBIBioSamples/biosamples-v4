package uk.ac.ebi.biosamples.service;

import java.math.BigInteger;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.legacyxml.SummaryInfo;


@Service
public class SummaryInfoService {

	public SummaryInfo fromPagedSampleResources(PagedResources<?> resources) {
				
		PageMetadata metadata = resources.getMetadata();
		
		SummaryInfo summaryInfo = new SummaryInfo();
		summaryInfo.setTotal(BigInteger.valueOf(metadata.getTotalElements()));
		summaryInfo.setFrom(BigInteger.valueOf(((metadata.getNumber()-1)*metadata.getSize())+1));
		summaryInfo.setTo(BigInteger.valueOf(metadata.getNumber()*metadata.getSize()));
		summaryInfo.setPageNumber(BigInteger.valueOf(metadata.getNumber()));
		summaryInfo.setPageSize(BigInteger.valueOf(metadata.getSize()));
		
		return summaryInfo;
	}

	public uk.ac.ebi.biosamples.model.legacyxml.SummaryInfo fromPagedGroupResources(PagedResources<?> resources) {
				
		PageMetadata metadata = resources.getMetadata();
		
		SummaryInfo summaryInfo = new SummaryInfo();
		summaryInfo.setTotal(BigInteger.valueOf(metadata.getTotalElements()));
		summaryInfo.setFrom(BigInteger.valueOf(((metadata.getNumber()-1)*metadata.getSize())+1));
		summaryInfo.setTo(BigInteger.valueOf(metadata.getNumber()*metadata.getSize()));
		summaryInfo.setPageNumber(BigInteger.valueOf(metadata.getNumber()));
		summaryInfo.setPageSize(BigInteger.valueOf(metadata.getSize()));
		
		return summaryInfo;
	}
}
