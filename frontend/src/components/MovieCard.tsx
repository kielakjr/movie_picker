import type { Movie } from '../types';

interface Props {
  movie: Movie;
  onRate?: (movieId: number) => void;
  userRating?: number;
}

export function MovieCard({ movie, onRate, userRating }: Props) {
  return (
    <div className="movie-card">
      <div className="movie-poster-wrapper">
        {movie.posterUrl ? (
          <img src={movie.posterUrl} alt={movie.title} className="movie-poster" />
        ) : (
          <div className="movie-poster placeholder">No Poster</div>
        )}
        {userRating !== undefined && (
          <span className="rating-badge">★ {userRating}/10</span>
        )}
      </div>
      <div className="movie-info">
        <h3>{movie.title}</h3>
        <span className="genre-badge">{movie.genre}</span>
        <p className="movie-description">{movie.description}</p>
        {onRate && (
          <button className="btn btn-secondary" onClick={() => onRate(movie.id)}>
            {userRating !== undefined ? 'Re-rate' : 'Rate'}
          </button>
        )}
      </div>
    </div>
  );
}
