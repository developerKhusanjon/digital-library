package com.digitallibrary.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the OpenLibrary individual author details response:
 * GET https://openlibrary.org/authors/{authorKey}.json
 *
 * Example:
 * {
 *   "key": "/authors/OL23919A",
 *   "name": "J. K. Rowling",
 *   "personal_name": "J. K. Rowling",
 *   "birth_date": "31 July 1965"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryAuthorDetailsResponse {

    private String key;

    private String name;

    @JsonProperty("personal_name")
    private String personalName;

    @JsonProperty("birth_date")
    private String birthDate;

    /**
     * Resolves the best human-readable display name for the author.
     * Prefers 'name', falls back to 'personalName'.
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (personalName != null && !personalName.isBlank()) {
            return personalName.trim();
        }
        return null;
    }
}
