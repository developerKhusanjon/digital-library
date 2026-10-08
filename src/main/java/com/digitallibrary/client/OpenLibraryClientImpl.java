package com.digitallibrary.client;

import com.digitallibrary.client.dto.OpenLibraryAuthorDetailsResponse;
import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class OpenLibraryClientImpl implements OpenLibraryClient {

    private final RestClient restClient;

    public OpenLibraryClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Normalizes an author identifier key by removing URL prefixes ("/authors/"),
     * human-readable slugs (e.g. "/J._K._Rowling"), and trailing ".json" extensions.
     *
     * Examples:
     * - "OL23919A" -> "OL23919A"
     * - "/authors/OL23919A" -> "OL23919A"
     * - "OL23919A/J._K._Rowling" -> "OL23919A"
     * - "/authors/OL23919A/J._K._Rowling" -> "OL23919A"
     * - "OL23919A.json" -> "OL23919A"
     *
     * @param authorId raw author key or path
     * @return clean OpenLibrary author key (e.g. "OL23919A")
     */
    public static String normalizeAuthorKey(String authorId) {
        if (authorId == null || authorId.isBlank()) {
            return "";
        }
        String cleaned = authorId.trim();

        // Strip OpenLibrary full URL or path prefix if present
        if (cleaned.contains("/authors/")) {
            cleaned = cleaned.substring(cleaned.indexOf("/authors/") + "/authors/".length());
        } else if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        // Strip trailing .json if caller appended it
        if (cleaned.endsWith(".json")) {
            cleaned = cleaned.substring(0, cleaned.length() - 5);
        }

        // Strip human-readable slug after slash (e.g. "OL23919A/J._K._Rowling" -> "OL23919A")
        int slashIdx = cleaned.indexOf('/');
        if (slashIdx != -1) {
            cleaned = cleaned.substring(0, slashIdx);
        }

        return cleaned;
    }

    @Override
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "searchAuthorsFallback")
    @Retry(name = "openLibrary")
    public Optional<OpenLibraryAuthorSearchResponse> searchAuthors(String query) {
        log.debug("Calling OpenLibrary search authors API for query: {}", query);

        try {
            OpenLibraryAuthorSearchResponse response = restClient.get()
                    .uri("/search/authors.json?q={query}", query)
                    .retrieve()
                    .body(OpenLibraryAuthorSearchResponse.class);

            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No author search results found (404) for query: {}", query);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "getAuthorWorksFallback")
    @Retry(name = "openLibrary")
    public Optional<OpenLibraryWorksResponse> getAuthorWorks(String authorId) {
        String cleanKey = normalizeAuthorKey(authorId);
        log.debug("Calling OpenLibrary works API for author: {}", cleanKey);

        try {
            OpenLibraryWorksResponse response = restClient.get()
                    .uri("/authors/{authorId}/works.json?limit=1000", cleanKey)
                    .retrieve()
                    .body(OpenLibraryWorksResponse.class);

            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Author works not found in OpenLibrary (404) for key: {}", cleanKey);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "getAuthorDetailsFallback")
    @Retry(name = "openLibrary")
    public Optional<OpenLibraryAuthorDetailsResponse> getAuthorDetails(String authorId) {
        String cleanKey = normalizeAuthorKey(authorId);
        log.debug("Calling OpenLibrary author details API for author: {}", cleanKey);

        try {
            OpenLibraryAuthorDetailsResponse response = restClient.get()
                    .uri("/authors/{authorId}.json", cleanKey)
                    .retrieve()
                    .body(OpenLibraryAuthorDetailsResponse.class);

            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Author details not found in OpenLibrary (404) for key: {}", cleanKey);
            return Optional.empty();
        }
    }

    // --- Fallback methods (invoked when circuit is open or all retries exhausted) ---

    @SuppressWarnings("unused")
    private Optional<OpenLibraryAuthorSearchResponse> searchAuthorsFallback(String query, Throwable t) {
        log.warn("OpenLibrary author search unavailable for query '{}'. Reason: {}", query, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<OpenLibraryWorksResponse> getAuthorWorksFallback(String authorId, Throwable t) {
        log.warn("OpenLibrary works API unavailable for author '{}'. Reason: {}", authorId, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<OpenLibraryAuthorDetailsResponse> getAuthorDetailsFallback(String authorId, Throwable t) {
        log.warn("OpenLibrary author details API unavailable for author '{}'. Reason: {}", authorId, t.getMessage());
        return Optional.empty();
    }
}
