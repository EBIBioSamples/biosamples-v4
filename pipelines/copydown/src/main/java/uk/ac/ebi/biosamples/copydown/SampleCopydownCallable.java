package uk.ac.ebi.biosamples.copydown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SampleCopydownCallable implements Callable<Void> {
    private static final Attribute mixedAttribute = Attribute.build("organism", "mixed sample", "http://purl.obolibrary.org/obo/NCBITaxon_1427524", null);
    private final Sample sample;
    private final BioSamplesClient bioSamplesClient;
    private final String domain;
    private Logger log = LoggerFactory.getLogger(getClass());
    static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

    public SampleCopydownCallable(final BioSamplesClient bioSamplesClient, final Sample sample, final String domain) {
        this.bioSamplesClient = bioSamplesClient;
        this.sample = sample;
        this.domain = domain;
    }

    @Override
    public Void call() {
        final String accession = sample.getAccession();
        final boolean hasOrganism = sample.getAttributes().stream().anyMatch(attribute -> "organism".equals(attribute.getType().toLowerCase()));
        final boolean hasDerivedFrom = sample.getRelationships().stream().anyMatch
                (relationship -> "derived from".equals(relationship.getType().toLowerCase()) && accession.equals(relationship.getSource()));

        if (!hasOrganism && hasDerivedFrom) {
            //walk up the derived from relationships and pull out all the organisms
            final Set<Attribute> organisms = getOrganismsForSample(sample, false);

            if (organisms.size() > 1) {
                //if there are multiple organisms, use a "mixed sample" taxonomy reference
                //some users expect one taxonomy reference, no more, no less
                log.debug("Applying curation to " + accession);
                applyCuration(mixedAttribute);
            } else if (organisms.size() == 1) {
                log.debug("Applying curation to " + accession);
                applyCuration(organisms.iterator().next());
            } else {
                failedQueue.add(accession);
                log.warn("Unable to find organism for " + accession);
            }
        } else if (hasOrganism && hasDerivedFrom) {
            //this sample has an organism, but that might have been applied by a previous curation
            bioSamplesClient.fetchCurationLinksOfSample(accession).forEach(curationLink -> {
                if (domain.equals(curationLink.getContent().getDomain())) {
                    SortedSet<Attribute> attributesPre = curationLink.getContent().getCuration().getAttributesPre();
                    SortedSet<Attribute> attributesPost = curationLink.getContent().getCuration().getAttributesPost();
                    //check that this is as structured as expected
                    if (attributesPre.size() != 0) {
                        throw new RuntimeException("Expected no pre attribute, got " + attributesPre.size());
                    }
                    if (attributesPost.size() != 1) {
                        throw new RuntimeException("Expected single post attribute, got " + attributesPost.size());
                    }
                    //this curation link was applied by us, check it is still valid
                    final Set<Attribute> organisms = getOrganismsForSample(sample, true);

                    if (organisms.size() > 1) {
                        //check if the postattribute is the same as the organisms
                        final String organism = "mixed sample";

                        if (!organism.equals(attributesPost.iterator().next().getValue())) {
                            log.debug("Replacing curation on " + accession + " with \"mixed Sample\"");

                            bioSamplesClient.deleteCurationLink(curationLink.getContent());
                            applyCuration(mixedAttribute);
                        }
                    } else if (organisms.size() == 1) {
                        //check if the postattribute is the same as the organisms
                        final Attribute organism = organisms.iterator().next();

                        if (!organism.getValue().equals(attributesPost.iterator().next().getValue())) {
                            log.debug("Replacing curation on " + accession + " with " + organism);

                            bioSamplesClient.deleteCurationLink(curationLink.getContent());
                            applyCuration(organism);
                        }
                    }
                }
            });
        }

        return null;
    }

    private void applyCuration(final Attribute organismValue) {
        final Set<Attribute> postAttributes = new HashSet<>();

        postAttributes.add(organismValue);
        final Curation curation = Curation.build(Collections.emptyList(), postAttributes);

        bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
    }

    private Set<Attribute> getOrganismsForSample(Sample sample, boolean ignoreSample) {
        final Set<Attribute> organisms = new HashSet<>();

        if (!ignoreSample) {
            sample.getAttributes().stream().filter(attribute -> "organism".equals(attribute.getType().toLowerCase())).forEach(attribute -> organisms.add(attribute));
        }
        //if there are no organisms directly, check derived from relationships
        if (organisms.size() == 0) {
            log.trace("" + sample.getAccession() + " has no organism");

            sample.getRelationships().stream().
                    filter(relationship -> "derived from".equals(relationship.getType().toLowerCase()) && sample.getAccession().equals(relationship.getSource())).
                    forEach(relationship -> {
                        log.trace("checking derived from " + relationship.getTarget());

                        //recursion ahoy!
                        bioSamplesClient.fetchSampleResource(relationship.getTarget())
                                .ifPresent(sampleResource -> organisms.addAll(getOrganismsForSample(sampleResource.getContent(), false)));
                    });
        }

        return organisms;
    }
}
