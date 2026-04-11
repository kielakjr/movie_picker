package com.kielakjr.movie_picker.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RatingRequest {
    private Long movieId;
    private Long userId;
    private Integer rating;
}
