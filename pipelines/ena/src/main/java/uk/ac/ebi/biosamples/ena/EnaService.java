package uk.ac.ebi.biosamples.ena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EnaService {

    public Collection<String> splitIdentifiers(String input) {
    	//split by commas
        List<String> idents = new ArrayList<String>();
        if (input.contains(",")) {
            for (String substr : input.split(",")) {
                idents.add(substr);
            }
        } else {
            idents.add(input);
        }

        //convert hyphenated ranges into separate numbers
        List<String> newidents = new ArrayList<String>();
        for (String ident : idents) {
            if (ident.contains("-")) {
                // its a range
                String[] range = ident.split("-");
                int lower = new Integer(range[0].substring(3));
                int upper = new Integer(range[1].substring(3));
                String prefix = range[0].substring(0, 3);
                for (int i = lower; i <= upper; i++) {
                    newidents.add(String.format(prefix + "%06d", i));
                }
            } else {
                newidents.add(ident);
            }
        }
        
        //sort it before returning
        Collections.sort(newidents);

        return newidents;
    }
}
