package uk.ac.ebi.biosamples;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;

public class LegacyJsonRunner implements ApplicationRunner, ExitCodeGenerator, Ordered{
    @Override
    public void run(ApplicationArguments args) throws Exception {
        switch (Phase.readPhaseFromArguments(args)) {
            case ONE:
                phaseOne();
                break;
            case TWO:
                phaseTwo();
                break;
            default:

        }
    }


    private void phaseOne() {

    }

    private void phaseTwo() {

    }

    @Override
    public int getExitCode() {
        return 0;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
