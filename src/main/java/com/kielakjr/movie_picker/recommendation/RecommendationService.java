package com.kielakjr.movie_picker.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kielakjr.movie_picker.rating.Rating;
import com.kielakjr.movie_picker.rating.RatingRepository;
import com.kielakjr.movie_picker.user.UserProfileCluster;
import com.kielakjr.movie_picker.user.UserProfileClusterRepository;
import com.kielakjr.movie_picker.user.UserRepository;
import com.kielakjr.movie_picker.movie.DbscanClusterer;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.movie.MovieResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final UserProfileClusterRepository userProfileClusterRepository;
    private final DbscanClusterer dbscanClusterer;
    private final DoubleSupplier explorationRandom;

    private static final double EXPLORATION_RATE = 0.2;
    private static final int DEFAULT_RECOMMENDATION_COUNT = 5;

    public MovieResponse getNextMovie(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (ratingRepository.countByUserId(userId) == 0) return randomOrThrow(userId);

        if (explorationRandom.getAsDouble() < EXPLORATION_RATE) return randomOrThrow(userId);

        List<UserProfileCluster> clusters = userProfileClusterRepository.findByUserId(userId);

        if (clusters.isEmpty()) return randomOrThrow(userId);

        return bestCandidateAcrossClusters(userId, clusters)
                .map(this::toMovieResponse)
                .orElseGet(() -> randomOrThrow(userId));
    }

    private MovieResponse randomOrThrow(Long userId) {
        return toMovieResponse(movieRepository.findRandomUnratedMovie(userId)
                .orElseThrow(() -> new IllegalStateException("No movies available")));
    }

    @Transactional
    public void updateUserProfile(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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

        userProfileClusterRepository.deleteByUserId(userId);
        userProfileClusterRepository.saveAll(
                centroids.stream()
                        .map(c -> UserProfileCluster.builder().userId(userId).centroid(c).build())
                        .toList()
        );
    }

    public List<RecommendedMovieResponse> getRecommendations(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserProfileCluster> clusters = userProfileClusterRepository.findByUserIdOrderById(userId);

        if (clusters.isEmpty()) {
            return movieRepository.findUnratedMovies(userId, DEFAULT_RECOMMENDATION_COUNT)
                    .stream()
                    .map(m -> toRecommendedMovieResponse(m, -1))
                    .toList();
        }

        return clusters.stream()
                .flatMap(c -> movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(
                        userId, c.getCentroid(), DEFAULT_RECOMMENDATION_COUNT).stream())
                .collect(Collectors.toMap(Movie::getId, m -> m, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingDouble((Movie m) -> minCosineDistanceToAnyCluster(m.getEmbedding(), clusters)))
                .limit(DEFAULT_RECOMMENDATION_COUNT)
                .map(m -> toRecommendedMovieResponse(m, findNearestClusterIndex(m.getEmbedding(), clusters)))
                .toList();
    }

    public List<TasteProfileResponse> getTasteProfile(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserProfileCluster> clusters = userProfileClusterRepository.findByUserIdOrderById(userId);
        if (clusters.isEmpty()) return List.of();

        List<Rating> goodRatings = ratingRepository.findByUserId(userId).stream()
                .filter(r -> r.getRating() >= 7)
                .filter(r -> r.getMovie().getEmbedding() != null)
                .toList();

        Map<Integer, List<String>> clusterGenres = new HashMap<>();
        Map<Integer, Integer> clusterCounts = new HashMap<>();
        for (Rating r : goodRatings) {
            int idx = findNearestClusterIndex(r.getMovie().getEmbedding(), clusters);
            clusterGenres.computeIfAbsent(idx, k -> new ArrayList<>()).add(r.getMovie().getGenre());
            clusterCounts.merge(idx, 1, Integer::sum);
        }

        return IntStream.range(0, clusters.size())
                .mapToObj(i -> {
                    List<String> genreList = clusterGenres.getOrDefault(i, List.of());
                    List<String> topGenres = genreList.stream()
                            .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(3)
                            .map(Map.Entry::getKey)
                            .toList();
                    return TasteProfileResponse.builder()
                            .clusterIndex(i)
                            .genres(topGenres)
                            .movieCount(clusterCounts.getOrDefault(i, 0))
                            .build();
                })
                .toList();
    }

    private Optional<Movie> bestCandidateAcrossClusters(Long userId, List<UserProfileCluster> clusters) {
        return clusters.stream()
                .map(c -> movieRepository.findTopUnratedMovieByEmbeddingSimilarity(userId, c.getCentroid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingDouble((Movie m) -> minCosineDistanceToAnyCluster(m.getEmbedding(), clusters)));
    }

    private double minCosineDistanceToAnyCluster(float[] embedding, List<UserProfileCluster> clusters) {
        if (embedding == null) return Double.MAX_VALUE;
        return clusters.stream()
                .mapToDouble(c -> cosineDistance(embedding, c.getCentroid()))
                .min()
                .orElse(Double.MAX_VALUE);
    }

    private int findNearestClusterIndex(float[] embedding, List<UserProfileCluster> clusters) {
        if (embedding == null) return 0;
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < clusters.size(); i++) {
            double dist = cosineDistance(embedding, clusters.get(i).getCentroid());
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private double cosineDistance(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 1.0;
        return 1.0 - dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private MovieResponse toMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .posterUrl(movie.getPosterUrl())
                .build();
    }

    private RecommendedMovieResponse toRecommendedMovieResponse(Movie movie, int clusterIndex) {
        return RecommendedMovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .posterUrl(movie.getPosterUrl())
                .clusterIndex(clusterIndex)
                .build();
    }
}
