package com.kielakjr.movie_picker.rating;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.kielakjr.movie_picker.user.User;
import com.kielakjr.movie_picker.user.UserRepository;
import com.kielakjr.movie_picker.movie.Movie;
import com.kielakjr.movie_picker.movie.MovieRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    public RatingResponse createRating(RatingRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Rating rating = Rating.builder()
                .movie(movie)
                .user(user)
                .rating(request.getRating())
                .build();
        return mapToResponse(ratingRepository.save(rating));
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
                .build();
    }
}
