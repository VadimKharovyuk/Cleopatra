package com.example.cleopatra.repository;

import com.example.cleopatra.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    Optional<Photo> findByIdAndAuthorId(Long photoId, Long authorId);


    List<Photo> findByAuthorIdOrderByUploadDateDesc(Long userId);

}
