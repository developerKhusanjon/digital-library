package com.digitallibrary.mapper;

import com.digitallibrary.client.dto.OpenLibraryWorksResponse.WorkEntry;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.entity.Author;
import com.digitallibrary.entity.Work;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkMapperTest {

    private final WorkMapper mapper = Mappers.getMapper(WorkMapper.class);

    @Test
    @DisplayName("toDto should map Work entity to WorkDto")
    void toDto_MapsCorrectly() {
        Work work = Work.builder()
                .id(1L)
                .externalKey("/works/OL27516W")
                .title("The Hobbit")
                .build();

        WorkDto dto = mapper.toDto(work);

        assertThat(dto).isNotNull();
        assertThat(dto.getKey()).isEqualTo("/works/OL27516W");
        assertThat(dto.getTitle()).isEqualTo("The Hobbit");
    }

    @Test
    @DisplayName("toDtoList should map list of Work entities to list of WorkDtos")
    void toDtoList_MapsCorrectly() {
        Work work1 = Work.builder().externalKey("/w1").title("Title 1").build();
        Work work2 = Work.builder().externalKey("/w2").title("Title 2").build();

        List<WorkDto> dtos = mapper.toDtoList(List.of(work1, work2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getKey()).isEqualTo("/w1");
        assertThat(dtos.get(1).getKey()).isEqualTo("/w2");
    }

    @Test
    @DisplayName("toEntity should map WorkEntry and Author to Work entity")
    void toEntity_MapsCorrectly() {
        WorkEntry entry = new WorkEntry();
        entry.setKey("/works/OL27516W");
        entry.setTitle("The Hobbit");

        Author author = Author.builder().id(10L).name("J.R.R. Tolkien").build();

        Work work = mapper.toEntity(entry, author);

        assertThat(work).isNotNull();
        assertThat(work.getExternalKey()).isEqualTo("/works/OL27516W");
        assertThat(work.getTitle()).isEqualTo("The Hobbit");
        assertThat(work.getAuthor()).isEqualTo(author);
    }

    @Test
    @DisplayName("entryToDto should map WorkEntry to WorkDto")
    void entryToDto_MapsCorrectly() {
        WorkEntry entry = new WorkEntry();
        entry.setKey("/works/OL27516W");
        entry.setTitle("The Hobbit");

        WorkDto dto = mapper.entryToDto(entry);

        assertThat(dto).isNotNull();
        assertThat(dto.getKey()).isEqualTo("/works/OL27516W");
        assertThat(dto.getTitle()).isEqualTo("The Hobbit");
    }
}
