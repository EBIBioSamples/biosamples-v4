package uk.ac.ebi.biosamples.service;

import com.google.common.base.Strings;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class SampleManipulationService {

    /**
     * Remove the legacy fields from the contacts in the sample
     * @param sample
     * @return a sample which contacts has only name field
     */
    public Sample removeLegacyFields(Sample sample) {
        SortedSet<Contact> contacts = sample.getContacts().stream()
                .map(this::removeContactLegacyFields)
                .collect(Collectors.toCollection(TreeSet::new));

//        return Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
//                sample.getRelease(), sample.getUpdate(), sample.getCharacteristics(),
//                sample.getRelationships(), sample.getExternalReferences(), sample.getOrganizations(),
//                contacts, sample.getPublications());
        return Sample.Builder.fromSample(sample).withContacts(contacts).build();
    }

    /**
     * Reset the sample update date to now
     * @param sample
     * @return sample which update date is set to current instant
     */
    public Sample setUpdateDateToNow(Sample sample) {
//        return Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
//                sample.getRelease(), Instant.now(),
//                sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences(),
//                sample.getOrganizations(), sample.getContacts(), sample.getPublications());
        return Sample.Builder.fromSample(sample).withUpdateDate(Instant.now()).build();
    }


    /**
     * Maintain the name from the contact as unique field
     * @param contact
     * @return the polished contact
     */
    private Contact removeContactLegacyFields(Contact contact) {
        String name = contact.getName();
        if (Strings.isNullOrEmpty(name)) {
            String nullSafeFirstName = contact.getFirstName() == null ? "" : contact.getFirstName();
            String nullSafeLastName = contact.getLastName() == null ? "" : contact.getLastName();
            String fullName = (nullSafeFirstName + " " + nullSafeLastName).trim();

            if (!fullName.isEmpty()) name = fullName;

        }
        return new Contact.Builder().name(name).build();

    }

}
