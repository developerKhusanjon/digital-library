package com.digitallibrary.repository;

import com.digitallibrary.entity.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {

    List<Work> findByAuthorId(Long authorId);

    boolean existsByAuthorId(Long authorId);

    boolean existsByExternalKey(String externalKey);

    java.util.Optional<Work> findByExternalKey(String externalKey);
}
