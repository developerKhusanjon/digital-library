package com.digitallibrary.client;

import com.digitallibrary.client.dto.OpenLibraryAuthorDetailsResponse;
import com.digitallibrary.client.dto.OpenLibraryAuthorSearchResponse;
import com.digitallibrary.client.dto.OpenLibraryWorksResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OpenLibraryClientImplTest {

    @ParameterizedTest(name = "normalizeAuthorKey(\"{0}\") should return \"{1}\"")
    @CsvSource({
            "OL23919A, OL23919A",
            "/authors/OL23919A, OL23919A",
            "OL23919A/J._K._Rowling, OL23919A",
            "/authors/OL23919A/J._K._Rowling, OL23919A",
            "OL23919A.json, OL23919A",
            "/authors/OL23919A.json, OL23919A",
            "https://openlibrary.org/authors/OL23919A/J._K._Rowling, OL23919A",
            "'', ''",
            "'   ', ''"
    })
    @DisplayName("normalizeAuthorKey should clean author slugs, prefixes, and extensions per OpenLibrary specs")
    void testNormalizeAuthorKey(String input, String expected) {
        assertThat(OpenLibraryClientImpl.normalizeAuthorKey(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("OpenLibraryAuthorDetailsResponse getDisplayName resolves name or personalName")
    void testAuthorDetailsDisplayName() {
        OpenLibraryAuthorDetailsResponse res1 = OpenLibraryAuthorDetailsResponse.builder()
                .name("J. K. Rowling")
                .personalName("Joanne Rowling")
                .build();
        assertThat(res1.getDisplayName()).isEqualTo("J. K. Rowling");

        OpenLibraryAuthorDetailsResponse res2 = OpenLibraryAuthorDetailsResponse.builder()
                .personalName("Joanne Rowling")
                .build();
        assertThat(res2.getDisplayName()).isEqualTo("Joanne Rowling");

        OpenLibraryAuthorDetailsResponse res3 = OpenLibraryAuthorDetailsResponse.builder().build();
        assertThat(res3.getDisplayName()).isNull();
    }
}
