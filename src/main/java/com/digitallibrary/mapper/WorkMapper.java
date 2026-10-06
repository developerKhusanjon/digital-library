package com.digitallibrary.mapper;

import com.digitallibrary.client.dto.OpenLibraryWorksResponse.WorkEntry;
import com.digitallibrary.dto.WorkDto;
import com.digitallibrary.entity.Author;
import com.digitallibrary.entity.Work;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkMapper {

    @Mapping(target = "key", source = "externalKey")
    WorkDto toDto(Work work);

    List<WorkDto> toDtoList(List<Work> works);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalKey", source = "entry.key")
    @Mapping(target = "title", source = "entry.title")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "createdAt", ignore = true)
    Work toEntity(WorkEntry entry, Author author);

    @Mapping(target = "key", source = "key")
    WorkDto entryToDto(WorkEntry entry);

    List<WorkDto> entriesToDtoList(List<WorkEntry> entries);
}
