/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;

/**
 * Class representing a phase of the integration test: Phase 1 - data put into the queues, ready to
 * be processed Phase 2 - data available in biosamples database and readable from interfaces (api,
 * html,...)
 */
public enum Phase {
  UNKNOWN(-1),
  NO_PHASE(0),
  ONE(1),
  TWO(2),
  THREE(3),
  FOUR(4),
  FIVE(5),
  SIX(6);

  private static Logger log = LoggerFactory.getLogger(Phase.class);

  private static final Map<Integer, Phase> phaseLookup = new HashMap<>();

  static {
    for (Phase s : EnumSet.allOf(Phase.class))
      switch (s) {
        case UNKNOWN:
        case NO_PHASE:
          continue;
        default:
          phaseLookup.put(s.getCode(), s);
      }
  }

  private int phaseCode;

  Phase(int phaseRepresentation) {
    this.phaseCode = phaseRepresentation;
  }

  public int getCode() {
    return this.phaseCode;
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
