package com.kielakjr.movie_picker.movie;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MovieRequest {
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
    @NotBlank(message = "Genre is required")
    private String genre;
}
