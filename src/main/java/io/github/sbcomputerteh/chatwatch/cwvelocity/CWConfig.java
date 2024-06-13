package io.github.sbcomputerteh.chatwatch.cwvelocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CWConfig {
    public static CWConfig load(Path dataDir) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        Path configPath = dataDir.resolve("config.json");

        if(!configPath.toFile().exists()) {
            CWConfig defaultConfig = new CWConfig();
            try {
                Files.writeString(configPath, gson.toJson(defaultConfig));
                return defaultConfig;
            } catch (IOException e) {
                return defaultConfig;
            }
        }

        String configJson;
        try {
            configJson = Files.readString(configPath);
        } catch (IOException e) {
            return new CWConfig();
        }
        return gson.fromJson(configJson, CWConfig.class);
    }

    private CWConfig() {
        serverAddress = "127.0.0.1";
        serverPort = 8080;
        ingestToken = new UUID(0, 0);
        messageCommands = List.of("msg", "tell", "w");
    }

    public String serverAddress;
    public int serverPort;
    public UUID ingestToken;
    public List<String> messageCommands;
}
