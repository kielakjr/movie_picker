package com.kielakjr.movie_picker.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kielakjr.movie_picker.ai.EmbeddingClient;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TmdbSeederService {

    private final MovieRepository movieRepository;
    private final EmbeddingClient embeddingClient;
    private RestClient restClient;

    @Value("${tmdb.token}")
    private String tmdbToken;

    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    private static final String POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl(TMDB_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + tmdbToken)
                .build();
    }

    public int seedMovies(int pages) {
        Map<Long, String> genreById = fetchGenreMap();

        int count = 0;
        for (int page = 1; page <= pages; page++) {
            TmdbPageResponse response = restClient.get()
                    .uri("/movie/popular?language=en-US&page=" + page)
                    .retrieve()
                    .body(TmdbPageResponse.class);

            if (response == null) continue;

            for (TmdbMovie tmdbMovie : response.results()) {
                if (movieRepository.existsByTitle(tmdbMovie.title())) continue;
                if (tmdbMovie.overview() == null || tmdbMovie.overview().isBlank()) continue;

                String genres = tmdbMovie.genreIds().stream()
                        .map(id -> genreById.getOrDefault(id, ""))
                        .filter(name -> !name.isBlank())
                        .collect(Collectors.joining(", "));

                String posterUrl = tmdbMovie.posterPath() != null
                        ? POSTER_BASE_URL + tmdbMovie.posterPath()
                        : null;

                float[] embedding = embeddingClient.getEmbedding(
                        tmdbMovie.title() + " " + genres + " " + tmdbMovie.overview()
                );

                Movie movie = Movie.builder()
                        .title(tmdbMovie.title())
                        .description(tmdbMovie.overview())
                        .genre(genres)
                        .posterUrl(posterUrl)
                        .embedding(embedding)
                        .build();

                movieRepository.save(movie);
                count++;
            }
        }
        return count;
    }

    private Map<Long, String> fetchGenreMap() {
        TmdbGenreListResponse response = restClient.get()
                .uri("/genre/movie/list?language=en-US")
                .retrieve()
                .body(TmdbGenreListResponse.class);
        if (response == null || response.genres() == null) return Map.of();
        return response.genres().stream()
                .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));
    }

    record TmdbPageResponse(List<TmdbMovie> results) {}

    record TmdbMovie(
            Long id,
            String title,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("genre_ids") List<Long> genreIds
    ) {}

    record TmdbGenreListResponse(List<TmdbGenre> genres) {}

    record TmdbGenre(Long id, String name) {}
}
