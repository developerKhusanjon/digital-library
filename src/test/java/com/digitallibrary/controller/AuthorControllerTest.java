package com.digitallibrary.controller;

import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.exception.AuthorNotFoundException;
import com.digitallibrary.exception.GlobalExceptionHandler;
import com.digitallibrary.service.AuthorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorController.class)
@Import(GlobalExceptionHandler.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorService authorService;

    @Test
    @DisplayName("GET /api/v1/authors?name=Tolkien - should return 200 OK with authors list")
    void searchAuthors_Success() throws Exception {
        List<AuthorDto> authors = List.of(
                new AuthorDto("OL26320A", "J.R.R. Tolkien"),
                new AuthorDto("OL12627750A", "Simon Tolkien")
        );

        when(authorService.searchAuthorsByName("Tolkien")).thenReturn(authors);

        mockMvc.perform(get("/api/v1/authors")
                        .param("name", "Tolkien")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("OL26320A")))
                .andExpect(jsonPath("$[0].name", is("J.R.R. Tolkien")))
                .andExpect(jsonPath("$[1].id", is("OL12627750A")))
                .andExpect(jsonPath("$[1].name", is("Simon Tolkien")));

        verify(authorService, times(1)).searchAuthorsByName("Tolkien");
    }

    @Test
    @DisplayName("GET /api/v1/authors - should return 400 Bad Request when name parameter is missing")
    void searchAuthors_MissingParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/authors")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));

        verify(authorService, never()).searchAuthorsByName(anyString());
    }

    @Test
    @DisplayName("GET /api/v1/authors?name=  - should return 400 Bad Request when name parameter is blank")
    void searchAuthors_BlankParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/authors")
                        .param("name", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));

        verify(authorService, never()).searchAuthorsByName(anyString());
    }

    @Test
    @DisplayName("GET /api/v1/authors/{authorId}/works - should return 200 OK with author works")
    void getAuthorWorks_Success() throws Exception {
        List<WorkDto> works = List.of(
                new WorkDto("/works/OL27516W", "The Hobbit"),
                new WorkDto("/works/OL27479W", "The Lord of the Rings")
        );

        when(authorService.getWorksByAuthorId("OL26320A")).thenReturn(works);

        mockMvc.perform(get("/api/v1/authors/OL26320A/works")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].key", is("/works/OL27516W")))
                .andExpect(jsonPath("$[0].title", is("The Hobbit")))
                .andExpect(jsonPath("$[1].key", is("/works/OL27479W")))
                .andExpect(jsonPath("$[1].title", is("The Lord of the Rings")));

        verify(authorService, times(1)).getWorksByAuthorId("OL26320A");
    }

    @Test
    @DisplayName("GET /api/v1/authors/{authorId}/works - should return 404 Not Found when author does not exist")
    void getAuthorWorks_NotFound() throws Exception {
        when(authorService.getWorksByAuthorId("UNKNOWN_ID"))
                .thenThrow(new AuthorNotFoundException("UNKNOWN_ID"));

        mockMvc.perform(get("/api/v1/authors/UNKNOWN_ID/works")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Author not found with id: UNKNOWN_ID")));

        verify(authorService, times(1)).getWorksByAuthorId("UNKNOWN_ID");
    }

    @Test
    @DisplayName("GET /api/v1/authors?q=Tolkien - should support 'q' query parameter")
    void searchAuthors_WithQParam_Success() throws Exception {
        List<AuthorDto> authors = List.of(new AuthorDto("OL26320A", "J.R.R. Tolkien"));
        when(authorService.searchAuthorsByName("Tolkien")).thenReturn(authors);

        mockMvc.perform(get("/api/v1/authors")
                        .param("q", "Tolkien")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("J.R.R. Tolkien")));

        verify(authorService, times(1)).searchAuthorsByName("Tolkien");
    }

    @Test
    @DisplayName("GET /authors?name=Tolkien - should support root alias path /authors")
    void searchAuthors_RootAliasPath_Success() throws Exception {
        List<AuthorDto> authors = List.of(new AuthorDto("OL26320A", "J.R.R. Tolkien"));
        when(authorService.searchAuthorsByName("Tolkien")).thenReturn(authors);

        mockMvc.perform(get("/authors")
                        .param("name", "Tolkien")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("J.R.R. Tolkien")));

        verify(authorService, times(1)).searchAuthorsByName("Tolkien");
    }

    @Test
    @DisplayName("GET /api/v1/authors/works?authorId=OL26320A - should support authorId as query param")
    void getAuthorWorks_WithQueryParam_Success() throws Exception {
        List<WorkDto> works = List.of(new WorkDto("/works/OL27516W", "The Hobbit"));
        when(authorService.getWorksByAuthorId("OL26320A")).thenReturn(works);

        mockMvc.perform(get("/api/v1/authors/works")
                        .param("authorId", "OL26320A")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("The Hobbit")));

        verify(authorService, times(1)).getWorksByAuthorId("OL26320A");
    }

    @Test
    @DisplayName("GET /api/v1/authors/{authorId}/works - should return 503 when circuit breaker is open")
    void getAuthorWorks_CircuitBreakerOpen_Returns503() throws Exception {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb =
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("openLibrary");
        io.github.resilience4j.circuitbreaker.CallNotPermittedException exception =
                io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException(cb);

        when(authorService.getWorksByAuthorId("OL26320A")).thenThrow(exception);

        mockMvc.perform(get("/api/v1/authors/OL26320A/works")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.error", is("Service Unavailable")));
    }
}
