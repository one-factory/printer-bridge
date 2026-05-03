package tigerworkshop.webapphardwarebridge.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Log4j2
public class ConfigService {
    @Getter
    private static final ConfigService instance = new ConfigService();

    private static final String CONFIG_FILENAME = "config.json";
    private static final String CONFIG_DIR_NAME = "OneFactoryPrinterBridge";
    private static final String PRINTER_PLACEHOLDER = "";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Getter
    private Config config = new Config();

    @Getter
    private final Path configPath = buildConfigPath();

    private ConfigService() {
        try {
            loadFromFile(configPath);
        } catch (Exception e) {
            log.warn("Failed loading config, creating new file");
            try {
                save();
            } catch (IOException ex) {
                log.error("Failed to create config file", ex);
            }
        }
    }

    public void loadFromJson(String json) throws JsonProcessingException {
        log.info("Loading config from JSON: {}", json);
        config = objectMapper.readValue(json, Config.class);
    }

    public void loadFromFile(Path filePath) throws IOException {
        log.info("Loading config from file: {}", filePath.toAbsolutePath());
        config = objectMapper.readValue(filePath.toFile(), Config.class);
    }

    public void save() throws IOException {
        Files.createDirectories(configPath.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
        log.info("Saved config file to: {}", configPath.toAbsolutePath());
    }

    public void addPrintTypeToList(String printType) {
        config.getPrinter().getMappings().add(new Config.PrinterMapping(printType, PRINTER_PLACEHOLDER, false, true, 0));
        try {
            save();
        } catch (IOException e) {
            log.error("Failed to save config file after adding print type", e);
        }
    }

    private static Path buildConfigPath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");

        if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", CONFIG_DIR_NAME, CONFIG_FILENAME);
        }

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, CONFIG_DIR_NAME, CONFIG_FILENAME);
            }
            return Paths.get(userHome, "AppData", "Roaming", CONFIG_DIR_NAME, CONFIG_FILENAME);
        }

        return Paths.get(userHome, ".config", CONFIG_DIR_NAME, CONFIG_FILENAME);
    }
}
