import { useState, useEffect } from 'react';
import type { Movie, User } from '../types';
import { api } from '../api';
import { MovieCard } from './MovieCard';
import { RatingModal } from './RatingModal';

interface Props {
  user: User | null;
}

export function MovieList({ user }: Props) {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ratingMovie, setRatingMovie] = useState<Movie | null>(null);

  useEffect(() => {
    setLoading(true);
    api
      .getMovies()
      .then(setMovies)
      .catch(() => setError('Failed to load movies'))
      .finally(() => setLoading(false));
  }, []);

  async function handleRate(rating: number) {
    if (!user || !ratingMovie) return;
    try {
      await api.createRating(ratingMovie.id, user.id, rating);
      setRatingMovie(null);
    } catch {
      setError('Failed to submit rating');
    }
  }

  if (loading) return <p className="loading">Loading movies...</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <>
      <div className="movie-grid">
        {movies.map((movie) => (
          <MovieCard
            key={movie.id}
            movie={movie}
            onRate={user ? () => setRatingMovie(movie) : undefined}
          />
        ))}
      </div>
      {ratingMovie && (
        <RatingModal
          movieTitle={ratingMovie.title}
          onSubmit={handleRate}
          onClose={() => setRatingMovie(null)}
        />
      )}
    </>
  );
}
