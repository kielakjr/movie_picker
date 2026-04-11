package com.kielakjr.movie_picker.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    class CreateUser {

        @Test
        void returnsResponseWithIdAndEmailFromSavedUser() {
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User input = invocation.getArgument(0);
                return User.builder().id(42L).email(input.getEmail()).build();
            });

            UserResponse response = userService.createUser(
                    UserRequest.builder().email("alice@example.com").build());

            assertThat(response.getId()).isEqualTo(42L);
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
        }
    }

    @Nested
    class GetAllUsers {

        @Test
        void whenRepositoryEmpty_returnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> responses = userService.getAllUsers();

            assertThat(responses).isEmpty();
        }

        @Test
        void mapsEveryStoredUserToResponse() {
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id(1L).email("alice@example.com").build(),
                    User.builder().id(2L).email("bob@example.com").build()
            ));

            List<UserResponse> responses = userService.getAllUsers();

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(UserResponse::getId).containsExactly(1L, 2L);
            assertThat(responses).extracting(UserResponse::getEmail)
                    .containsExactly("alice@example.com", "bob@example.com");
        }
    }
}
