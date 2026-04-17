package com.kielakjr.movie_picker.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.kielakjr.movie_picker.movie.MovieResponse;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/movies/next/{userId}")
    public ResponseEntity<MovieResponse> getNextMovie(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getNextMovie(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<RecommendedMovieResponse>> getRecommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<List<TasteProfileResponse>> getTasteProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getTasteProfile(userId));
    }
}
