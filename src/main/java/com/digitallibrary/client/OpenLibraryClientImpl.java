package com.digitallibrary.client;

import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class OpenLibraryClientImpl implements OpenLibraryClient {

    private final RestClient restClient;

    public OpenLibraryClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "searchAuthorsFallback")
    @Retry(name = "openLibrary")
    public Optional<OpenLibraryAuthorSearchResponse> searchAuthors(String query) {
        log.debug("Calling OpenLibrary search authors API for query: {}", query);

        OpenLibraryAuthorSearchResponse response = restClient.get()
                .uri("/search/authors.json?q={query}", query)
                .retrieve()
                .body(OpenLibraryAuthorSearchResponse.class);

        return Optional.ofNullable(response);
    }

    @Override
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "getAuthorWorksFallback")
    @Retry(name = "openLibrary")
    public Optional<OpenLibraryWorksResponse> getAuthorWorks(String authorId) {
        log.debug("Calling OpenLibrary works API for author: {}", authorId);

        OpenLibraryWorksResponse response = restClient.get()
                .uri("/authors/{authorId}/works.json?limit=1000", authorId)
                .retrieve()
                .body(OpenLibraryWorksResponse.class);

        return Optional.ofNullable(response);
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
}
