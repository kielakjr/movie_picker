package com.kielakjr.movie_picker.movie;

import com.kielakjr.movie_picker.config.TestcontainersConfig;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class MovieControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class CreateMovie {

        @Test
        void withValidFields_returnsCreatedMovieAndPersistsIt() throws Exception {
            MovieRequest request = MovieRequest.builder()
                    .title("Inception")
                    .description("A thief who steals corporate secrets through dream-sharing technology.")
                    .genre("Sci-Fi")
                    .build();

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("Inception"))
                    .andExpect(jsonPath("$.description").value("A thief who steals corporate secrets through dream-sharing technology."))
                    .andExpect(jsonPath("$.genre").value("Sci-Fi"));

            assertThat(movieRepository.findAll()).hasSize(1);
        }

        @Test
        void withBlankTitle_returnsBadRequest() throws Exception {
            MovieRequest request = MovieRequest.builder()
                    .title("")
                    .description("Something")
                    .genre("Drama")
                    .build();

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(movieRepository.findAll()).isEmpty();
        }

        @Test
        void withBlankDescription_returnsBadRequest() throws Exception {
            MovieRequest request = MovieRequest.builder()
                    .title("Title")
                    .description("")
                    .genre("Drama")
                    .build();

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(movieRepository.findAll()).isEmpty();
        }

        @Test
        void withBlankGenre_returnsBadRequest() throws Exception {
            MovieRequest request = MovieRequest.builder()
                    .title("Title")
                    .description("Something")
                    .genre("")
                    .build();

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(movieRepository.findAll()).isEmpty();
        }
    }

    @Nested
    class GetAllMovies {

        @Test
        void whenNoMovies_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void returnsAllPersistedMovies() throws Exception {
            movieRepository.save(Movie.builder().title("A").description("desc A").genre("Drama").build());
            movieRepository.save(Movie.builder().title("B").description("desc B").genre("Comedy").build());

            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[*].title",
                            org.hamcrest.Matchers.containsInAnyOrder("A", "B")));
        }
    }

    @Nested
    class GetMovieById {

        @Test
        void whenExists_returnsMovie() throws Exception {
            Movie saved = movieRepository.save(
                    Movie.builder().title("Matrix").description("Neo").genre("Sci-Fi").build());

            mockMvc.perform(get("/api/movies/{id}", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.title").value("Matrix"))
                    .andExpect(jsonPath("$.description").value("Neo"))
                    .andExpect(jsonPath("$.genre").value("Sci-Fi"));
        }
    }
}
