package com.kielakjr.movie_picker.user;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@Transactional
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class CreateUser {

        @Test
        void withValidEmail_returnsCreatedUserAndPersistsIt() throws Exception {
            UserRequest request = UserRequest.builder().email("alice@example.com").build();

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.email").value("alice@example.com"));

            assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        }

        @Test
        void withInvalidEmailFormat_returnsBadRequest() throws Exception {
            UserRequest request = UserRequest.builder().email("not-an-email").build();

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(userRepository.findAll()).isEmpty();
        }

        @Test
        void withBlankEmail_returnsBadRequest() throws Exception {
            UserRequest request = UserRequest.builder().email("").build();

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(userRepository.findAll()).isEmpty();
        }
    }

    @Nested
    class GetAllUsers {

        @Test
        void whenNoUsers_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void returnsAllPersistedUsers() throws Exception {
            userRepository.save(User.builder().email("alice@example.com").build());
            userRepository.save(User.builder().email("bob@example.com").build());

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[*].email",
                            org.hamcrest.Matchers.containsInAnyOrder("alice@example.com", "bob@example.com")));
        }
    }
}
