import { useState, useEffect } from 'react';
import type { Movie, User, Page } from '../types';
import { api } from '../api';
import { MovieCard } from './MovieCard';
import { RatingModal } from './RatingModal';

interface Props {
  user: User | null;
}

export function MovieList({ user }: Props) {
  const [page, setPage] = useState<Page<Movie> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ratingMovie, setRatingMovie] = useState<Movie | null>(null);

  useEffect(() => {
    setLoading(true);
    api
      .getMovies(currentPage)
      .then(setPage)
      .catch(() => setError('Failed to load movies'))
      .finally(() => setLoading(false));
  }, [currentPage]);

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
  if (!page) return null;

  return (
    <>
      <div className="movie-grid">
        {page.content.map((movie) => (
          <MovieCard
            key={movie.id}
            movie={movie}
            onRate={user ? () => setRatingMovie(movie) : undefined}
          />
        ))}
      </div>
      {page.totalPages > 1 && (
        <div className="pagination">
          <button
            disabled={page.first}
            onClick={() => setCurrentPage((p) => p - 1)}
          >
            Previous
          </button>
          <span>
            Page {page.number + 1} of {page.totalPages}
          </span>
          <button
            disabled={page.last}
            onClick={() => setCurrentPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
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
