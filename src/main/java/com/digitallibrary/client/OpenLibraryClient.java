package com.digitallibrary.client;

import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse;

import java.util.Optional;

/**
 * Client interface for the OpenLibrary Authors API.
 */
public interface OpenLibraryClient {

    /**
     * Search authors by name query.
     *
     * @param query the author name to search
     * @return search response or empty if the external API is unavailable
     */
    Optional<OpenLibraryAuthorSearchResponse> searchAuthors(String query);

    /**
     * Fetch works for a given author.
     *
     * @param authorId the OpenLibrary author key (e.g. "OL23919A")
     * @return works response or empty if the external API is unavailable
     */
    Optional<OpenLibraryWorksResponse> getAuthorWorks(String authorId);
}
