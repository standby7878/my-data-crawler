package com.mycrawler.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobSearchSite {
    ALL("all"),
    MOL("mol.fi"),
    DUUNITORI("duunitori.fi"),
    OIKOTIE("oikotie.fi"),
    TE_PALVELUT("te-palvelut.fi");

    private final String value;

    JobSearchSite(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static JobSearchSite fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (JobSearchSite site : values()) {
            if (site.value.equalsIgnoreCase(value.trim())) {
                return site;
            }
        }
        throw new IllegalArgumentException("Unknown site: " + value);
    }
}

