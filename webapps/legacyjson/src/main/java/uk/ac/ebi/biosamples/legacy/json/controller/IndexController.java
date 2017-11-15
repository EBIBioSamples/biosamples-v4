package uk.ac.ebi.biosamples.legacy.json.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class IndexController {

    Logger log = LoggerFactory.getLogger(getClass());


    @GetMapping(value = "/", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE} )
    public Resources root() {

        Resources resources = Resources.wrap(Collections.emptyList());
        resources.add(linkTo(methodOn(SamplesController.class).allSamples(null, null, null)).withRel("samples"));
        resources.add(linkTo(methodOn(GroupsController.class).allGroups(null, null, null)).withRel("groups"));
        resources.add(linkTo(methodOn(SamplesRelationsController.class).allSamplesRelations(null, null,null)).withRel("samplesrelations"));
        resources.add(linkTo(methodOn(GroupsRelationsController.class).allGroupsRelations(null, null, null)).withRel("groupsrelations"));
//        resources.add(linkTo(methodOn(ExternalLinksRelationsController.class).allSamplesRelations(null, null).withRel("externallinksrelations"));

        return resources;
    }

}
