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

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;

    private static final double EXPLORATION_RATE = 0.2;

    public MovieResponse getNextMovie(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Long> ratedMovieIds = ratingRepository.findByUserId(userId)
                .stream()
                .map(rating -> rating.getMovie().getId())
                .toList();

        if (user.getProfileVector() == null || ratedMovieIds.isEmpty()) {
            return toMovieResponse(movieRepository.findRandomUnratedMovie(userId).orElseThrow(() -> new IllegalStateException("No movies available")));
        }

        if (Math.random() < EXPLORATION_RATE) {
            return toMovieResponse(movieRepository.findRandomUnratedMovie(userId).orElseThrow(() -> new IllegalStateException("No movies available")));
        }

        return toMovieResponse(movieRepository.findTopUnratedMovieByEmbeddingSimilarity(userId, user.getProfileVector())
                .orElseGet(() -> movieRepository.findRandomUnratedMovie(userId).orElseThrow(() -> new IllegalStateException("No movies available"))));
    }

    public void updateUserProfile(Long userId) {

        List<Rating> ratings = ratingRepository.findByUserId(userId);

        List<Rating> goodRatings = ratings.stream()
                .filter(r -> r.getRating() >= 7)
                .toList();

        if (goodRatings.isEmpty()) return;

        float[] profileVector = new float[384];

        for (Rating rating : goodRatings) {
            float[] embedding = rating.getMovie().getEmbedding();
            if (embedding == null) continue;
            for (int i = 0; i < 384; i++) {
                profileVector[i] += embedding[i] * rating.getRating();
            }
        }

        float totalWeight = (float) goodRatings.stream()
                .mapToInt(Rating::getRating)
                .sum();

        for (int i = 0; i < 384; i++) {
            profileVector[i] /= totalWeight;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setProfileVector(profileVector);
        userRepository.save(user);
    }

    public List<MovieResponse> getRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Long> ratedMovieIds = ratingRepository.findByUserId(userId)
                .stream()
                .map(r -> r.getMovie().getId())
                .toList();

        if (user.getProfileVector() == null) {
            return movieRepository.findAll().stream()
                    .filter(m -> !ratedMovieIds.contains(m.getId()))
                    .limit(5)
                    .map(this::toMovieResponse)
                    .toList();
        }

        return movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(userId, user.getProfileVector(), 5)
                .stream()
                .map(this::toMovieResponse)
                .toList();
    }

    public MovieResponse toMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .build();
    }
}
