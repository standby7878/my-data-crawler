package com.mycrawler.orchestrator.run;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class BatchScanner {
    public List<Path> scanHtmlFiles(Path inputDir) throws IOException {
        try (Stream<Path> stream = Files.walk(inputDir)) {
            return stream
                    .filter(path -> path.toString().endsWith(".html"))
                    .sorted()
                    .toList();
        }
    }
}
