package com.digitallibrary.mapper;

import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse.AuthorDoc;
import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.entity.Author;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorMapperTest {

    private final AuthorMapper mapper = Mappers.getMapper(AuthorMapper.class);

    @Test
    @DisplayName("toDto should map Author entity to AuthorDto")
    void toDto_MapsCorrectly() {
        Author author = Author.builder()
                .id(1L)
                .externalId("OL26320A")
                .name("J.R.R. Tolkien")
                .build();

        AuthorDto dto = mapper.toDto(author);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("OL26320A");
        assertThat(dto.getName()).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    @DisplayName("toDtoList should map list of Author entities to list of AuthorDtos")
    void toDtoList_MapsCorrectly() {
        Author author1 = Author.builder().externalId("OL1").name("Author 1").build();
        Author author2 = Author.builder().externalId("OL2").name("Author 2").build();

        List<AuthorDto> dtos = mapper.toDtoList(List.of(author1, author2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo("OL1");
        assertThat(dtos.get(1).getId()).isEqualTo("OL2");
    }

    @Test
    @DisplayName("toEntity should map AuthorDoc to Author entity")
    void toEntity_MapsCorrectly() {
        AuthorDoc doc = new AuthorDoc();
        doc.setKey("OL26320A");
        doc.setName("J.R.R. Tolkien");

        Author author = mapper.toEntity(doc);

        assertThat(author).isNotNull();
        assertThat(author.getExternalId()).isEqualTo("OL26320A");
        assertThat(author.getName()).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    @DisplayName("docToDto should map AuthorDoc to AuthorDto")
    void docToDto_MapsCorrectly() {
        AuthorDoc doc = new AuthorDoc();
        doc.setKey("OL26320A");
        doc.setName("J.R.R. Tolkien");

        AuthorDto dto = mapper.docToDto(doc);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("OL26320A");
        assertThat(dto.getName()).isEqualTo("J.R.R. Tolkien");
    }
}
