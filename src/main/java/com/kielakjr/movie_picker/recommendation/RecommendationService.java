package com.kielakjr.movie_picker.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.kielakjr.movie_picker.rating.Rating;
import com.kielakjr.movie_picker.rating.RatingRepository;
import com.kielakjr.movie_picker.user.UserRepository;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieResponse;
import com.kielakjr.movie_picker.movie.DbscanClusterer;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final DbscanClusterer dbscanClusterer;
    private final DoubleSupplier explorationRandom;

    private static final double EXPLORATION_RATE = 0.2;
    private static final int DEFAULT_RECOMMENDATION_COUNT = 5;

    public MovieResponse getNextMovie(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getProfileVector() == null) return randomOrThrow(userId);

        if (ratingRepository.countByUserId(userId) == 0) return randomOrThrow(userId);

        if (explorationRandom.getAsDouble() < EXPLORATION_RATE) return randomOrThrow(userId);

        return toMovieResponse(movieRepository.findTopUnratedMovieByEmbeddingSimilarity(userId, user.getProfileVector())
                .orElseGet(() -> movieRepository.findRandomUnratedMovie(userId)
                        .orElseThrow(() -> new IllegalStateException("No movies available"))));
    }

    private MovieResponse randomOrThrow(Long userId) {
        return toMovieResponse(movieRepository.findRandomUnratedMovie(userId)
                .orElseThrow(() -> new IllegalStateException("No movies available")));
    }

    public void updateUserProfile(Long userId) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);

        List<Rating> goodRatings = ratings.stream()
                .filter(r -> r.getRating() >= 7)
                .toList();

        if (goodRatings.isEmpty()) return;

        List<float[]> vectors = goodRatings.stream()
                .map(r -> r.getMovie().getEmbedding())
                .filter(Objects::nonNull)
                .toList();

        if (vectors.isEmpty()) return;

        List<float[]> centroids = dbscanClusterer.findClusterCentroids(vectors);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setProfileVector(centroids.get(0));
        userRepository.save(user);
    }

    public List<MovieResponse> getRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getProfileVector() == null) {
            return movieRepository.findUnratedMovies(userId, DEFAULT_RECOMMENDATION_COUNT)
                    .stream()
                    .map(this::toMovieResponse)
                    .toList();
        }

        return movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(userId, user.getProfileVector(), DEFAULT_RECOMMENDATION_COUNT)
                .stream()
                .map(this::toMovieResponse)
                .toList();
    }

    private MovieResponse toMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .build();
    }
}
