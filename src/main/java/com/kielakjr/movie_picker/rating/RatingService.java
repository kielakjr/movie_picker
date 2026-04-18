package com.kielakjr.movie_picker.rating;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserRepository;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;
import com.kielakjr.movie_picker.recommendation.RecommendationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final RecommendationService recommendationService;
    private final DiscardedMovieRepository discardedMovieRepository;

    @Transactional
    public RatingResponse createRating(RatingRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Rating rating = Rating.builder()
                .movie(movie)
                .user(user)
                .rating(request.getRating())
                .build();
        RatingResponse response = mapToResponse(ratingRepository.save(rating));
        recommendationService.updateUserProfile(request.getUserId());
        return response;
    }

    @Transactional
    public void discardMovie(DiscardRequest request) {
        movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!discardedMovieRepository.existsByUserIdAndMovieId(request.getUserId(), request.getMovieId())) {
            discardedMovieRepository.save(DiscardedMovie.builder()
                    .userId(request.getUserId())
                    .movieId(request.getMovieId())
                    .build());
        }
    }

    public List<RatingResponse> getRatingsByUserId(Long userId) {
        return ratingRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private RatingResponse mapToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .movieId(rating.getMovie().getId())
                .userId(rating.getUser().getId())
                .rating(rating.getRating())
                .createdAt(rating.getCreatedAt())
                .movieTitle(rating.getMovie().getTitle())
                .posterUrl(rating.getMovie().getPosterUrl())
                .genre(rating.getMovie().getGenre())
                .build();
    }
}
