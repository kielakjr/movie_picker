import type { Movie } from '../types';

interface Props {
  movie: Movie;
  onRate?: (movieId: number) => void;
}

export function MovieCard({ movie, onRate }: Props) {
  return (
    <div className="movie-card">
      {movie.posterUrl ? (
        <img src={movie.posterUrl} alt={movie.title} className="movie-poster" />
      ) : (
        <div className="movie-poster placeholder">No Poster</div>
      )}
      <div className="movie-info">
        <h3>{movie.title}</h3>
        <span className="genre-badge">{movie.genre}</span>
        <p className="movie-description">{movie.description}</p>
        {onRate && (
          <button className="btn btn-secondary" onClick={() => onRate(movie.id)}>
            Rate
          </button>
        )}
      </div>
    </div>
  );
}
