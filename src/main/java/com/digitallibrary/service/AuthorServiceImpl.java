package com.digitallibrary.service;

import com.digitallibrary.client.OpenLibraryClient;
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

import java.util.Collections;
import java.util.List;

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
        log.debug("Searching authors in DB by name: {}", name);

        List<Author> localAuthors = authorRepository.findByNameContainingIgnoreCase(name);
        if (!localAuthors.isEmpty()) {
            log.debug("Found {} author(s) in DB for name: {}", localAuthors.size(), name);
            return authorMapper.toDtoList(localAuthors);
        }

        log.debug("No authors in DB, querying OpenLibrary API for: {}", name);
        return openLibraryClient.searchAuthors(name)
                .map(this::persistAndMapAuthors)
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional
    public List<WorkDto> getWorksByAuthorId(String authorId) {
        log.debug("Searching works for author: {}", authorId);

        Author author = authorRepository.findByExternalId(authorId).orElse(null);
        if (author != null) {
            List<Work> localWorks = workRepository.findByAuthorId(author.getId());
            if (!localWorks.isEmpty()) {
                log.debug("Found {} work(s) in DB for author: {}", localWorks.size(), authorId);
                return workMapper.toDtoList(localWorks);
            }
        }

        log.debug("No works in DB, querying OpenLibrary API for author: {}", authorId);
        return openLibraryClient.getAuthorWorks(authorId)
                .map(response -> persistAndMapWorks(response, authorId, author))
                .orElseThrow(() -> new AuthorNotFoundException(authorId));
    }

    // --- Private helpers ---

    private List<AuthorDto> persistAndMapAuthors(OpenLibraryAuthorSearchResponse response) {
        List<AuthorDoc> docs = response.getDocs();
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Author> newAuthors = docs.stream()
                .filter(doc -> doc.getKey() != null && doc.getName() != null)
                .filter(doc -> !authorRepository.existsByExternalId(doc.getKey()))
                .map(authorMapper::toEntity)
                .toList();

        if (!newAuthors.isEmpty()) {
            authorRepository.saveAll(newAuthors);
            log.debug("Persisted {} new author(s) from OpenLibrary", newAuthors.size());
        }

        return authorMapper.docsToDtoList(docs);
    }

    private List<WorkDto> persistAndMapWorks(OpenLibraryWorksResponse response, String authorId, Author existingAuthor) {
        List<WorkEntry> entries = response.getEntries();
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        Author author = (existingAuthor != null) ? existingAuthor : getOrCreateAuthor(authorId);

        List<Work> works = entries.stream()
                .filter(entry -> entry.getKey() != null && entry.getTitle() != null)
                .map(entry -> workMapper.toEntity(entry, author))
                .toList();

        if (!works.isEmpty()) {
            workRepository.saveAll(works);
            log.debug("Persisted {} work(s) for author: {}", works.size(), authorId);
        }

        return workMapper.entriesToDtoList(entries);
    }

    private Author getOrCreateAuthor(String authorId) {
        return authorRepository.findByExternalId(authorId)
                .orElseGet(() -> authorRepository.save(
                        Author.builder()
                                .externalId(authorId)
                                .name(authorId)
                                .build()
                ));
    }
}
