import { useState, useEffect } from 'react';
import type { Movie, User, Page } from '../types';
import { api } from '../api';
import { MovieCard } from './MovieCard';
import { RatingModal } from './RatingModal';

interface Props {
  user: User | null;
}

function SkeletonGrid() {
  return (
    <div className="movie-grid">
      {Array.from({ length: 8 }, (_, i) => (
        <div key={i} className="skeleton-card">
          <div className="skeleton-poster" />
          <div className="skeleton-info">
            <div className="skeleton-line" />
            <div className="skeleton-line short" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function MovieList({ user }: Props) {
  const [result, setResult] = useState<{ page: Page<Movie>; forPage: number } | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState('');
  const [ratingMovie, setRatingMovie] = useState<Movie | null>(null);
  const [ratedMovies, setRatedMovies] = useState<Map<number, number>>(new Map());
  const [toast, setToast] = useState<string | null>(null);

  const loading = result?.forPage !== currentPage;
  const page = loading ? null : result?.page ?? null;

  useEffect(() => {
    let active = true;
    api
      .getMovies(currentPage)
      .then((data) => {
        if (active) {
          setResult({ page: data, forPage: currentPage });
          setError('');
        }
      })
      .catch(() => {
        if (active) setError('Failed to load movies');
      });
    return () => {
      active = false;
    };
  }, [currentPage]);

  async function handleRate(rating: number) {
    if (!user || !ratingMovie) return;
    const movie = ratingMovie;
    try {
      await api.createRating(movie.id, user.id, rating);
      setRatedMovies((prev) => new Map(prev).set(movie.id, rating));
      setRatingMovie(null);
      setToast(`Rated "${movie.title}" — ${rating}/10`);
      setTimeout(() => setToast(null), 3000);
    } catch {
      setError('Failed to submit rating');
    }
  }

  return (
    <>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <SkeletonGrid />
      ) : page ? (
        <>
          <div className="movie-grid">
            {page.content.map((movie) => (
              <MovieCard
                key={movie.id}
                movie={movie}
                onRate={user ? () => setRatingMovie(movie) : undefined}
                userRating={ratedMovies.get(movie.id)}
              />
            ))}
          </div>
          {page.totalPages > 1 && (
            <div className="pagination">
              <button disabled={page.first} onClick={() => setCurrentPage((p) => p - 1)}>
                ← Prev
              </button>
              <span>
                Page {page.number + 1} of {page.totalPages}
              </span>
              <button disabled={page.last} onClick={() => setCurrentPage((p) => p + 1)}>
                Next →
              </button>
            </div>
          )}
        </>
      ) : null}
      {ratingMovie && (
        <RatingModal
          movieTitle={ratingMovie.title}
          onSubmit={handleRate}
          onClose={() => setRatingMovie(null)}
        />
      )}
      {toast && <div className="toast">{toast}</div>}
    </>
  );
}
