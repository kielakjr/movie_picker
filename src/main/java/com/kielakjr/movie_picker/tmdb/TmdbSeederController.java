package com.kielakjr.movie_picker.tmdb;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class TmdbSeederController {

    private final TmdbSeederService tmdbSeederService;

    @PostMapping("/seed-tmdb")
    public ResponseEntity<String> seedTmdb(@RequestParam(defaultValue = "5") int pages) {
        int count = tmdbSeederService.seedMovies(pages);
        return ResponseEntity.ok("Added " + count + " movies from TMDB");
    }
}
