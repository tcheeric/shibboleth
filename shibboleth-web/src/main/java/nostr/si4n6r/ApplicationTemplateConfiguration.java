package nostr.si4n6r;

import lombok.Data;
import nostr.si4n6r.core.impl.ApplicationProxy;

import java.io.IOException;
import java.io.InputStream;

@Data
public class ApplicationTemplateConfiguration {
    private ApplicationProxy.ApplicationTemplate template;
    private static ApplicationTemplateConfiguration instance;

    private ApplicationTemplateConfiguration() {
        loadTemplate();
    }

    public static ApplicationTemplateConfiguration getInstance() {
        if (instance == null) {
            instance = new ApplicationTemplateConfiguration();
        }
        return instance;
    }

    private void loadTemplate() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("cyclops.json")) {
            if (input == null) {
                System.out.println("Sorry, unable to find cyclops.json");
                return;
            }

            // Read the content of the file into a string
            byte[] bytes = input.readAllBytes();
            this.template = ApplicationProxy.ApplicationTemplate.fromJsonString(new String(bytes));

        } catch (IOException ex) {
            throw new RuntimeException("Unable to load cyclops.json", ex);
        }
    }

}
