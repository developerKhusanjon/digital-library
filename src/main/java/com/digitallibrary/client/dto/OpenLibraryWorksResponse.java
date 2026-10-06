package com.digitallibrary.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Maps the OpenLibrary author works response:
 * GET https://openlibrary.org/authors/{id}/works.json
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryWorksResponse {

    private int size;
    private List<WorkEntry> entries = Collections.emptyList();

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkEntry {
        private String key;    // e.g. "/works/OL34974873W"
        private String title;  // e.g. "Harry Potter and the Philosopher's Stone"
    }
}
