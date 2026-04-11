package com.kielakjr.movie_picker.movie;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    @Nested
    class CreateMovie {

        @Test
        void returnsResponseFromSavedMovie() {
            MovieRequest request = MovieRequest.builder()
                    .title("Inception")
                    .description("Dreams within dreams")
                    .genre("Sci-Fi")
                    .build();
            when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
                Movie input = invocation.getArgument(0);
                return Movie.builder()
                        .id(7L)
                        .title(input.getTitle())
                        .description(input.getDescription())
                        .genre(input.getGenre())
                        .build();
            });

            MovieResponse response = movieService.createMovie(request);

            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getTitle()).isEqualTo("Inception");
            assertThat(response.getDescription()).isEqualTo("Dreams within dreams");
            assertThat(response.getGenre()).isEqualTo("Sci-Fi");
        }
    }

    @Nested
    class GetMovieById {

        @Test
        void whenFound_returnsMatchingResponse() {
            Movie stored = Movie.builder()
                    .id(3L)
                    .title("Matrix")
                    .description("Neo")
                    .genre("Sci-Fi")
                    .build();
            when(movieRepository.findById(3L)).thenReturn(Optional.of(stored));

            MovieResponse response = movieService.getMovieById(3L);

            assertThat(response.getId()).isEqualTo(3L);
            assertThat(response.getTitle()).isEqualTo("Matrix");
            assertThat(response.getDescription()).isEqualTo("Neo");
            assertThat(response.getGenre()).isEqualTo("Sci-Fi");
        }

        @Test
        void whenNotFound_throws() {
            when(movieRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.getMovieById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    class GetAllMovies {

        @Test
        void whenRepositoryEmpty_returnsEmptyList() {
            when(movieRepository.findAll()).thenReturn(List.of());

            List<MovieResponse> responses = movieService.getAllMovies();

            assertThat(responses).isEmpty();
        }

        @Test
        void mapsEveryStoredMovieToResponse() {
            when(movieRepository.findAll()).thenReturn(List.of(
                    Movie.builder().id(1L).title("A").description("descA").genre("Drama").build(),
                    Movie.builder().id(2L).title("B").description("descB").genre("Comedy").build()
            ));

            List<MovieResponse> responses = movieService.getAllMovies();

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(MovieResponse::getTitle).containsExactly("A", "B");
            assertThat(responses).extracting(MovieResponse::getGenre).containsExactly("Drama", "Comedy");
        }
    }
}
