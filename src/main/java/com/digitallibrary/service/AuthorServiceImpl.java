package com.digitallibrary.service;

import com.digitallibrary.client.OpenLibraryClient;
import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse;
import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.entity.Author;
import com.digitallibrary.entity.Work;
import com.digitallibrary.exception.AuthorNotFoundException;
import com.digitallibrary.repository.AuthorRepository;
import com.digitallibrary.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final WorkRepository workRepository;
    private final OpenLibraryClient openLibraryClient;

    @Override
    @Transactional
    public List<AuthorDto> searchAuthorsByName(String name) {
        log.debug("Searching authors in DB by name: {}", name);

        // 1. Try local database first
        List<Author> localAuthors = authorRepository.findByNameContainingIgnoreCase(name);
        if (!localAuthors.isEmpty()) {
            log.debug("Found {} author(s) in DB for name: {}", localAuthors.size(), name);
            return toAuthorResponses(localAuthors);
        }

        // 2. Fallback to OpenLibrary API
        log.debug("No authors found in DB, querying OpenLibrary API for: {}", name);
        return openLibraryClient.searchAuthors(name)
                .map(this::persistAndMapAuthors)
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional
    public List<WorkDto> getWorksByAuthorId(String authorId) {
        log.debug("Searching works for author: {}", authorId);

        // 1. Find the author in DB
        Author author = authorRepository.findByExternalId(authorId).orElse(null);

        // 2. If author exists and has works cached, return them
        if (author != null) {
            List<Work> localWorks = workRepository.findByAuthorId(author.getId());
            if (!localWorks.isEmpty()) {
                log.debug("Found {} work(s) in DB for author: {}", localWorks.size(), authorId);
                return toWorkResponses(localWorks);
            }
        }

        // 3. Fallback to OpenLibrary API for works
        log.debug("No works found in DB, querying OpenLibrary API for author: {}", authorId);
        return openLibraryClient.getAuthorWorks(authorId)
                .map(response -> persistAndMapWorks(response, authorId, author))
                .orElseThrow(() -> new AuthorNotFoundException(authorId));
    }

    // --- Private helpers ---

    private List<AuthorDto> persistAndMapAuthors(OpenLibraryAuthorSearchResponse response) {
        if (response.getDocs() == null || response.getDocs().isEmpty()) {
            return Collections.emptyList();
        }

        List<Author> authors = response.getDocs().stream()
                .filter(doc -> doc.getKey() != null && doc.getName() != null)
                .filter(doc -> !authorRepository.existsByExternalId(doc.getKey()))
                .map(doc -> Author.builder()
                        .externalId(doc.getKey())
                        .name(doc.getName())
                        .build())
                .collect(Collectors.toList());

        if (!authors.isEmpty()) {
            authors = authorRepository.saveAll(authors);
            log.debug("Persisted {} new author(s) from OpenLibrary", authors.size());
        }

        // Return ALL matching docs (including already existing ones)
        return response.getDocs().stream()
                .filter(doc -> doc.getKey() != null && doc.getName() != null)
                .map(doc -> AuthorDto.builder()
                        .id(doc.getKey())
                        .name(doc.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<WorkDto> persistAndMapWorks(
            OpenLibraryWorksResponse response,
            String authorId,
            Author existingAuthor) {

        if (response.getEntries() == null || response.getEntries().isEmpty()) {
            return Collections.emptyList();
        }

        // Ensure the author exists in DB before linking works
        Author author = existingAuthor;
        if (author == null) {
            author = authorRepository.findByExternalId(authorId)
                    .orElseGet(() -> authorRepository.save(
                            Author.builder()
                                    .externalId(authorId)
                                    .name(authorId)
                                    .build()));
        }

        final Author persistedAuthor = author;
        List<Work> works = response.getEntries().stream()
                .filter(entry -> entry.getKey() != null && entry.getTitle() != null)
                .map(entry -> Work.builder()
                        .externalKey(entry.getKey())
                        .title(entry.getTitle())
                        .author(persistedAuthor)
                        .build())
                .collect(Collectors.toList());

        if (!works.isEmpty()) {
            workRepository.saveAll(works);
            log.debug("Persisted {} work(s) for author: {}", works.size(), authorId);
        }

        return response.getEntries().stream()
                .filter(entry -> entry.getKey() != null && entry.getTitle() != null)
                .map(entry -> WorkDto.builder()
                        .key(entry.getKey())
                        .title(entry.getTitle())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AuthorDto> toAuthorResponses(List<Author> authors) {
        return authors.stream()
                .map(a -> AuthorDto.builder()
                        .id(a.getExternalId())
                        .name(a.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<WorkDto> toWorkResponses(List<Work> works) {
        return works.stream()
                .map(w -> WorkDto.builder()
                        .key(w.getExternalKey())
                        .title(w.getTitle())
                        .build())
                .collect(Collectors.toList());
    }
}
