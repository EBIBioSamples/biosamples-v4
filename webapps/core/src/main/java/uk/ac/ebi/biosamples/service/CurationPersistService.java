/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.CurationLinkToMongoCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.CurationToMongoCurationConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;

@Service
public class CurationPersistService {

  private Logger log = LoggerFactory.getLogger(getClass());

  // TODO use constructor injection

  @Autowired private MongoCurationLinkRepository mongoCurationLinkRepository;

  @Autowired
  private CurationLinkToMongoCurationLinkConverter curationLinkToMongoCurationLinkConverter;

  @Autowired
  private MongoCurationLinkToCurationLinkConverter mongoCurationLinkToCurationLinkConverter;

  @Autowired private MongoCurationRepository mongoCurationRepository;
  @Autowired private CurationToMongoCurationConverter curationToMongoCurationConverter;

  @Autowired private MessagingService messagingSerivce;

  public CurationLink store(CurationLink curationLink) {
    // TODO do this as a trigger on the curation link repo
    // if it already exists, no need to save
    if (mongoCurationRepository.findOne(curationLink.getCuration().getHash()) == null) {
      MongoCuration mongoCuration =
          curationToMongoCurationConverter.convert(curationLink.getCuration());
      try {
        mongoCurationRepository.save(mongoCuration);
      } catch (DuplicateKeyException e) {
        // sometimes, if there are multiple threads there may be a collision
        // check if its a true duplicate and not an accidental hash collision
        MongoCuration existingMongoCuration =
            mongoCurationRepository.findOne(mongoCuration.getHash());
        if (!existingMongoCuration.equals(mongoCuration)) {
          // if it is a different curation with an hash collision, then throw an exception
          throw e;
        }
      }
    }

    // if it already exists, no need to save
    if (mongoCurationLinkRepository.findOne(curationLink.getHash()) == null) {
      curationLink =
          mongoCurationLinkToCurationLinkConverter.convert(
              mongoCurationLinkRepository.save(
                  curationLinkToMongoCurationLinkConverter.convert(curationLink)));
    }

    // for each relationship curation create reverse relationship curation
    createReverseRelationshipCurations(curationLink);

    messagingSerivce.fetchThenSendMessage(curationLink.getSample());
    return curationLink;
  }

  public void delete(CurationLink curationLink) {
    if (curationLink == null) throw new IllegalArgumentException("curationLink must not be null");
    MongoCurationLink mongoCurationLink =
        curationLinkToMongoCurationLinkConverter.convert(curationLink);
    mongoCurationLinkRepository.delete(mongoCurationLink.getHash());
    messagingSerivce.fetchThenSendMessage(curationLink.getSample());
  }

  // sample reverse relationships are dynamically generated, therefore should create for curations
  private void createReverseRelationshipCurations(CurationLink curationLink) {
    SortedSet<Relationship> relationshipsPre = curationLink.getCuration().getRelationshipsPre();
    SortedSet<Relationship> relationshipsPost = curationLink.getCuration().getRelationshipsPost();
    if (!relationshipsPre.isEmpty()) {
      for (Relationship rel : relationshipsPre) {
        SortedSet<Relationship> reverseRelationships = new TreeSet<>();
        reverseRelationships.add(
            rel); // to keep original direction, instead of adding reverse relationship
        Curation reverseCuration =
            Curation.build(null, null, null, null, reverseRelationships, null);
        CurationLink reverseCurationLink =
            CurationLink.build(
                rel.getTarget(),
                reverseCuration,
                curationLink.getDomain(),
                curationLink.getCreated());
        if (mongoCurationLinkRepository.findOne(reverseCurationLink.getHash()) == null) {
          mongoCurationLinkRepository.save(
              curationLinkToMongoCurationLinkConverter.convert(reverseCurationLink));
        }
      }
    }
    if (!relationshipsPost.isEmpty()) {
      for (Relationship rel : relationshipsPost) {
        SortedSet<Relationship> reverseRelationships = new TreeSet<>();
        reverseRelationships.add(
            rel); // to keep original direction, instead of adding reverse relationship
        Curation reverseCuration =
            Curation.build(null, null, null, null, null, reverseRelationships);
        CurationLink reverseCurationLink =
            CurationLink.build(
                rel.getTarget(),
                reverseCuration,
                curationLink.getDomain(),
                curationLink.getCreated());
        if (mongoCurationLinkRepository.findOne(reverseCurationLink.getHash()) == null) {
          mongoCurationLinkRepository.save(
              curationLinkToMongoCurationLinkConverter.convert(reverseCurationLink));
        }
      }
    }
  }
}
