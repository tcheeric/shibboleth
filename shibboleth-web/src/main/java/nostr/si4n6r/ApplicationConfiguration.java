package nostr.si4n6r;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
public class ApplicationConfiguration {

    private Properties properties;
    private static ApplicationConfiguration instance;

    private ApplicationConfiguration() {
        this.properties = new Properties();
        loadProperties();
    }

    public static ApplicationConfiguration getInstance() {
        if (instance == null) {
            instance = new ApplicationConfiguration();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return;
            }

            // load a properties file from class path
            properties.load(input);

        } catch (IOException ex) {
            throw new RuntimeException("Unable to load application.properties", ex);
        }
    }

    public String getAppNsec() {
        return getProperty("app.nsec");
    }

    public String getAppNpub() {
        return getProperty("app.npub");
    }

    public String getSignerNpub() {
        return getProperty("signer.npub");
    }

    public String getBottinServerBaseUrl() {
        var server = getProperty("bottin.server");
        var port = getProperty("bottin.port");
        return "http://" + server + ":" + port + "/bottin";
    }

    public String getNip05Url() {
        var server = getProperty("nip05.server");
        var port = getProperty("nip05.port");
        return "http://" + server + ":" + port + "/nip05";
    }

    public String getTokenAlgoSecret() {
        return getProperty("token.secret");
    }

    public int getTokenExpiration() {
        return Integer.parseInt(getProperty("token.expiration"));
    }

    private String getProperty(String key) {
        return properties.getProperty(key);
    }

}