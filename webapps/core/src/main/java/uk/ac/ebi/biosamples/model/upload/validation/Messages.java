package uk.ac.ebi.biosamples.model.upload.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Messages {
    private List<String> messagesList = new ArrayList<>();

    public void addMessage(String message) {
        messagesList.add(message);
    }

    public List<String> getMessagesList() {
        return messagesList;
    }

    public void clear() {
        messagesList.clear();
    }
}
