package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.controller.SampleTabV1Controller.SampleTabRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SampleTabMultipartFileConverter implements Converter<MultipartFile, SampleTabRequest> {
    @Override
    public SampleTabRequest convert(MultipartFile multipartFile) {
        List<List<String>> fileContent = new LinkedList<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                List<String> elements = Stream.of(line.split("\t"))
                        .map(String::trim)
                        .map(this::cleanFromEscapedCharacters)
                        .collect(Collectors.toList());
                fileContent.add(elements);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SampleTabRequest(fileContent);

    }

    private String cleanFromEscapedCharacters(String string) {
        String resultString =  string.replace("\"","").replace("\\\\", "\\");
        return resultString;
    }
}
