package com.kielakjr.movie_picker.rating;

import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RatingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RatingRepository ratingRepository;

    private User persistUser(String email) {
        return userRepository.save(User.builder().email(email).build());
    }

    private Movie persistMovie(String title) {
        return movieRepository.save(
                Movie.builder().title(title).description("desc").genre("Drama").build());
    }

    @Nested
    class CreateRating {

        @Test
        void withValidMovieAndUser_returnsCreatedRatingAndPersistsIt() throws Exception {
            User alice = persistUser("alice@example.com");
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(alice.getId())
                    .rating(5)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.movieId").value(matrix.getId()))
                    .andExpect(jsonPath("$.userId").value(alice.getId()))
                    .andExpect(jsonPath("$.rating").value(5))
                    .andExpect(jsonPath("$.createdAt").exists());

            assertThat(ratingRepository.findByUserId(alice.getId())).hasSize(1);
        }

        @Test
        void withMinimumRating_isAccepted() throws Exception {
            User alice = persistUser("alice@example.com");
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(alice.getId())
                    .rating(1)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(1));
        }

        @Test
        void withMaximumRating_isAccepted() throws Exception {
            User alice = persistUser("alice@example.com");
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(alice.getId())
                    .rating(10)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(10));
        }

        @Test
        void withNullMovieId_returnsBadRequest() throws Exception {
            User alice = persistUser("alice@example.com");

            RatingRequest request = RatingRequest.builder()
                    .movieId(null)
                    .userId(alice.getId())
                    .rating(5)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(ratingRepository.findAll()).isEmpty();
        }

        @Test
        void withNullUserId_returnsBadRequest() throws Exception {
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(null)
                    .rating(5)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(ratingRepository.findAll()).isEmpty();
        }

        @Test
        void withNullRating_returnsBadRequest() throws Exception {
            User alice = persistUser("alice@example.com");
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(alice.getId())
                    .rating(null)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(ratingRepository.findAll()).isEmpty();
        }

        @Test
        void withRatingBelowMinimum_returnsBadRequest() throws Exception {
            User alice = persistUser("alice@example.com");
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(alice.getId())
                    .rating(0)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(ratingRepository.findAll()).isEmpty();
        }

        @Test
        void withRatingAboveMaximum_returnsBadRequest() throws Exception {
            User alice = persistUser("alice@example.com");
            Movie matrix = persistMovie("Matrix");

            RatingRequest request = RatingRequest.builder()
                    .movieId(matrix.getId())
                    .userId(alice.getId())
                    .rating(11)
                    .build();

            mockMvc.perform(post("/api/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(ratingRepository.findAll()).isEmpty();
        }
    }

    @Nested
    class GetRatingsByUserId {

        @Test
        void whenUserHasNoRatings_returnsEmptyList() throws Exception {
            User alice = persistUser("alice@example.com");

            mockMvc.perform(get("/api/ratings/user/{userId}", alice.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void returnsOnlyRatingsBelongingToThatUser() throws Exception {
            User alice = persistUser("alice@example.com");
            User bob = persistUser("bob@example.com");
            Movie matrix = persistMovie("Matrix");
            Movie inception = persistMovie("Inception");

            ratingRepository.save(Rating.builder().user(alice).movie(matrix).rating(5).build());
            ratingRepository.save(Rating.builder().user(alice).movie(inception).rating(4).build());
            ratingRepository.save(Rating.builder().user(bob).movie(matrix).rating(3).build());

            mockMvc.perform(get("/api/ratings/user/{userId}", alice.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[*].userId",
                            org.hamcrest.Matchers.everyItem(
                                    org.hamcrest.Matchers.equalTo(alice.getId().intValue()))))
                    .andExpect(jsonPath("$[*].rating",
                            org.hamcrest.Matchers.containsInAnyOrder(5, 4)));
        }
    }
}
