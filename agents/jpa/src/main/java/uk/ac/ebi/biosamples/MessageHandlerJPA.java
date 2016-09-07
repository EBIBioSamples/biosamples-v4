package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.models.JPAAttribute;
import uk.ac.ebi.biosamples.models.JPASample;
import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.repos.JPAAttributeRepository;
import uk.ac.ebi.biosamples.repos.JPASampleRepository;

@Service
public class MessageHandlerJPA {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private JPASampleRepository jpaSampleRepository;

	@Autowired
	private JPAAttributeRepository jpaAttributeRepository;

	@RabbitListener(queues = Messaging.queueToBeLoaded)
	@Transactional
	public void handle(SimpleSample sample) {
		log.info("Recieved " + sample.getAccession());
		// convert it to the right object type
		JPASample jpaSample = JPASample.createFrom(sample);

		JPASample oldSample = jpaSampleRepository.findByAccession(jpaSample.getAccession());
		if (oldSample != null) {
			jpaSample.setId(oldSample.getId());
			log.info("Updating sample " + sample.getAccession() + " (" + oldSample.getId() + ")");
		} else {
			// save the new one
			log.info("Saving sample " + sample.getAccession());
		}

		// for each attribute, get lowest existing ids (if any)
		for (JPAAttribute attribute : jpaSample.getAttributes()) {

			String type = attribute.getKey();
			String value = attribute.getValue();
			//trim value to maximum size
			if (value.length() >= 255) {
				log.warn("attribute to long for sample "+sample.getAccession());
				value = value.substring(0, 255)+"...";
				attribute.setValue(value);
			}
			String unit = attribute.getUnit();
			String ontologyTerm = attribute.getOntologyTerm();

			Long oldAttributeId = null;

			// TODO check old sample if present

			Pageable pageable = new PageRequest(0,10);
			Iterable<JPAAttribute> oldAttributes = jpaAttributeRepository.findByKeyAndValueAndUnitAndOntologyTerm(type,
					value, unit, ontologyTerm);

			for (JPAAttribute oldAttribute : oldAttributes) {
				if (oldAttributeId == null || oldAttribute.getId() < oldAttributeId) {
					oldAttributeId = oldAttribute.getId();
				}
			}

			if (oldAttributeId != null) {
				log.trace("Got old attribute (" + oldAttributeId + ") for " + type + " " + value + " " + unit + " "
						+ ontologyTerm);
				attribute.setId(oldAttributeId);
			}
			jpaAttributeRepository.save(attribute);
		}

		// TODO relationships

		jpaSample = jpaSampleRepository.save(jpaSample);
	}
}
