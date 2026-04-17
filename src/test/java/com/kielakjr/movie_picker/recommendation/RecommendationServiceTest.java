package com.kielakjr.movie_picker.recommendation;

import com.kielakjr.movie_picker.movie.DbscanClusterer;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.movie.MovieResponse;
import com.kielakjr.movie_picker.rating.Rating;
import com.kielakjr.movie_picker.rating.RatingRepository;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserProfileCluster;
import com.kielakjr.movie_picker.user.UserProfileClusterRepository;
import com.kielakjr.movie_picker.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserProfileClusterRepository userProfileClusterRepository;

    @Mock
    private DoubleSupplier explorationRandom;

    @Spy
    private DbscanClusterer dbscanClusterer = new DbscanClusterer();

    @InjectMocks
    private RecommendationService recommendationService;

    private User user(long id) {
        return User.builder().id(id).email("a@b.com").build();
    }

    @Nested
    class GetNextMovie {

        @Test
        void whenUserNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getNextMovie(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void whenUserHasNoRatings_returnsRandomMovie() {
            Movie movie = Movie.builder().id(5L).title("Random").description("desc").genre("Drama").build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.countByUserId(1L)).thenReturn(0L);
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.of(movie));

            MovieResponse response = recommendationService.getNextMovie(1L);

            assertThat(response.getTitle()).isEqualTo("Random");
        }

        @Test
        void whenNoMoviesAvailable_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getNextMovie(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No movies available");
        }

        @Test
        void whenExplorationFires_returnsRandomMovie() {
            Movie random = Movie.builder().id(10L).title("Random").description("d").genre("G").build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.countByUserId(1L)).thenReturn(5L);
            when(explorationRandom.getAsDouble()).thenReturn(0.1);
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.of(random));

            MovieResponse response = recommendationService.getNextMovie(1L);

            assertThat(response.getTitle()).isEqualTo("Random");
            verify(movieRepository, never()).findTopUnratedMovieByEmbeddingSimilarity(any(), any());
        }

        @Test
        void whenExplorationSkipped_andNoClusters_returnsRandomMovie() {
            Movie random = Movie.builder().id(10L).title("Random").description("d").genre("G").build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.countByUserId(1L)).thenReturn(5L);
            when(explorationRandom.getAsDouble()).thenReturn(1.0);
            when(userProfileClusterRepository.findByUserId(1L)).thenReturn(List.of());
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.of(random));

            MovieResponse response = recommendationService.getNextMovie(1L);

            assertThat(response.getTitle()).isEqualTo("Random");
            verify(movieRepository, never()).findTopUnratedMovieByEmbeddingSimilarity(any(), any());
        }

        @Test
        void whenExplorationSkipped_andMultipleClusters_returnsBestMatchAcrossClusters() {
            float[] centroid1 = new float[384]; centroid1[0] = 1.0f;
            float[] centroid2 = new float[384]; centroid2[1] = 1.0f;

            UserProfileCluster cluster1 = UserProfileCluster.builder().userId(1L).centroid(centroid1).build();
            UserProfileCluster cluster2 = UserProfileCluster.builder().userId(1L).centroid(centroid2).build();

            float[] embA = new float[384]; embA[0] = 1.0f;
            float[] embB = new float[384]; embB[0] = 0.5f; embB[1] = 0.5f;

            Movie movieA = Movie.builder().id(10L).title("PerfectMatch").description("d").genre("G").embedding(embA).build();
            Movie movieB = Movie.builder().id(20L).title("PartialMatch").description("d").genre("G").embedding(embB).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.countByUserId(1L)).thenReturn(5L);
            when(explorationRandom.getAsDouble()).thenReturn(1.0);
            when(userProfileClusterRepository.findByUserId(1L)).thenReturn(List.of(cluster1, cluster2));
            when(movieRepository.findTopUnratedMovieByEmbeddingSimilarity(1L, centroid1)).thenReturn(Optional.of(movieA));
            when(movieRepository.findTopUnratedMovieByEmbeddingSimilarity(1L, centroid2)).thenReturn(Optional.of(movieB));

            MovieResponse response = recommendationService.getNextMovie(1L);

            assertThat(response.getTitle()).isEqualTo("PerfectMatch");
        }
    }

    @Nested
    class UpdateUserProfile {

        @Test
        void whenUserNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.updateUserProfile(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void whenNoGoodRatings_doesNotPersistClusters() {
            Movie movie = Movie.builder().id(1L).title("A").embedding(new float[384]).build();
            Rating lowRating = Rating.builder().id(1L).rating(3)
                    .movie(movie)
                    .user(User.builder().id(1L).build())
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(lowRating));

            recommendationService.updateUserProfile(1L);

            verify(userProfileClusterRepository, never()).saveAll(any());
        }

        @Test
        void centroidIsComputedAndPersistedForGoodRatings() {
            float[] embedding1 = new float[384]; embedding1[0] = 1.0f;
            float[] embedding2 = new float[384]; embedding2[0] = 2.0f;
            float[] embedding3 = new float[384]; embedding3[0] = 3.0f;

            Movie movie1 = Movie.builder().id(1L).title("A").embedding(embedding1).build();
            Movie movie2 = Movie.builder().id(2L).title("B").embedding(embedding2).build();
            Movie movie3 = Movie.builder().id(3L).title("C").embedding(embedding3).build();
            Rating rating1 = Rating.builder().id(1L).rating(8).movie(movie1).user(User.builder().id(1L).build()).build();
            Rating rating2 = Rating.builder().id(2L).rating(10).movie(movie2).user(User.builder().id(1L).build()).build();
            Rating rating3 = Rating.builder().id(3L).rating(9).movie(movie3).user(User.builder().id(1L).build()).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rating1, rating2, rating3));

            List<UserProfileCluster> saved = new ArrayList<>();
            when(userProfileClusterRepository.saveAll(any())).thenAnswer(inv -> {
                Iterable<UserProfileCluster> arg = inv.getArgument(0);
                arg.forEach(saved::add);
                return List.of();
            });

            recommendationService.updateUserProfile(1L);

            assertThat(saved).isNotEmpty();
            assertThat(saved.get(0).getCentroid()[0]).isCloseTo(2.0f, org.assertj.core.data.Offset.offset(0.001f));
        }

        @Test
        void whenAllGoodRatedMoviesHaveNullEmbedding_doesNotPersistClusters() {
            Movie movieWithoutEmbedding = Movie.builder().id(1L).title("A").embedding(null).build();
            Rating goodRating = Rating.builder().id(1L).rating(9)
                    .movie(movieWithoutEmbedding)
                    .user(User.builder().id(1L).build())
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(goodRating));

            recommendationService.updateUserProfile(1L);

            verify(userProfileClusterRepository, never()).saveAll(any());
        }

        @Test
        void skipsRatingsWithNullEmbeddingWhenComputingCentroid() {
            float[] embedding = new float[384]; embedding[0] = 2.0f;

            Movie movieWithEmbedding1 = Movie.builder().id(1L).title("A").embedding(embedding).build();
            Movie movieWithEmbedding2 = Movie.builder().id(2L).title("B").embedding(embedding).build();
            Movie movieWithEmbedding3 = Movie.builder().id(3L).title("C").embedding(embedding).build();
            Movie movieWithoutEmbedding = Movie.builder().id(4L).title("D").embedding(null).build();
            Rating rating1 = Rating.builder().id(1L).rating(9).movie(movieWithEmbedding1).user(User.builder().id(1L).build()).build();
            Rating rating2 = Rating.builder().id(2L).rating(8).movie(movieWithEmbedding2).user(User.builder().id(1L).build()).build();
            Rating rating3 = Rating.builder().id(3L).rating(9).movie(movieWithEmbedding3).user(User.builder().id(1L).build()).build();
            Rating rating4 = Rating.builder().id(4L).rating(8).movie(movieWithoutEmbedding).user(User.builder().id(1L).build()).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rating1, rating2, rating3, rating4));

            List<UserProfileCluster> saved = new ArrayList<>();
            when(userProfileClusterRepository.saveAll(any())).thenAnswer(inv -> {
                Iterable<UserProfileCluster> arg = inv.getArgument(0);
                arg.forEach(saved::add);
                return List.of();
            });

            recommendationService.updateUserProfile(1L);

            assertThat(saved).isNotEmpty();
            assertThat(saved.get(0).getCentroid()[0]).isCloseTo(2.0f, org.assertj.core.data.Offset.offset(0.001f));
        }

        @Test
        void savesAllClustersForDistinctTasteGroups() {
            float[] embA = new float[384]; embA[0] = 1.0f;
            float[] embB = new float[384]; embB[1] = 1.0f;

            Movie mA1 = Movie.builder().id(1L).title("A1").embedding(embA).build();
            Movie mA2 = Movie.builder().id(2L).title("A2").embedding(embA).build();
            Movie mA3 = Movie.builder().id(5L).title("A3").embedding(embA).build();
            Movie mB1 = Movie.builder().id(3L).title("B1").embedding(embB).build();
            Movie mB2 = Movie.builder().id(4L).title("B2").embedding(embB).build();
            Movie mB3 = Movie.builder().id(6L).title("B3").embedding(embB).build();

            Rating rA1 = Rating.builder().id(1L).rating(9).movie(mA1).user(User.builder().id(1L).build()).build();
            Rating rA2 = Rating.builder().id(2L).rating(8).movie(mA2).user(User.builder().id(1L).build()).build();
            Rating rA3 = Rating.builder().id(5L).rating(9).movie(mA3).user(User.builder().id(1L).build()).build();
            Rating rB1 = Rating.builder().id(3L).rating(9).movie(mB1).user(User.builder().id(1L).build()).build();
            Rating rB2 = Rating.builder().id(4L).rating(8).movie(mB2).user(User.builder().id(1L).build()).build();
            Rating rB3 = Rating.builder().id(6L).rating(8).movie(mB3).user(User.builder().id(1L).build()).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rA1, rA2, rA3, rB1, rB2, rB3));

            List<UserProfileCluster> capturedClusters = new ArrayList<>();
            when(userProfileClusterRepository.saveAll(any())).thenAnswer(invocation -> {
                Iterable<UserProfileCluster> arg = invocation.getArgument(0);
                arg.forEach(capturedClusters::add);
                return List.of();
            });

            recommendationService.updateUserProfile(1L);

            assertThat(capturedClusters).hasSize(2);
            assertThat(capturedClusters).allMatch(c -> c.getUserId().equals(1L));
        }
    }

    @Nested
    class GetRecommendations {

        @Test
        void whenUserNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getRecommendations(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void whenNoClusters_returnsUnratedMovies() {
            Movie unrated1 = Movie.builder().id(2L).title("U1").description("d").genre("G").build();
            Movie unrated2 = Movie.builder().id(3L).title("U2").description("d").genre("G").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of());
            when(movieRepository.findUnratedMovies(1L, 5)).thenReturn(List.of(unrated1, unrated2));

            List<RecommendedMovieResponse> results = recommendationService.getRecommendations(1L);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(RecommendedMovieResponse::getTitle).containsExactly("U1", "U2");
        }

        @Test
        void whenNoClusters_setsClusterIndexToMinusOne() {
            Movie unrated = Movie.builder().id(2L).title("U1").description("d").genre("G").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of());
            when(movieRepository.findUnratedMovies(1L, 5)).thenReturn(List.of(unrated));

            List<RecommendedMovieResponse> results = recommendationService.getRecommendations(1L);

            assertThat(results.get(0).getClusterIndex()).isEqualTo(-1);
        }

        @Test
        void whenHasMultipleClusters_mergesAndDeduplicatesAcrossClusters() {
            float[] centroid1 = new float[384]; centroid1[0] = 1.0f;
            float[] centroid2 = new float[384]; centroid2[1] = 1.0f;

            UserProfileCluster cluster1 = UserProfileCluster.builder().userId(1L).centroid(centroid1).build();
            UserProfileCluster cluster2 = UserProfileCluster.builder().userId(1L).centroid(centroid2).build();

            float[] emb1 = new float[384]; emb1[0] = 1.0f;
            float[] emb2 = new float[384]; emb2[1] = 1.0f;
            float[] embShared = new float[384]; embShared[0] = 0.5f; embShared[1] = 0.5f;

            Movie m1 = Movie.builder().id(1L).title("A").description("d").genre("G").embedding(emb1).build();
            Movie m2 = Movie.builder().id(2L).title("B").description("d").genre("G").embedding(emb2).build();
            Movie mShared = Movie.builder().id(3L).title("Shared").description("d").genre("G").embedding(embShared).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of(cluster1, cluster2));
            when(movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(1L, centroid1, 5))
                    .thenReturn(List.of(m1, mShared));
            when(movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(1L, centroid2, 5))
                    .thenReturn(List.of(m2, mShared));

            List<RecommendedMovieResponse> results = recommendationService.getRecommendations(1L);

            assertThat(results).hasSize(3);
            assertThat(results).extracting(RecommendedMovieResponse::getTitle).doesNotHaveDuplicates();
        }

        @Test
        void assignsClusterIndexBasedOnNearestCentroid() {
            float[] centroid1 = new float[384]; centroid1[0] = 1.0f;
            float[] centroid2 = new float[384]; centroid2[1] = 1.0f;

            UserProfileCluster cluster1 = UserProfileCluster.builder().userId(1L).centroid(centroid1).build();
            UserProfileCluster cluster2 = UserProfileCluster.builder().userId(1L).centroid(centroid2).build();

            float[] embCloseToCentroid1 = new float[384]; embCloseToCentroid1[0] = 1.0f;

            Movie movie = Movie.builder().id(1L).title("A").description("d").genre("G").embedding(embCloseToCentroid1).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of(cluster1, cluster2));
            when(movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(1L, centroid1, 5)).thenReturn(List.of(movie));
            when(movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(1L, centroid2, 5)).thenReturn(List.of());

            List<RecommendedMovieResponse> results = recommendationService.getRecommendations(1L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getClusterIndex()).isEqualTo(0);
        }
    }

    @Nested
    class GetTasteProfile {

        @Test
        void whenUserNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getTasteProfile(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void whenNoClusters_returnsEmptyList() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of());

            List<TasteProfileResponse> result = recommendationService.getTasteProfile(1L);

            assertThat(result).isEmpty();
        }

        @Test
        void groupsMoviesByNearestCluster() {
            float[] centroid1 = new float[384]; centroid1[0] = 1.0f;
            float[] centroid2 = new float[384]; centroid2[1] = 1.0f;

            UserProfileCluster cluster1 = UserProfileCluster.builder().userId(1L).centroid(centroid1).build();
            UserProfileCluster cluster2 = UserProfileCluster.builder().userId(1L).centroid(centroid2).build();

            float[] embA = new float[384]; embA[0] = 1.0f;
            float[] embB = new float[384]; embB[1] = 1.0f;

            Movie movieA = Movie.builder().id(1L).title("A").genre("Drama").embedding(embA).build();
            Movie movieB = Movie.builder().id(2L).title("B").genre("Action").embedding(embB).build();

            Rating ratingA = Rating.builder().id(1L).rating(9).movie(movieA).user(User.builder().id(1L).build()).build();
            Rating ratingB = Rating.builder().id(2L).rating(8).movie(movieB).user(User.builder().id(1L).build()).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of(cluster1, cluster2));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(ratingA, ratingB));

            List<TasteProfileResponse> result = recommendationService.getTasteProfile(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getClusterIndex()).isEqualTo(0);
            assertThat(result.get(0).getGenres()).containsExactly("Drama");
            assertThat(result.get(0).getMovieCount()).isEqualTo(1);
            assertThat(result.get(1).getClusterIndex()).isEqualTo(1);
            assertThat(result.get(1).getGenres()).containsExactly("Action");
            assertThat(result.get(1).getMovieCount()).isEqualTo(1);
        }

        @Test
        void excludesLowRatingsFromProfile() {
            float[] centroid = new float[384]; centroid[0] = 1.0f;
            UserProfileCluster cluster = UserProfileCluster.builder().userId(1L).centroid(centroid).build();

            float[] emb = new float[384]; emb[0] = 1.0f;
            Movie movie = Movie.builder().id(1L).title("A").genre("Drama").embedding(emb).build();
            Rating lowRating = Rating.builder().id(1L).rating(5).movie(movie).user(User.builder().id(1L).build()).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
            when(userProfileClusterRepository.findByUserIdOrderById(1L)).thenReturn(List.of(cluster));
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(lowRating));

            List<TasteProfileResponse> result = recommendationService.getTasteProfile(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMovieCount()).isEqualTo(0);
            assertThat(result.get(0).getGenres()).isEmpty();
        }
    }
}
