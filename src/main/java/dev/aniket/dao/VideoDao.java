package dev.aniket.dao;

import dev.aniket.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoDao extends JpaRepository<Video, Integer> {
    Optional<Video> findByTitle(String title);
}
