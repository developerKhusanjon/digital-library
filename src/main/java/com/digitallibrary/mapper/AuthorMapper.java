package com.digitallibrary.mapper;

import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse.AuthorDoc;
import com.digitallibrary.dto.AuthorDto;
import com.digitallibrary.entity.Author;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "id", source = "externalId")
    AuthorDto toDto(Author author);

    List<AuthorDto> toDtoList(List<Author> authors);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", source = "key")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "works", ignore = true)
    Author toEntity(AuthorDoc doc);

    @Mapping(target = "id", source = "key")
    AuthorDto docToDto(AuthorDoc doc);

    List<AuthorDto> docsToDtoList(List<AuthorDoc> docs);
}
