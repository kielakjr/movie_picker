package com.kielakjr.movie_picker.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscardedMovieRepository extends JpaRepository<DiscardedMovie, Long> {
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
}
