package com.kielakjr.movie_picker.rating;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RatingResponse {
    private Long id;
    private Long movieId;
    private Long userId;
    private Integer rating;
    private LocalDateTime createdAt;
}
