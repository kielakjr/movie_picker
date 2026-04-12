package com.kielakjr.movie_picker.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kielakjr.movie_picker.movie.MovieResponse;
import java.util.List;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public List<MovieResponse> getRecommendations(@PathVariable Long userId) {
        return recommendationService.getRecommendations(userId);
    }

    @GetMapping("/movies/next/{userId}")
    public MovieResponse getNextMovie(@PathVariable Long userId) {
        return recommendationService.getNextMovie(userId);
    }
}
