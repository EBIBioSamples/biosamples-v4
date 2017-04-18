package uk.ac.ebi.biosamples;

import org.springframework.boot.ApplicationArguments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing a phase of the integration test:
 * Phase 1 - data put into the queues, ready to be processed
 * Phase 2 - data available in biosamples database and readable from interfaces (api, html,...)
 */
public enum Phase {
    ONE(1, "Input"),
    TWO(2, "Read"),
    UNKNOWN(-1, "Unknown"),
    NO_PHASE(0, "No phase");

    static final Map<Integer, Phase> phaseLookup = new HashMap<>();

    static {
        for(Phase s : EnumSet.allOf(Phase.class))
            switch (s) {
                case UNKNOWN:
                case NO_PHASE:
                    continue;
                default:
                    phaseLookup.put(s.getCode(), s);
            }

    }

    private int phaseCode;
    private String description;


    Phase(int phaseRepresentation, String description) {
        this.phaseCode = phaseRepresentation;
        this.description = description;
    }

    public int getCode() {
        return this.phaseCode;
    }

    public String getDescription() {
        return description;
    }

    public static Phase readPhaseFromArguments(ApplicationArguments args) {
        if (args.containsOption("phase")) {
            int phaseCode = Integer.parseInt(args.getOptionValues("phase").get(0));
            Phase phase = phaseLookup.getOrDefault(phaseCode, Phase.UNKNOWN);
            if (phase.equals(UNKNOWN)) {
                throw new IllegalArgumentException(String.format("Unknown phase %d", phaseCode));
            } else {
                return phase;
            }
        }
        return NO_PHASE;
    }
}
