package com.kielakjr.movie_picker.recommendation;

import com.kielakjr.movie_picker.config.TestcontainersConfig;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserRepository;
import com.kielakjr.movie_picker.rating.Rating;
import com.kielakjr.movie_picker.rating.RatingRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class RecommendationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Nested
    class GetNextMovie {

        @Test
        void whenUserNotFound_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/recommendations/movies/next/{userId}", 999))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        void whenMoviesExist_returnsAMovie() throws Exception {
            User user = userRepository.save(User.builder().email("test@test.com").build());
            movieRepository.save(Movie.builder().title("Inception").description("Dreams").genre("Sci-Fi").build());

            mockMvc.perform(get("/api/recommendations/movies/next/{userId}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Inception"));
        }

        @Test
        void whenAllMoviesRated_returnsServerError() throws Exception {
            User user = userRepository.save(User.builder().email("test@test.com").build());
            Movie movie = movieRepository.save(Movie.builder().title("Inception").description("Dreams").genre("Sci-Fi").build());
            ratingRepository.save(Rating.builder().user(user).movie(movie).rating(8).build());

            mockMvc.perform(get("/api/recommendations/movies/next/{userId}", user.getId()))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    class GetRecommendations {

        @Test
        void whenUserNotFound_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/recommendations/{userId}", 999))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        void whenNoProfileVector_returnsUnratedMovies() throws Exception {
            User user = userRepository.save(User.builder().email("test@test.com").build());
            movieRepository.save(Movie.builder().title("A").description("d").genre("G").build());
            movieRepository.save(Movie.builder().title("B").description("d").genre("G").build());

            mockMvc.perform(get("/api/recommendations/{userId}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void excludesAlreadyRatedMovies() throws Exception {
            User user = userRepository.save(User.builder().email("test@test.com").build());
            Movie rated = movieRepository.save(Movie.builder().title("Rated").description("d").genre("G").build());
            movieRepository.save(Movie.builder().title("Unrated").description("d").genre("G").build());
            ratingRepository.save(Rating.builder().user(user).movie(rated).rating(5).build());

            mockMvc.perform(get("/api/recommendations/{userId}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Unrated"));
        }

        @Test
        void returnsAtMostFiveMovies() throws Exception {
            User user = userRepository.save(User.builder().email("test@test.com").build());
            for (int i = 0; i < 8; i++) {
                movieRepository.save(Movie.builder().title("Movie " + i).description("d").genre("G").build());
            }

            mockMvc.perform(get("/api/recommendations/{userId}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(5));
        }
    }
}
