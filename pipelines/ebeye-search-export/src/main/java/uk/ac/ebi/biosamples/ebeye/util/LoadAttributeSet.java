package uk.ac.ebi.biosamples.ebeye.util;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.util.*;

@Service
public class LoadAttributeSet {
    private Set<String> attributeSet = new HashSet<>();

    public Set<String> getAllAttributes() throws FileNotFoundException {
        final Scanner scanner = new Scanner(ResourceUtils.getFile("classpath:most_common_attributes.list"));
        final List<String> list = new ArrayList<>();

        while (scanner.hasNextLine()){
            list.add(scanner.nextLine());
        }

        scanner.close();

        list.forEach(element -> {
            attributeSet.add(Arrays.asList(element.split(",")).get(0));
        });

        return attributeSet;
    }
}
