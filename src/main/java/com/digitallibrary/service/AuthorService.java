package com.digitallibrary.service;

import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.dto.WorkDto;

import java.util.List;

public interface AuthorService {

    /**
     * Search authors by name. Falls back to OpenLibrary API if not found locally.
     *
     * @param name the author name to search
     * @return list of matching authors
     */
    List<AuthorDto> searchAuthorsByName(String name);

    /**
     * Get works for an author by their OpenLibrary external ID.
     * Falls back to OpenLibrary API if works are not cached locally.
     *
     * @param authorId the OpenLibrary author key (e.g. "OL23919A")
     * @return list of works by the author
     */
    List<WorkDto> getWorksByAuthorId(String authorId);
}
