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
import com.digitallibrary.repository.AuthorRepository;
import com.digitallibrary.repository.WorkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import com.digitallibrary.mapper.AuthorMapper;
import com.digitallibrary.mapper.WorkMapper;
import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private WorkRepository workRepository;

    @Mock
    private OpenLibraryClient openLibraryClient;

    @Spy
    private AuthorMapper authorMapper = Mappers.getMapper(AuthorMapper.class);

    @Spy
    private WorkMapper workMapper = Mappers.getMapper(WorkMapper.class);

    @InjectMocks
    private AuthorServiceImpl authorService;

    @Nested
    @DisplayName("searchAuthorsByName tests")
    class SearchAuthorsTests {

        @Test
        @DisplayName("When authors exist in DB, should return them directly without calling OpenLibrary")
        void searchAuthors_WhenFoundInDb_ShouldReturnFromDb() {
            // Given
            Author author = Author.builder()
                    .id(1L)
                    .externalId("OL26320A")
                    .name("J.R.R. Tolkien")
                    .build();

            when(authorRepository.findByNameContainingIgnoreCase("Tolkien"))
                    .thenReturn(List.of(author));

            // When
            List<AuthorDto> result = authorService.searchAuthorsByName("Tolkien");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("OL26320A");
            assertThat(result.get(0).getName()).isEqualTo("J.R.R. Tolkien");

            verify(authorRepository).findByNameContainingIgnoreCase("Tolkien");
            verifyNoInteractions(openLibraryClient);
        }

        @Test
        @DisplayName("When DB is empty, should call OpenLibrary and persist new authors")
        void searchAuthors_WhenNotInDb_ShouldFetchFromOpenLibraryAndPersist() {
            // Given
            when(authorRepository.findByNameContainingIgnoreCase("Tolkien"))
                    .thenReturn(Collections.emptyList());

            AuthorDoc doc1 = new AuthorDoc();
            doc1.setKey("OL26320A");
            doc1.setName("J.R.R. Tolkien");

            AuthorDoc doc2 = new AuthorDoc();
            doc2.setKey("OL12627750A");
            doc2.setName("Simon Tolkien");

            OpenLibraryAuthorSearchResponse apiResponse = new OpenLibraryAuthorSearchResponse();
            apiResponse.setNumFound(2);
            apiResponse.setDocs(List.of(doc1, doc2));

            when(openLibraryClient.searchAuthors("Tolkien")).thenReturn(Optional.of(apiResponse));
            when(authorRepository.existsByExternalId("OL26320A")).thenReturn(false);
            when(authorRepository.existsByExternalId("OL12627750A")).thenReturn(false);

            // When
            List<AuthorDto> result = authorService.searchAuthorsByName("Tolkien");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AuthorDto::getId).containsExactly("OL26320A", "OL12627750A");

            verify(authorRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("When author not in DB and OpenLibrary returns empty, should return empty list")
        void searchAuthors_WhenNotFoundAnywhere_ShouldReturnEmptyList() {
            // Given
            when(authorRepository.findByNameContainingIgnoreCase("Unknown"))
                    .thenReturn(Collections.emptyList());
            when(openLibraryClient.searchAuthors("Unknown")).thenReturn(Optional.empty());

            // When
            List<AuthorDto> result = authorService.searchAuthorsByName("Unknown");

            // Then
            assertThat(result).isEmpty();
            verify(authorRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("getWorksByAuthorId tests")
    class GetWorksTests {

        @Test
        @DisplayName("When works exist in DB, should return them directly without calling OpenLibrary")
        void getWorks_WhenCachedInDb_ShouldReturnFromDb() {
            // Given
            Author author = Author.builder()
                    .id(1L)
                    .externalId("OL26320A")
                    .name("J.R.R. Tolkien")
                    .build();
            Work work = Work.builder()
                    .id(10L)
                    .externalKey("/works/OL27516W")
                    .title("The Hobbit")
                    .author(author)
                    .build();

            when(authorRepository.findByExternalId("OL26320A")).thenReturn(Optional.of(author));
            when(workRepository.findByAuthorId(1L)).thenReturn(List.of(work));

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("OL26320A");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKey()).isEqualTo("/works/OL27516W");
            assertThat(result.get(0).getTitle()).isEqualTo("The Hobbit");

            verifyNoInteractions(openLibraryClient);
        }

        @Test
        @DisplayName("When author is in DB but works are not cached, should fetch from OpenLibrary and persist")
        void getWorks_WhenNotInDb_ShouldFetchAndPersist() {
            // Given
            Author author = Author.builder()
                    .id(1L)
                    .externalId("OL26320A")
                    .name("J.R.R. Tolkien")
                    .build();

            when(authorRepository.findByExternalId("OL26320A")).thenReturn(Optional.of(author));
            when(workRepository.findByAuthorId(1L)).thenReturn(Collections.emptyList());

            WorkEntry entry = new WorkEntry();
            entry.setKey("/works/OL27516W");
            entry.setTitle("The Hobbit");

            OpenLibraryWorksResponse apiResponse = new OpenLibraryWorksResponse();
            apiResponse.setSize(1);
            apiResponse.setEntries(List.of(entry));

            when(openLibraryClient.getAuthorWorks("OL26320A")).thenReturn(Optional.of(apiResponse));

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("OL26320A");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKey()).isEqualTo("/works/OL27516W");
            assertThat(result.get(0).getTitle()).isEqualTo("The Hobbit");

            verify(workRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("When author does not exist in DB or OpenLibrary, should throw AuthorNotFoundException")
        void getWorks_WhenAuthorNotFound_ShouldThrowException() {
            // Given
            when(authorRepository.findByExternalId("INVALID_ID")).thenReturn(Optional.empty());
            when(openLibraryClient.getAuthorWorks("INVALID_ID")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authorService.getWorksByAuthorId("INVALID_ID"))
                    .isInstanceOf(AuthorNotFoundException.class)
                    .hasMessageContaining("INVALID_ID");

            verify(workRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("When author not in DB but works returned from OpenLibrary, should create author and save works")
        void getWorks_WhenAuthorNotInDb_ShouldCreateAuthorAndSaveWorks() {
            // Given
            when(authorRepository.findByExternalId("OL26320A"))
                    .thenReturn(Optional.empty()) // first lookup
                    .thenReturn(Optional.empty()); // inside fallback

            Author newAuthor = Author.builder()
                    .id(2L)
                    .externalId("OL26320A")
                    .name("OL26320A")
                    .build();

            when(authorRepository.save(any(Author.class))).thenReturn(newAuthor);

            WorkEntry entry = new WorkEntry();
            entry.setKey("/works/OL27516W");
            entry.setTitle("The Hobbit");

            OpenLibraryWorksResponse apiResponse = new OpenLibraryWorksResponse();
            apiResponse.setSize(1);
            apiResponse.setEntries(List.of(entry));

            when(openLibraryClient.getAuthorWorks("OL26320A")).thenReturn(Optional.of(apiResponse));

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("OL26320A");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("The Hobbit");
            verify(authorRepository).save(any(Author.class));
            verify(workRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("When author not in DB, should fetch author details from OpenLibrary and persist with real name")
        void getWorks_WhenAuthorNotInDb_ShouldFetchAuthorDetailsAndSaveWithRealName() {
            // Given
            when(authorRepository.findByExternalId("OL23919A"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.empty());

            com.digitallibrary.client.dto.OpenLibraryAuthorDetailsResponse authorDetails =
                    com.digitallibrary.client.dto.OpenLibraryAuthorDetailsResponse.builder()
                            .key("/authors/OL23919A")
                            .name("J. K. Rowling")
                            .build();

            when(openLibraryClient.getAuthorDetails("OL23919A")).thenReturn(Optional.of(authorDetails));

            Author newAuthor = Author.builder()
                    .id(3L)
                    .externalId("OL23919A")
                    .name("J. K. Rowling")
                    .build();

            when(authorRepository.save(any(Author.class))).thenReturn(newAuthor);

            WorkEntry entry = new WorkEntry();
            entry.setKey("/works/OL46092343W");
            entry.setTitle("Harry Potter and the Philosopher's Stone");

            OpenLibraryWorksResponse worksResponse = new OpenLibraryWorksResponse();
            worksResponse.setEntries(List.of(entry));

            when(openLibraryClient.getAuthorWorks("OL23919A")).thenReturn(Optional.of(worksResponse));

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("OL23919A");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");

            verify(authorRepository).save(argThat(author -> "J. K. Rowling".equals(author.getName())));
        }

        @Test
        @DisplayName("When author ID contains human-readable slug, should normalize key and fetch works")
        void getWorks_WithHumanReadableSlug_ShouldNormalizeAndFetch() {
            // Given
            Author author = Author.builder()
                    .id(1L)
                    .externalId("OL23919A")
                    .name("J. K. Rowling")
                    .build();

            when(authorRepository.findByExternalId("OL23919A")).thenReturn(Optional.of(author));

            Work work = Work.builder()
                    .id(10L)
                    .externalKey("/works/OL46092343W")
                    .title("Harry Potter")
                    .author(author)
                    .build();

            when(workRepository.findByAuthorId(1L)).thenReturn(List.of(work));

            // When - query with slug like /authors/OL23919A/J._K._Rowling
            List<WorkDto> result = authorService.getWorksByAuthorId("/authors/OL23919A/J._K._Rowling");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKey()).isEqualTo("/works/OL46092343W");
        }

        @Test
        @DisplayName("When author ID is numeric database ID, should find author by primary key")
        void getWorks_WithNumericId_ShouldFindAuthorByPrimaryKey() {
            // Given
            Author author = Author.builder()
                    .id(5L)
                    .externalId("OL26320A")
                    .name("J.R.R. Tolkien")
                    .build();

            when(authorRepository.findById(5L)).thenReturn(Optional.of(author));

            Work work = Work.builder()
                    .id(20L)
                    .externalKey("/works/OL27516W")
                    .title("The Hobbit")
                    .author(author)
                    .build();

            when(workRepository.findByAuthorId(5L)).thenReturn(List.of(work));

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("5");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("The Hobbit");
            verify(authorRepository).findById(5L);
        }

        @Test
        @DisplayName("When works contain duplicates, should deduplicate and save each unique work once")
        void getWorks_WithDuplicateEntries_ShouldDeduplicate() {
            // Given
            Author author = Author.builder()
                    .id(1L)
                    .externalId("OL26320A")
                    .name("J.R.R. Tolkien")
                    .build();

            when(authorRepository.findByExternalId("OL26320A")).thenReturn(Optional.of(author));
            when(workRepository.findByAuthorId(1L)).thenReturn(Collections.emptyList());

            WorkEntry entry1 = new WorkEntry();
            entry1.setKey("/works/OL1W");
            entry1.setTitle("Work One");

            WorkEntry entry2 = new WorkEntry();
            entry2.setKey("/works/OL1W"); // duplicate key
            entry2.setTitle("Work One (Duplicate)");

            WorkEntry entry3 = new WorkEntry();
            entry3.setKey("/works/OL2W");
            entry3.setTitle("Work Two");

            OpenLibraryWorksResponse apiResponse = new OpenLibraryWorksResponse();
            apiResponse.setEntries(List.of(entry1, entry2, entry3));

            when(openLibraryClient.getAuthorWorks("OL26320A")).thenReturn(Optional.of(apiResponse));
            when(workRepository.existsByExternalKey("/works/OL1W")).thenReturn(false);
            when(workRepository.existsByExternalKey("/works/OL2W")).thenReturn(false);

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("OL26320A");

            // Then
            assertThat(result).hasSize(2);
            verify(workRepository).saveAll(argThat(iterable -> {
                List<Work> savedList = (List<Work>) iterable;
                return savedList.size() == 2;
            }));
        }

        @Test
        @DisplayName("When work title exceeds 500 characters, should truncate to 500 characters safely")
        void getWorks_WithLongTitle_ShouldTruncateSafely() {
            // Given
            Author author = Author.builder()
                    .id(1L)
                    .externalId("OL26320A")
                    .name("J.R.R. Tolkien")
                    .build();

            when(authorRepository.findByExternalId("OL26320A")).thenReturn(Optional.of(author));
            when(workRepository.findByAuthorId(1L)).thenReturn(Collections.emptyList());

            String longTitle = "A".repeat(600);
            WorkEntry entry = new WorkEntry();
            entry.setKey("/works/LONG_TITLE_WORK");
            entry.setTitle(longTitle);

            OpenLibraryWorksResponse apiResponse = new OpenLibraryWorksResponse();
            apiResponse.setEntries(List.of(entry));

            when(openLibraryClient.getAuthorWorks("OL26320A")).thenReturn(Optional.of(apiResponse));
            when(workRepository.existsByExternalKey("/works/LONG_TITLE_WORK")).thenReturn(false);

            // When
            List<WorkDto> result = authorService.getWorksByAuthorId("OL26320A");

            // Then
            assertThat(result).hasSize(1);
            verify(workRepository).saveAll(argThat(iterable -> {
                List<Work> savedList = (List<Work>) iterable;
                return savedList.get(0).getTitle().length() == 500;
            }));
        }
    }
}
