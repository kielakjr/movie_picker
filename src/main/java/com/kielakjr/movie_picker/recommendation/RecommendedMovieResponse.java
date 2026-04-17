package com.kielakjr.movie_picker.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RecommendedMovieResponse {
    private Long id;
    private String title;
    private String description;
    private String genre;
    private String posterUrl;
    private int clusterIndex;
}
