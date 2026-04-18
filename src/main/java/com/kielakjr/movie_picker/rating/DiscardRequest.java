package com.kielakjr.movie_picker.rating;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DiscardRequest {
    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @NotNull(message = "User ID is required")
    private Long userId;
}
