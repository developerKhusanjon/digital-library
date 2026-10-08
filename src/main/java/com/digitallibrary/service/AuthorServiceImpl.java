package com.digitallibrary.service;

import com.digitallibrary.client.OpenLibraryClient;
import com.digitallibrary.client.OpenLibraryClientImpl;
import com.digitallibrary.client.dto.OpenLibraryAuthorDetailsResponse;
import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse;
import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse.AuthorDoc;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse.WorkEntry;
import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.entity.Author;
import com.digitallibrary.entity.Work;
import com.digitallibrary.exception.AuthorNotFoundException;
import com.digitallibrary.mapper.AuthorMapper;
import com.digitallibrary.mapper.WorkMapper;
import com.digitallibrary.repository.AuthorRepository;
import com.digitallibrary.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final WorkRepository workRepository;
    private final OpenLibraryClient openLibraryClient;
    private final AuthorMapper authorMapper;
    private final WorkMapper workMapper;

    @Override
    @Transactional
    public List<AuthorDto> searchAuthorsByName(String name) {
        if (name == null || name.isBlank()) {
            return Collections.emptyList();
        }

        String query = name.trim();
        log.debug("Searching authors in DB by name: {}", query);

        List<Author> localAuthors = authorRepository.findByNameContainingIgnoreCase(query);
        if (!localAuthors.isEmpty()) {
            log.debug("Found {} author(s) in DB for name: {}", localAuthors.size(), query);
            return authorMapper.toDtoList(localAuthors);
        }

        log.debug("No authors in DB, querying OpenLibrary API for: {}", query);
        return openLibraryClient.searchAuthors(query)
                .map(this::persistAndMapAuthors)
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional
    public List<WorkDto> getWorksByAuthorId(String authorId) {
        if (authorId == null || authorId.isBlank()) {
            throw new AuthorNotFoundException(authorId);
        }

        log.debug("Searching works for author identifier: {}", authorId);

        // 1. Check if identifier is internal numeric DB ID
        Author author = findAuthorByNumericId(authorId.trim());

        // 2. If not found by DB numeric ID, check by normalized external OpenLibrary key
        String cleanKey = OpenLibraryClientImpl.normalizeAuthorKey(authorId);
        if (author == null && !cleanKey.isBlank()) {
            author = authorRepository.findByExternalId(cleanKey).orElse(null);
        }

        // 3. Author exists in DB
        if (author != null) {
            List<Work> localWorks = workRepository.findByAuthorId(author.getId());
            if (!localWorks.isEmpty()) {
                log.debug("Found {} work(s) in DB for author: {}", localWorks.size(), author.getExternalId());
                return workMapper.toDtoList(localWorks);
            }

            // Author exists locally, but works are not cached yet -> query OpenLibrary works API
            log.debug("Author found in DB, but works not cached. Querying OpenLibrary for: {}", author.getExternalId());
            final Author existingAuthor = author;
            return openLibraryClient.getAuthorWorks(author.getExternalId())
                    .map(response -> persistAndMapWorks(response, existingAuthor))
                    .orElse(Collections.emptyList());
        }

        // 4. Author not in DB -> search on OpenLibrary API side
        log.debug("Author not in DB, querying OpenLibrary API for author: {}", cleanKey);
        return fetchAuthorAndWorksFromOpenLibrary(cleanKey, authorId);
    }

    // --- Private helpers ---

    private Author findAuthorByNumericId(String input) {
        try {
            Long id = Long.parseLong(input);
            return authorRepository.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<WorkDto> fetchAuthorAndWorksFromOpenLibrary(String cleanKey, String originalInput) {
        // Query author details to resolve real author name and verify existence
        Optional<OpenLibraryAuthorDetailsResponse> authorDetailsOpt = openLibraryClient.getAuthorDetails(cleanKey);
        // Query author works
        Optional<OpenLibraryWorksResponse> worksResponseOpt = openLibraryClient.getAuthorWorks(cleanKey);

        if (authorDetailsOpt.isEmpty() && worksResponseOpt.isEmpty()) {
            log.debug("Author not found in OpenLibrary: {}", cleanKey);
            throw new AuthorNotFoundException(originalInput);
        }

        // Resolve author name (from author details, or fallback to cleanKey)
        String authorName = authorDetailsOpt
                .map(OpenLibraryAuthorDetailsResponse::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(cleanKey);

        // Persist author entity in DB
        Author author = authorRepository.findByExternalId(cleanKey)
                .orElseGet(() -> authorRepository.save(
                        Author.builder()
                                .externalId(cleanKey)
                                .name(authorName)
                                .build()
                ));

        if (worksResponseOpt.isPresent()) {
            return persistAndMapWorks(worksResponseOpt.get(), author);
        }

        return Collections.emptyList();
    }

    private List<AuthorDto> persistAndMapAuthors(OpenLibraryAuthorSearchResponse response) {
        List<AuthorDoc> docs = response.getDocs();
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplicate docs by normalized external key
        Map<String, AuthorDoc> uniqueDocs = new LinkedHashMap<>();
        for (AuthorDoc doc : docs) {
            if (doc != null && doc.getKey() != null && !doc.getKey().isBlank() && doc.getName() != null && !doc.getName().isBlank()) {
                String cleanKey = OpenLibraryClientImpl.normalizeAuthorKey(doc.getKey());
                uniqueDocs.putIfAbsent(cleanKey, doc);
            }
        }

        List<Author> newAuthors = new ArrayList<>();
        for (Map.Entry<String, AuthorDoc> entry : uniqueDocs.entrySet()) {
            String key = entry.getKey();
            AuthorDoc doc = entry.getValue();
            if (!authorRepository.existsByExternalId(key)) {
                newAuthors.add(Author.builder()
                        .externalId(key)
                        .name(doc.getName().trim())
                        .build());
            }
        }

        if (!newAuthors.isEmpty()) {
            authorRepository.saveAll(newAuthors);
            log.debug("Persisted {} new author(s) from OpenLibrary", newAuthors.size());
        }

        return uniqueDocs.entrySet().stream()
                .map(e -> AuthorDto.builder()
                        .id(e.getKey())
                        .name(e.getValue().getName().trim())
                        .build())
                .toList();
    }

    private List<WorkDto> persistAndMapWorks(OpenLibraryWorksResponse response, Author author) {
        List<WorkEntry> entries = response.getEntries();
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplicate entries by work key to avoid duplicate key errors
        Map<String, WorkEntry> uniqueEntries = new LinkedHashMap<>();
        for (WorkEntry entry : entries) {
            if (entry != null && entry.getKey() != null && !entry.getKey().isBlank() && entry.getTitle() != null && !entry.getTitle().isBlank()) {
                uniqueEntries.putIfAbsent(entry.getKey(), entry);
            }
        }

        List<Work> worksToSave = new ArrayList<>();
        for (WorkEntry entry : uniqueEntries.values()) {
            if (!workRepository.existsByExternalKey(entry.getKey())) {
                String title = entry.getTitle().trim();
                if (title.length() > 500) {
                    title = title.substring(0, 500);
                }
                worksToSave.add(Work.builder()
                        .externalKey(entry.getKey())
                        .title(title)
                        .author(author)
                        .build());
            }
        }

        if (!worksToSave.isEmpty()) {
            workRepository.saveAll(worksToSave);
            log.debug("Persisted {} work(s) for author: {}", worksToSave.size(), author.getExternalId());
        }

        return workMapper.entriesToDtoList(new ArrayList<>(uniqueEntries.values()));
    }
}
