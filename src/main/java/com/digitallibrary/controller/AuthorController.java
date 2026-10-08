package com.digitallibrary.controller;

import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/authors", "/api/authors", "/authors"})
@RequiredArgsConstructor
@Validated
public class AuthorController {

    private final AuthorService authorService;

    /**
     * Search authors by name in the database.
     * In case there is no author in db, search is continued on the authors API side.
     * The result of the API call is saved to the database.
     * Supports both 'name' and 'q' query parameters.
     *
     * @param name author name to search
     * @param q alternative query parameter for author name
     * @return list of matching authors with id and name
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuthorDto>> searchAuthors(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "q", required = false) String q) throws MissingServletRequestParameterException {

        if (name == null && q == null) {
            throw new MissingServletRequestParameterException("name", "String");
        }

        String searchName = (name != null && !name.isBlank()) ? name : q;
        if (searchName == null || searchName.isBlank()) {
            throw new IllegalArgumentException("Name parameter must not be blank");
        }

        List<AuthorDto> authors = authorService.searchAuthorsByName(searchName);
        return ResponseEntity.ok(authors);
    }

    /**
     * Search author's works by author id in the database.
     * In case there is no author's works in db, search is continued on the authors API side.
     * The result of the API call is saved to the database.
     *
     * @param pathAuthorId author ID (OpenLibrary key or numeric DB ID) from path
     * @param queryAuthorId optional authorId from query parameter
     * @return list of the author's works
     */
    @GetMapping(value = {"/{authorId}/works", "/works"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorkDto>> getAuthorWorks(
            @PathVariable(name = "authorId", required = false) String pathAuthorId,
            @RequestParam(name = "authorId", required = false) String queryAuthorId) throws MissingServletRequestParameterException {

        String authorId = (pathAuthorId != null && !pathAuthorId.isBlank()) ? pathAuthorId : queryAuthorId;
        if (authorId == null || authorId.isBlank()) {
            throw new MissingServletRequestParameterException("authorId", "String");
        }

        List<WorkDto> works = authorService.getWorksByAuthorId(authorId);
        return ResponseEntity.ok(works);
    }
}
