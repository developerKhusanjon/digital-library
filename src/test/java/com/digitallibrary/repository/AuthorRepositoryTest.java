package com.digitallibrary.repository;

import com.digitallibrary.entity.Author;
import com.digitallibrary.entity.Work;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private WorkRepository workRepository;

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return authors matching search query case-insensitively")
    void findByNameContainingIgnoreCase_ReturnsMatchingAuthors() {
        Author tolkien = Author.builder()
                .externalId("OL26320A")
                .name("J.R.R. Tolkien")
                .build();
        Author rowling = Author.builder()
                .externalId("OL23919A")
                .name("J.K. Rowling")
                .build();

        entityManager.persist(tolkien);
        entityManager.persist(rowling);
        entityManager.flush();

        List<Author> results = authorRepository.findByNameContainingIgnoreCase("tolkien");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    @DisplayName("findByExternalId should return author by external OpenLibrary key")
    void findByExternalId_ReturnsAuthor() {
        Author author = Author.builder()
                .externalId("OL26320A")
                .name("J.R.R. Tolkien")
                .build();
        entityManager.persist(author);
        entityManager.flush();

        Optional<Author> found = authorRepository.findByExternalId("OL26320A");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    @DisplayName("existsByExternalId should return true when author exists, false otherwise")
    void existsByExternalId_ReturnsCorrectBoolean() {
        Author author = Author.builder()
                .externalId("OL26320A")
                .name("J.R.R. Tolkien")
                .build();
        entityManager.persist(author);
        entityManager.flush();

        assertThat(authorRepository.existsByExternalId("OL26320A")).isTrue();
        assertThat(authorRepository.existsByExternalId("NON_EXISTING")).isFalse();
    }

    @Test
    @DisplayName("findByAuthorId should return all works linked to author")
    void findByAuthorId_ReturnsWorksForAuthor() {
        Author author = Author.builder()
                .externalId("OL26320A")
                .name("J.R.R. Tolkien")
                .build();
        entityManager.persist(author);

        Work hobbit = Work.builder()
                .externalKey("/works/OL27516W")
                .title("The Hobbit")
                .author(author)
                .build();
        Work lotr = Work.builder()
                .externalKey("/works/OL27479W")
                .title("The Lord of the Rings")
                .author(author)
                .build();

        entityManager.persist(hobbit);
        entityManager.persist(lotr);
        entityManager.flush();

        List<Work> works = workRepository.findByAuthorId(author.getId());

        assertThat(works).hasSize(2);
        assertThat(works).extracting(Work::getTitle).containsExactlyInAnyOrder("The Hobbit", "The Lord of the Rings");
    }
}
