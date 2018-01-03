package uk.ac.ebi.biosamples.legacy.json.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.model.Sample;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class TestUtilities {

    @Autowired
    private static PagedResourcesAssembler<Sample> samplePagedResourcesAssembler;

    @Autowired
    private static PagedResourcesAssembler<SamplesRelations> sampleRelationsPagedResourcesAssembler;



    public static PagedResources<Resource<Sample>> getTestSamplePagedResources(int perPage, int totalSamples) {
        List<Sample> allSamples = new ArrayList<Sample>();
        for (int i = 0; i < perPage; i++) {
            allSamples.add(new TestSample(Integer.toString(i)).build());
        }

        Pageable pageInfo = new PageRequest(0,totalSamples);
        Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
        return samplePagedResourcesAssembler.toResource(samplePage);
    }

    public static PagedResources<Resource<SamplesRelations>> getTestSampleRelationsPagedResources(int perPage, int totalSamples) {
        List<SamplesRelations> allSamples = new ArrayList<>();
        for (int i = 0; i < perPage; i++) {
            allSamples.add(new SamplesRelations(new TestSample(Integer.toString(i)).build()));
        }

        Pageable pageInfo = new PageRequest(0,totalSamples);
        Page<SamplesRelations> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
        return sampleRelationsPagedResourcesAssembler.toResource(samplePage);
    }


}
