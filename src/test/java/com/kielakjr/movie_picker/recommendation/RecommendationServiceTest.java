package com.kielakjr.movie_picker.recommendation;

import com.kielakjr.movie_picker.movie.DbscanClusterer;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.movie.MovieResponse;
import com.kielakjr.movie_picker.rating.Rating;
import com.kielakjr.movie_picker.rating.RatingRepository;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Spy
    private DbscanClusterer dbscanClusterer = new DbscanClusterer();

    @InjectMocks
    private RecommendationService recommendationService;

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
        void whenUserHasNoProfileVector_returnsRandomMovie() {
            User user = User.builder().id(1L).email("a@b.com").profileVector(null).build();
            Movie movie = Movie.builder().id(5L).title("Random").description("desc").genre("Drama").build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(ratingRepository.countByUserId(1L)).thenReturn(0L);
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.of(movie));

            MovieResponse response = recommendationService.getNextMovie(1L);

            assertThat(response.getTitle()).isEqualTo("Random");
            verify(movieRepository, never()).findTopUnratedMovieByEmbeddingSimilarity(any(), any());
        }

        @Test
        void whenUserHasNoRatings_returnsRandomMovie() {
            User user = User.builder().id(1L).email("a@b.com").profileVector(new float[384]).build();
            Movie movie = Movie.builder().id(5L).title("Random").description("desc").genre("Drama").build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(ratingRepository.countByUserId(1L)).thenReturn(0L);
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.of(movie));

            MovieResponse response = recommendationService.getNextMovie(1L);

            assertThat(response.getTitle()).isEqualTo("Random");
        }

        @Test
        void whenNoMoviesAvailable_throws() {
            User user = User.builder().id(1L).email("a@b.com").profileVector(null).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(ratingRepository.countByUserId(1L)).thenReturn(0L);
            when(movieRepository.findRandomUnratedMovie(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getNextMovie(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No movies available");
        }
    }

    @Nested
    class UpdateUserProfile {

        @Test
        void whenUserNotFound_throws() {
            when(ratingRepository.findByUserId(99L)).thenReturn(List.of());
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            Movie movie = Movie.builder().id(1L).title("A").embedding(new float[384]).build();
            Rating goodRating = Rating.builder().id(1L).rating(8)
                    .movie(movie)
                    .user(User.builder().id(99L).build())
                    .build();
            when(ratingRepository.findByUserId(99L)).thenReturn(List.of(goodRating));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.updateUserProfile(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void whenNoGoodRatings_doesNotUpdateProfile() {
            Movie movie = Movie.builder().id(1L).title("A").embedding(new float[384]).build();
            Rating lowRating = Rating.builder().id(1L).rating(3)
                    .movie(movie)
                    .user(User.builder().id(1L).build())
                    .build();
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(lowRating));

            recommendationService.updateUserProfile(1L);

            verify(userRepository, never()).save(any());
        }

        @Test
        void computesWeightedAverageFromGoodRatings() {
            float[] embedding1 = new float[384];
            embedding1[0] = 1.0f;
            float[] embedding2 = new float[384];
            embedding2[0] = 3.0f;

            Movie movie1 = Movie.builder().id(1L).title("A").embedding(embedding1).build();
            Movie movie2 = Movie.builder().id(2L).title("B").embedding(embedding2).build();
            Rating rating1 = Rating.builder().id(1L).rating(8).movie(movie1).user(User.builder().id(1L).build()).build();
            Rating rating2 = Rating.builder().id(2L).rating(10).movie(movie2).user(User.builder().id(1L).build()).build();

            User user = User.builder().id(1L).email("a@b.com").build();
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rating1, rating2));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            recommendationService.updateUserProfile(1L);

            assertThat(user.getProfileVector()).isNotNull();
            assertThat(user.getProfileVector()[0]).isCloseTo(2.0f, org.assertj.core.data.Offset.offset(0.001f));
            verify(userRepository).save(user);
        }

        @Test
        void skipsRatingsWithNullEmbedding() {
            float[] embedding = new float[384];
            embedding[0] = 2.0f;

            Movie movieWithEmbedding = Movie.builder().id(1L).title("A").embedding(embedding).build();
            Movie movieWithoutEmbedding = Movie.builder().id(2L).title("B").embedding(null).build();
            Rating rating1 = Rating.builder().id(1L).rating(9).movie(movieWithEmbedding).user(User.builder().id(1L).build()).build();
            Rating rating2 = Rating.builder().id(2L).rating(8).movie(movieWithoutEmbedding).user(User.builder().id(1L).build()).build();

            User user = User.builder().id(1L).email("a@b.com").build();
            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rating1, rating2));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            recommendationService.updateUserProfile(1L);

            assertThat(user.getProfileVector()[0]).isCloseTo(2.0f, org.assertj.core.data.Offset.offset(0.001f));
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
        void whenNoProfileVector_returnsUnratedMovies() {
            User user = User.builder().id(1L).email("a@b.com").profileVector(null).build();
            Movie unrated1 = Movie.builder().id(2L).title("U1").description("d").genre("G").build();
            Movie unrated2 = Movie.builder().id(3L).title("U2").description("d").genre("G").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(movieRepository.findUnratedMovies(1L, 5)).thenReturn(List.of(unrated1, unrated2));

            List<MovieResponse> results = recommendationService.getRecommendations(1L);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(MovieResponse::getTitle).containsExactly("U1", "U2");
        }

        @Test
        void whenHasProfileVector_usesSimilaritySearch() {
            float[] profileVector = new float[384];
            User user = User.builder().id(1L).email("a@b.com").profileVector(profileVector).build();
            Movie similar = Movie.builder().id(2L).title("Similar").description("d").genre("G").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(movieRepository.findTopUnratedMoviesByEmbeddingSimilarity(eq(1L), eq(profileVector), eq(5)))
                    .thenReturn(List.of(similar));

            List<MovieResponse> results = recommendationService.getRecommendations(1L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getTitle()).isEqualTo("Similar");
        }
    }
}
