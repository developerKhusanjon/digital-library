package com.digitallibrary.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Maps the OpenLibrary author search response:
 * GET https://openlibrary.org/search/authors.json?q={query}
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryAuthorSearchResponse {

    private int numFound;
    private List<AuthorDoc> docs = Collections.emptyList();

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorDoc {
        private String key;   // e.g. "OL23919A"
        private String name;  // e.g. "J. K. Rowling"
    }
}
