package com.kielakjr.movie_picker.movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    boolean existsByTitle(String title);
    @Query(value = """
            SELECT * FROM movies
            WHERE id NOT IN (
                SELECT movie_id FROM ratings WHERE user_id = :userId
            )
            ORDER BY RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<Movie> findRandomUnratedMovie(@Param("userId") Long userId);

    @Query(value = """
            SELECT * FROM movies
            WHERE id NOT IN (
                SELECT movie_id FROM ratings WHERE user_id = :userId
            )
            ORDER BY embedding <=> CAST(:vector AS vector)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Movie> findTopUnratedMovieByEmbeddingSimilarity(
            @Param("userId") Long userId,
            @Param("vector") float[] vector);

    @Query(value = """
            SELECT * FROM movies
            WHERE id NOT IN (
                SELECT movie_id FROM ratings WHERE user_id = :userId
            )
            ORDER BY embedding <=> CAST(:vector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Movie> findTopUnratedMoviesByEmbeddingSimilarity(
            @Param("userId") Long userId,
            @Param("vector") float[] vector,
            @Param("limit") int limit);
}
