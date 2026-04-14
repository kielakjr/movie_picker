package com.kielakjr.movie_picker.rating;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    @EntityGraph(attributePaths = "movie")
    List<Rating> findByUserId(Long userId);

    long countByUserId(Long userId);
}
