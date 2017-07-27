package uk.ac.ebi.biosamples;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CurationApplicationService;
import uk.ac.ebi.biosamples.zooma.ZoomaProcessor;

public class SampleCurationCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Sample sample;
	private final BioSamplesClient bioSamplesClient;
	private final ZoomaProcessor zoomaProcessor;
	private final CurationApplicationService curationApplicationService;

	public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();

	public SampleCurationCallable(BioSamplesClient bioSamplesClient, Sample sample, 
			ZoomaProcessor zoomaProcessor, CurationApplicationService curationApplicationService) {
		this.bioSamplesClient = bioSamplesClient;
		this.sample = sample;
		this.zoomaProcessor = zoomaProcessor;
		this.curationApplicationService = curationApplicationService;
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
			
		} catch (Exception e) {
			failedQueue.add(sample.getAccession());
		}
		return null;
	}
	
	public Sample curate(Sample sample) {
		for (Attribute attribute : sample.getAttributes()) {
			// clean unexpected characters
			String newType = cleanString(attribute.getType());
			String newValue = cleanString(attribute.getValue());
			if (!attribute.getType().equals(newType) || !attribute.getValue().equals(newValue)) {
				Attribute newAttribute = Attribute.build(newType, newValue, attribute.getIri(),
						attribute.getUnit());
				Curation curation = Curation.build(attribute, newAttribute);
				bioSamplesClient.persistCuration(sample.getAccession(), curation);
				sample = curationApplicationService.applyCurationToSample(sample, curation);
				return sample;

			}

			// if no information content, remove
			if (isEqualToNotApplicable(attribute.getValue())) {
				Curation curation = Curation.build(attribute, null);
				bioSamplesClient.persistCuration(sample.getAccession(), curation);
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
							curation);
					sample = curationApplicationService.applyCurationToSample(sample, curation);
					return sample;
				}
			}
		}

		// TODO write me

		// query un-mapped attributes against Zooma

		
		
		
		
		// validate existing ontology terms against OLS
		// expand short-version ontology terms using OLS

		// turn attributes with biosample accessions into relationships

		// split number+unit attributes

		// lowercase attribute types
		// lowercase relationship types
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
		string = string.replaceAll("_", " ");

		// trim extra whitespace at start and end
		string = string.trim();

		// XML/HTML automatically replaces consecutive spaces with single spaces
		// TODO use regex for any series of whitespace characters or equivalent
		while (string.contains("  ")) {
			string = string.replace("  ", " ");
		}

		// <br> or <b>
		string = string.replaceAll("\\s*\\</?[bB][rR]? ?/?\\>\\s*", " ");
		// <p>
		string = string.replaceAll("\\s*\\</?[pP] ?/?\\>\\s*", " ");
		// <i>
		string = string.replaceAll("\\s*\\</?[iI] ?/?\\>\\s*", " ");

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

	private boolean isEqualToNotApplicable(String string) {
		String lsString = string.toLowerCase().trim();
		// remove not applicables
		if (lsString.equals("n/a") || lsString.equals("na") || lsString.equals("n.a") || lsString.equals("none")
				|| lsString.equals("unknown") || lsString.equals("--") || lsString.equals(".")
				|| lsString.equals("null") || lsString.equals("missing") || lsString.equals("[not reported]")
				|| lsString.equals("[not requested]") || lsString.equals("not applicable")
				|| lsString.equals("not collected") || lsString.equals("not specified") || lsString.equals("not known")
				|| lsString.equals("not reported")) {
			// leave unknown-sex as is. implies it has been looked at and is
			// non-determinate
			return true;
		} else {
			return false;
		}
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
				|| lcval.equals("degreecelcius") || lcval.equals("centigrade") || lcval.equals("degree centigrade")
				|| lcval.equals("degrees centigrade") || lcval.equals("degreecentigrade") || lcval.equals("c")
				|| lcval.equals("??c") || lcval.equals("degree c") || lcval.equals("internationaltemperaturescale1990")
				|| lcval.equals("iternationaltemperaturescale1990")) {
			return "Celcius";
		} else {
			// no change
			return unit;
		}
	}
	

	private void zooma(Sample sample) {
		
		for (Attribute attribute : sample.getAttributes()) {
			
			if (attribute.getIri() != null) {
				continue;
			} 
			if (attribute.getUnit() != null) {
				continue;
			} 
			
			if (attribute.getType().equals("synonym")) {
				log.info("Skipping synonym "+attribute.getValue());
				continue;
			} 
			if (attribute.getType().equals("label")) {
				log.info("Skipping label "+attribute.getValue());
				continue;
			}
			if (attribute.getType().equals("host_subject_id")) {
				log.info("Skipping host_subject_id "+attribute.getValue());
				continue;
			}

			
			if (attribute.getValue().matches("^[0-9.-]+$")) {
				log.info("Skipping number "+attribute.getValue());
				continue;
			}
			if (attribute.getValue().matches("^[ESD]R[SRX][0-9]+$")) {
				log.info("Skipping SRA/ENA/DDBJ identifier "+attribute.getValue());
				continue;
			} 
			if (attribute.getValue().matches("^GSM[0-9]+$")) {
				log.info("Skipping GEO identifier "+attribute.getValue());
				continue;
			} 
			if (attribute.getValue().matches("^SAM[END]A?[0-9]+$")) {
				log.info("Skipping BioSample identifier "+attribute.getValue());
				continue;
			}
			
			
			if (attribute.getIri() == null && attribute.getType().length() < 64 && attribute.getValue().length() < 128) {
				Optional<String> iri = zoomaProcessor.queryZooma(attribute.getType(), attribute.getValue());
				if (iri.isPresent()) {
					log.info("Mapped "+attribute+" to "+iri.get());
					Attribute mapped = Attribute.build(attribute.getType(), attribute.getValue(), iri.get(), null);
					Curation curation = Curation.build(Collections.singleton(attribute), Collections.singleton(mapped), null, null);
				
					//save the curation back in biosamples
					bioSamplesClient.persistCuration(sample.getAccession(), curation);
				}
			}
		}
	}

}
