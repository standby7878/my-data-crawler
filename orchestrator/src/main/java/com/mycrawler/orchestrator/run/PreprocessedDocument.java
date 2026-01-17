package com.mycrawler.orchestrator.run;

import java.util.Map;

public record PreprocessedDocument(String title, String text, Map<String, String> snippets) {
}
