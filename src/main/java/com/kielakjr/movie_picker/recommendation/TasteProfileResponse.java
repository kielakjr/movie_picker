package com.kielakjr.movie_picker.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TasteProfileResponse {
    private int clusterIndex;
    private List<String> genres;
    private int movieCount;
}
