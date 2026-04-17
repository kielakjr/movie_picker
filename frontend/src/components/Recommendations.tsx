import { useState, useEffect } from 'react';
import type { Movie, User } from '../types';
import { api } from '../api';
import { MovieCard } from './MovieCard';

interface Props {
  user: User;
}

export function Recommendations({ user }: Props) {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [nextMovie, setNextMovie] = useState<Movie | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([
      api.getRecommendations(user.id).catch(() => [] as Movie[]),
      api.getNextMovie(user.id).catch(() => null),
    ])
      .then(([recs, next]) => {
        setMovies(recs);
        setNextMovie(next);
      })
      .catch(() => setError('Failed to load recommendations'))
      .finally(() => setLoading(false));
  }, [user.id]);

  if (loading) return <p className="loading">Loading recommendations...</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <div>
      {nextMovie && (
        <div className="featured-card">
          {nextMovie.posterUrl ? (
            <img src={nextMovie.posterUrl} alt={nextMovie.title} className="featured-poster" />
          ) : (
            <div className="featured-poster placeholder">No Poster</div>
          )}
          <div className="featured-info">
            <span className="featured-label">Up Next</span>
            <h2>{nextMovie.title}</h2>
            <span className="genre-badge">{nextMovie.genre}</span>
            <p className="movie-description">{nextMovie.description}</p>
          </div>
        </div>
      )}
      {movies.length > 0 ? (
        <>
          <h3 className="section-heading">Recommended for You</h3>
          <div className="movie-grid">
            {movies.map((movie) => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
        </>
      ) : (
        <p className="empty-state">
          No recommendations yet. Rate some movies to get started!
        </p>
      )}
    </div>
  );
}
