package uk.ac.ebi.biosamples.curation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SampleCurationCallable implements Callable<Void> {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final Sample sample;
    private final BioSamplesClient bioSamplesClient;
    private final OlsProcessor olsProcessor;
    private final CurationApplicationService curationApplicationService;
    private final String domain;

    public static final String[] NON_APPLICABLE_SYNONYMS = {"n/a", "na", "n.a", "none",
            "unknown", "--", ".", "null", "missing", "[not reported]",
            "[not requested]", "not applicable", "not_applicable", "not collected", "not specified", "not known", "not reported"};

    public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();

    public SampleCurationCallable(BioSamplesClient bioSamplesClient, Sample sample,
                                  OlsProcessor olsProcessor,
                                  CurationApplicationService curationApplicationService, String domain) {
        this.bioSamplesClient = bioSamplesClient;
        this.sample = sample;
        this.olsProcessor = olsProcessor;
        this.curationApplicationService = curationApplicationService;
        this.domain = domain;
    }

    @Override
    public Void call() throws Exception {

        try {

            Sample last = sample;
            Sample curated = sample;
            do {
                last = curated;
                curated = curate(last);
            } while (!last.equals(curated));

            do {
                last = curated;
                curated = ols(last);
            } while (!last.equals(curated));

        } catch (Exception e) {
            log.warn("Encountered exception with " + sample.getAccession(), e);
            failedQueue.add(sample.getAccession());
        }
        return null;
    }

    public Sample curate(Sample sample) {
        for (Attribute attribute : sample.getAttributes()) {
            // clean unexpected characters
            String newType = cleanString(attribute.getType());
            String newValue = cleanString(attribute.getValue());

            //if the clean type or value would be empty, curate to an non attribute
            if (newType.length() == 0 || newValue.length() == 0) {
                Curation curation = Curation.build(Collections.singleton(attribute), Collections.emptyList());
                bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                sample = curationApplicationService.applyCurationToSample(sample, curation);
                return sample;
            }


            if (!attribute.getType().equals(newType) || !attribute.getValue().equals(newValue)) {
                Attribute newAttribute = Attribute.build(newType, newValue, attribute.getIri(),
                        attribute.getUnit());
                Curation curation = Curation.build(attribute, newAttribute);
                bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                sample = curationApplicationService.applyCurationToSample(sample, curation);
                return sample;

            }

            // if no information content, remove
            if (isNotApplicableSynonym(attribute.getValue())) {
                Curation curation = Curation.build(attribute, null);
                bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                sample = curationApplicationService.applyCurationToSample(sample, curation);
                return sample;
            }

            // if it has a unit, make sure it is clean
            if (attribute.getUnit() != null) {
                String newUnit = correctUnit(attribute.getUnit());
                if (!attribute.getUnit().equals(newUnit)) {
                    Attribute newAttribute = Attribute.build(attribute.getType(), attribute.getValue(),
                            attribute.getIri(), newUnit);
                    Curation curation = Curation.build(attribute, newAttribute);
                    bioSamplesClient.persistCuration(sample.getAccession(),
                            curation, domain);
                    sample = curationApplicationService.applyCurationToSample(sample, curation);
                    return sample;
                }
            }

            //if it is an organism with a single numeric IRI, assume NCBI taxon
            if (attribute.getType().toLowerCase().equals("organism") && attribute.getIri().size() == 1) {
                Integer taxId = null;
                try {
                    taxId = Integer.parseInt(attribute.getIri().first());
                } catch (NumberFormatException e) {
                    taxId = null;
                }
                if (taxId != null) {
                    SortedSet<String> iris = new TreeSet<>();
                    iris.add("http://purl.obolibrary.org/obo/NCBITaxon_" + taxId);
                    //TODO check this IRI exists via OLS

                    Attribute newAttribute = Attribute.build(attribute.getType(), attribute.getValue(),
                            iris, attribute.getUnit());
                    Curation curation = Curation.build(attribute, newAttribute);
                    bioSamplesClient.persistCuration(sample.getAccession(),
                            curation, domain);
                    sample = curationApplicationService.applyCurationToSample(sample, curation);
                    return sample;
                }
            }
        }

        // TODO validate existing ontology terms against OLS

        // TODO turn attributes with biosample accessions into relationships

        // TODO split number+unit attributes

        // TODO lowercase attribute types
        // TODO lowercase relationship types
        return sample;
    }

    public String cleanString(String string) {
        if (string == null) {
            return string;
        }
        // purge all strange characters not-quite-whitespace
        // note, you can find these unicode codes by pasting u"the character"
        // into python
        string = string.replaceAll("\"", "");
        string = string.replaceAll("\n", "");
        string = string.replaceAll("\t", "");
        string = string.replaceAll("\u2011", "-"); // hypen
        string = string.replaceAll("\u2012", "-"); // hypen
        string = string.replaceAll("\u2013", "-"); // hypen
        string = string.replaceAll("\u2014", "-"); // hypen
        string = string.replaceAll("\u2015", "-"); // hypen
        string = string.replaceAll("\u2009", " "); // thin space
        string = string.replaceAll("\u00A0", " "); // non-breaking space
        string = string.replaceAll("\uff09", ") "); // full-width right
        // parenthesis
        string = string.replaceAll("\uff08", " ("); // full-width left
        // parenthesis

        // replace underscores with spaces
        // string = string.replaceAll("_", " ");
        //this is a significant change, so leave it undone for the moment....

        // <br> or <b>
        string = string.replaceAll("\\s*\\</?[bB][rR]? ?/?\\>\\s*", " ");
        // <p>
        string = string.replaceAll("\\s*\\</?[pP] ?/?\\>\\s*", " ");
        // <i>
        string = string.replaceAll("\\s*\\</?[iI] ?/?\\>\\s*", " ");

        // trim extra whitespace at start and end
        string = string.trim();

        // XML/HTML automatically replaces consecutive spaces with single spaces
        // TODO use regex for any series of whitespace characters or equivalent
        while (string.contains("  ")) {
            string = string.replace("  ", " ");
        }

        // some UTF-8 hacks
        // TODO replace with code from Solr UTF-8 plugin
        string = string.replaceAll("ÃƒÂ¼", "ü");

        // also strip UTF-8 control characters that invalidate XML
        // from
        // http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html
        // TODO check for valid UTF-8 but invalid JSON characters
        StringBuffer sb = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.
        for (int i = 0; i < string.length(); i++) {
            current = string.charAt(i); // NOTE: No IndexOutOfBoundsException
            // caught here; it should not happen.
            if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))
                    || ((current >= 0x10000) && (current <= 0x10FFFF))) {
                sb.append(current);
            }
        }
        return sb.toString();
    }

    private static boolean stringContainsItemFromList(String value, String[] items) {
        return Arrays.stream(items).parallel().anyMatch(value::contains);
    }

    public static boolean isNotApplicableSynonym(String string) {
        String lsString = string.toLowerCase().trim();
        return stringContainsItemFromList(lsString, NON_APPLICABLE_SYNONYMS);
    }

    private String correctUnit(String unit) {
        String lcval = unit.toLowerCase();
        if (lcval.equals("alphanumeric") || lcval.equals("na") || lcval.equals("n/a") || lcval.equals("n.a")
                || lcval.equals("censored/uncensored") || lcval.equals("m/f") || lcval.equals("test/control")
                || lcval.equals("yes/no") || lcval.equals("y/n") || lcval.equals("not specified")
                || lcval.equals("not collected") || lcval.equals("not known") || lcval.equals("not reported")
                || lcval.equals("missing"))
        // NOTE -this is for units ONLY
        {
            return null;
        } else if (lcval.equals("meter") || lcval.equals("meters")) {
            return "meter";
        } else if (lcval.equals("cellsperliter") || lcval.equals("cells per liter") || lcval.equals("cellperliter")
                || lcval.equals("cell per liter") || lcval.equals("cellsperlitre") || lcval.equals("cells per litre")
                || lcval.equals("cellperlitre") || lcval.equals("cell per litre")) {
            return "cell per liter";
        } else if (lcval.equals("cellspermilliliter") || lcval.equals("cells per milliliter")
                || lcval.equals("cellpermilliliter") || lcval.equals("cell per milliliter")
                || lcval.equals("cellspermillilitre") || lcval.equals("cells per millilitre")
                || lcval.equals("cellpermillilitre") || lcval.equals("cell per millilitre")) {
            return "cell per millilitre";
        } else if (lcval.equals("micromolesperliter") || lcval.equals("micromoleperliter")
                || lcval.equals("micromole per liter") || lcval.equals("micromoles per liter")
                || lcval.equals("micromolesperlitre") || lcval.equals("micromoleperlitre")
                || lcval.equals("micromole per litre") || lcval.equals("micromoles per litre")) {
            return "micromole per liter";
        } else if (lcval.equals("microgramsperliter") || lcval.equals("microgramperliter")
                || lcval.equals("microgram per liter") || lcval.equals("micrograms per liter")
                || lcval.equals("microgramsperlitre") || lcval.equals("microgramperlitre")
                || lcval.equals("microgram per litre") || lcval.equals("micrograms per litre")) {
            return "microgram per liter";
        } else if (lcval.equals("micromolesperkilogram") || lcval.equals("micromoles per kilogram")
                || lcval.equals("micromoleperkilogram") || lcval.equals("micromole per kilogram")) {
            return "micromole per kilogram";
        } else if (lcval.equals("psu") || lcval.equals("practicalsalinityunit")
                || lcval.equals("practical salinity unit") || lcval.equals("practical salinity units")
                || lcval.equals("pss-78") || lcval.equals("practicalsalinityscale1978 ")) {
            // technically, this is not a unit since its dimensionless..
            return "practical salinity unit";
        } else if (lcval.equals("micromoles") || lcval.equals("micromole")) {
            return "micromole";
        } else if (lcval.equals("decimalhours") || lcval.equals("decimalhour") || lcval.equals("hours")
                || lcval.equals("hour")) {
            return "hour";
        } else if (lcval.equals("day") || lcval.equals("days")) {
            return "day";
        } else if (lcval.equals("week") || lcval.equals("weeks")) {
            return "week";
        } else if (lcval.equals("month") || lcval.equals("months")) {
            return "month";
        } else if (lcval.equals("year") || lcval.equals("years")) {
            return "year";
        } else if (lcval.equals("percentage")) {
            return "percent";
        } else if (lcval.equals("decimal degrees") || lcval.equals("decimal degree") || lcval.equals("decimaldegrees")
                || lcval.equals("decimaldegree")) {
            return "decimal degree";
        } else if (lcval.equals("celcius") || lcval.equals("degree celcius") || lcval.equals("degrees celcius")
                || lcval.equals("degreecelcius") || lcval.equals("degree celsius") || lcval.equals("degrees celsius")
                || lcval.equals("degreecelsius") || lcval.equals("centigrade") || lcval.equals("degree centigrade")
                || lcval.equals("degrees centigrade") || lcval.equals("degreecentigrade") || lcval.equals("c")
                || lcval.equals("??c") || lcval.equals("degree c") || lcval.equals("internationaltemperaturescale1990")
                || lcval.equals("iternationaltemperaturescale1990")) {
            return "Celsius";
        } else {
            // no change
            return unit;
        }
    }

    private Sample ols(Sample sample) {
        for (Attribute attribute : sample.getAttributes()) {
            for (String iri : attribute.getIri()) {
                log.trace("Checking iri " + iri);
                if (iri.matches("^[A-Za-z]+[_:\\-][0-9]+$")) {
                    log.trace("Querying OLS for iri " + iri);
                    Optional<String> iriResult = olsProcessor.queryOlsForShortcode(iri);
                    if (iriResult.isPresent()) {
                        log.trace("Mapped " + iri + " to " + iriResult.get());
                        Attribute mapped = Attribute.build(attribute.getType(), attribute.getValue(), iriResult.get(), null);
                        Curation curation = Curation.build(Collections.singleton(attribute), Collections.singleton(mapped), null, null);

                        //save the curation back in biosamples
                        bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                        sample = curationApplicationService.applyCurationToSample(sample, curation);
                        return sample;
                    }
                }
            }
        }
        return sample;
    }

}
