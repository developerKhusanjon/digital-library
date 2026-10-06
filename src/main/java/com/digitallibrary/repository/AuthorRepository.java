package com.digitallibrary.repository;

import com.digitallibrary.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findByNameContainingIgnoreCase(String name);

    Optional<Author> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
