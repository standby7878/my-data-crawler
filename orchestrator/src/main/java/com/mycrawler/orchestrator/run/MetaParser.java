package com.mycrawler.orchestrator.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MetaParser {
    private final ObjectMapper objectMapper;

    public MetaParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MetaData parse(Path metaPath) throws IOException {
        if (!Files.exists(metaPath)) {
            return new MetaData(null, null);
        }
        Map<String, Object> data = objectMapper.readValue(metaPath.toFile(), Map.class);
        String url = data.get("url") != null ? data.get("url").toString() : null;
        String company = data.get("company") != null ? data.get("company").toString() : null;
        return new MetaData(url, company);
    }
}
