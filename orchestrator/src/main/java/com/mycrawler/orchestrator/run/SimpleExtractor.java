package com.mycrawler.orchestrator.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimpleExtractor {
    public JobPosting extract(PreprocessedDocument doc, MetaData meta) {
        JobPosting posting = new JobPosting();
        String sourceKey = (doc.title() == null ? "" : doc.title()) + "|" + (meta.url() == null ? "" : meta.url());
        posting.setJobId(UUID.nameUUIDFromBytes(sourceKey.getBytes()).toString());
        posting.setJobTitle(blankToNull(doc.title()));
        posting.setCompanyName(blankToNull(meta.company()));
        posting.setCompanyIndustry(null);
        posting.setJobDescriptionSummary(summarize(doc.text()));
        posting.setLocationMunicipality("Uusimaa");
        posting.setLocationSpecific(null);
        posting.setEmploymentDuration(null);
        posting.setWorkingHours(null);
        posting.setApplicationDeadline(null);
        posting.setApplicationMethod(null);
        posting.setContactEmail(null);
        posting.setContactPhone(null);
        posting.setContactPersonName(null);
        posting.setContactPersonTitle(null);
        posting.setRequiredSkills(new ArrayList<>());
        posting.setPreferredSkills(new ArrayList<>());
        posting.setEducationRequirements(null);
        posting.setLanguageRequirements(null);
        posting.setSalaryInfo(null);
        posting.setBenefits(new ArrayList<>());
        posting.setApplicationInstructions(null);
        posting.setSourceUrl(blankToNull(meta.url()));
        posting.setSourceLanguage("unknown");
        posting.setExtractionNotes("heuristic-baseline");
        posting.setRawFields(new HashMap<>());
        posting.setExtractionConfidence(scoreConfidence(posting));
        return posting;
    }

    private String summarize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.substring(0, Math.min(text.length(), 600));
    }

    private String scoreConfidence(JobPosting posting) {
        int score = 0;
        score += posting.getJobTitle() != null ? 1 : 0;
        score += posting.getCompanyName() != null ? 1 : 0;
        score += posting.getJobDescriptionSummary() != null ? 1 : 0;
        score += posting.getSourceUrl() != null ? 1 : 0;
        if (score >= 4) {
            return "high";
        }
        if (score >= 2) {
            return "medium";
        }
        return "low";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
