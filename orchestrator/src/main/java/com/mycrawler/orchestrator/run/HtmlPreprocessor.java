package com.mycrawler.orchestrator.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HtmlPreprocessor {
    public PreprocessedDocument preprocess(Path htmlPath) throws IOException {
        Document document = Jsoup.parse(htmlPath.toFile(), "UTF-8");
        String title = document.title();
        if (title == null || title.isBlank()) {
            title = document.selectFirst("h1") != null ? document.selectFirst("h1").text() : "";
        }
        String text = document.body() != null ? document.body().text() : "";
        text = text == null ? "" : text.trim();
        Map<String, String> snippets = new HashMap<>();
        if (!text.isBlank()) {
            snippets.put("preview", text.substring(0, Math.min(text.length(), 600)));
        }
        return new PreprocessedDocument(title, text, snippets);
    }
}
