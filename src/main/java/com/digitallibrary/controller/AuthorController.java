package com.digitallibrary.controller;

import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.service.AuthorService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Validated
public class AuthorController {

    private final AuthorService authorService;

    /**
     * Search authors by name.
     * Checks local DB first, then falls back to OpenLibrary API.
     *
     * @param name the author name to search
     * @return list of matching authors
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuthorDto>> searchAuthors(
            @RequestParam @NotBlank(message = "Name parameter must not be blank") String name) {

        List<AuthorDto> authors = authorService.searchAuthorsByName(name);
        return ResponseEntity.ok(authors);
    }

    /**
     * Get works by author ID (OpenLibrary key).
     * Checks local DB first, then falls back to OpenLibrary API.
     *
     * @param authorId the OpenLibrary author key (e.g. "OL23919A")
     * @return list of the author's works
     */
    @GetMapping(value = "/{authorId}/works", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorkDto>> getAuthorWorks(
            @PathVariable @NotBlank String authorId) {

        List<WorkDto> works = authorService.getWorksByAuthorId(authorId);
        return ResponseEntity.ok(works);
    }
}
