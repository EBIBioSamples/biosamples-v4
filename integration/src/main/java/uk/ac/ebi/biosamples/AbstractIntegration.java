package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;

import uk.ac.ebi.biosamples.client.BioSamplesClient;

public abstract class AbstractIntegration implements ApplicationRunner, ExitCodeGenerator {
	
	protected abstract void phaseOne();
	protected abstract void phaseTwo();
	protected abstract void phaseThree();
	protected abstract void phaseFour();
	protected abstract void phaseFive();


	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	protected int exitCode = 1;

	protected final BioSamplesClient client;
	
	public AbstractIntegration(BioSamplesClient client) {
		this.client = client;
	}
	
	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Phase phase = Phase.readPhaseFromArguments(args);

		if (phase == Phase.ONE) {
			log.info("Starting phase one");
			phaseOne();
			log.info("Finished phase one");
		}
		if (phase == Phase.TWO) {
			log.info("Starting phase two");
			phaseTwo();
			log.info("Finished phase two");
		}
		if (phase == Phase.THREE) {
			log.info("Starting phase three");
			phaseThree();
			log.info("Finished phase three");
		}
		if (phase == Phase.FOUR) {
			log.info("Starting phase four");
			phaseFour();
			log.info("Finished phase four");
		}
		if (phase == Phase.FIVE) {
			log.info("Starting phase five");
			phaseFive();
			log.info("Finished phase five");
		}	
		
		close();
		
		exitCode = 0;
	}
	
	public void close() {
		//do nothing
	}

}
