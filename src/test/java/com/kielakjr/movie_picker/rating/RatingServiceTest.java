package com.kielakjr.movie_picker.rating;

import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.recommendation.RecommendationService;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RatingService ratingService;

    @Nested
    class CreateRating {

        @Test
        void whenMovieAndUserExist_returnsResponseBuiltFromSavedRating() {
            User alice = User.builder().id(10L).email("alice@example.com").build();
            Movie matrix = Movie.builder().id(20L).title("Matrix").description("d").genre("Sci-Fi").build();
            LocalDateTime createdAt = LocalDateTime.of(2026, 4, 11, 12, 0);
            when(movieRepository.findById(20L)).thenReturn(Optional.of(matrix));
            when(userRepository.findById(10L)).thenReturn(Optional.of(alice));
            when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
                Rating input = invocation.getArgument(0);
                return Rating.builder()
                        .id(99L)
                        .movie(input.getMovie())
                        .user(input.getUser())
                        .rating(input.getRating())
                        .createdAt(createdAt)
                        .build();
            });

            RatingResponse response = ratingService.createRating(
                    RatingRequest.builder().movieId(20L).userId(10L).rating(5).build());

            assertThat(response.getId()).isEqualTo(99L);
            assertThat(response.getMovieId()).isEqualTo(20L);
            assertThat(response.getUserId()).isEqualTo(10L);
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        void whenMovieNotFound_throws() {
            when(movieRepository.findById(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.createRating(
                    RatingRequest.builder().movieId(20L).userId(10L).rating(5).build()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Movie");
        }

        @Test
        void whenUserNotFound_throws() {
            Movie matrix = Movie.builder().id(20L).title("Matrix").build();
            when(movieRepository.findById(20L)).thenReturn(Optional.of(matrix));
            when(userRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.createRating(
                    RatingRequest.builder().movieId(20L).userId(10L).rating(5).build()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    class GetRatingsByUserId {

        @Test
        void whenUserHasNoRatings_returnsEmptyList() {
            when(ratingRepository.findByUserId(10L)).thenReturn(List.of());

            List<RatingResponse> responses = ratingService.getRatingsByUserId(10L);

            assertThat(responses).isEmpty();
        }

        @Test
        void mapsEveryFoundRatingToResponse() {
            User alice = User.builder().id(10L).email("alice@example.com").build();
            Movie matrix = Movie.builder().id(20L).title("Matrix").build();
            Movie inception = Movie.builder().id(21L).title("Inception").build();
            LocalDateTime t1 = LocalDateTime.of(2026, 4, 11, 12, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 4, 11, 13, 0);
            when(ratingRepository.findByUserId(10L)).thenReturn(List.of(
                    Rating.builder().id(1L).user(alice).movie(matrix).rating(5).createdAt(t1).build(),
                    Rating.builder().id(2L).user(alice).movie(inception).rating(4).createdAt(t2).build()
            ));

            List<RatingResponse> responses = ratingService.getRatingsByUserId(10L);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(RatingResponse::getId).containsExactly(1L, 2L);
            assertThat(responses).extracting(RatingResponse::getMovieId).containsExactly(20L, 21L);
            assertThat(responses).extracting(RatingResponse::getRating).containsExactly(5, 4);
            assertThat(responses).allMatch(r -> r.getUserId().equals(10L));
        }
    }
}
