import { useState, useEffect } from 'react';
import type { Movie, User } from '../types';
import { api } from '../api';
import { MovieCard } from './MovieCard';
import { RatingModal } from './RatingModal';

interface Props {
  user: User;
}

function SkeletonFeatured() {
  return (
    <div className="featured-card" style={{ marginBottom: '2.5rem' }}>
      <div className="skeleton-poster" style={{ width: 220, flexShrink: 0 }} />
      <div className="skeleton-info" style={{ flex: 1, padding: '1.75rem' }}>
        <div className="skeleton-line short" style={{ marginBottom: '1rem' }} />
        <div className="skeleton-line" style={{ height: '1.6rem', marginBottom: '0.75rem' }} />
        <div className="skeleton-line short" />
        <div className="skeleton-line" />
        <div className="skeleton-line" />
      </div>
    </div>
  );
}

export function Recommendations({ user }: Props) {
  const [data, setData] = useState<{
    userId: number;
    movies: Movie[];
    nextMovie: Movie | null;
  } | null>(null);
  const [error, setError] = useState('');
  const [ratingMovie, setRatingMovie] = useState<Movie | null>(null);
  const [ratedMovies, setRatedMovies] = useState<Map<number, number>>(new Map());
  const [toast, setToast] = useState<string | null>(null);

  const loading = data?.userId !== user.id;
  const movies = loading ? [] : (data?.movies ?? []);
  const nextMovie = loading ? null : (data?.nextMovie ?? null);

  useEffect(() => {
    let active = true;
    Promise.all([
      api.getRecommendations(user.id).catch(() => [] as Movie[]),
      api.getNextMovie(user.id).catch(() => null),
    ])
      .then(([recs, next]) => {
        if (active) {
          setData({ userId: user.id, movies: recs, nextMovie: next });
          setError('');
        }
      })
      .catch(() => {
        if (active) setError('Failed to load recommendations');
      });
    return () => {
      active = false;
    };
  }, [user.id]);

  async function handleRate(rating: number) {
    if (!ratingMovie) return;
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

  if (error) return <p className="error">{error}</p>;

  return (
    <div>
      {loading ? (
        <SkeletonFeatured />
      ) : nextMovie ? (
        <div className="featured-card">
          {nextMovie.posterUrl ? (
            <img
              src={nextMovie.posterUrl}
              alt={nextMovie.title}
              className="featured-poster"
            />
          ) : (
            <div className="featured-poster placeholder">No Poster</div>
          )}
          <div className="featured-info">
            <span className="featured-label">Up Next</span>
            <h2>{nextMovie.title}</h2>
            <span className="genre-badge">{nextMovie.genre}</span>
            <p className="movie-description">{nextMovie.description}</p>
            <button
              className="btn btn-primary featured-rate-btn"
              onClick={() => setRatingMovie(nextMovie)}
            >
              Rate this movie
            </button>
          </div>
        </div>
      ) : null}

      {loading ? (
        <div className="movie-grid">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="skeleton-card">
              <div className="skeleton-poster" />
              <div className="skeleton-info">
                <div className="skeleton-line" />
                <div className="skeleton-line short" />
              </div>
            </div>
          ))}
        </div>
      ) : movies.length > 0 ? (
        <>
          <h3 className="section-heading">Recommended for You</h3>
          <div className="movie-grid">
            {movies.map((movie) => (
              <MovieCard
                key={movie.id}
                movie={movie}
                onRate={() => setRatingMovie(movie)}
                userRating={ratedMovies.get(movie.id)}
              />
            ))}
          </div>
        </>
      ) : (
        <p className="empty-state">
          No recommendations yet. Rate some movies to get started!
        </p>
      )}

      {ratingMovie && (
        <RatingModal
          movieTitle={ratingMovie.title}
          onSubmit={handleRate}
          onClose={() => setRatingMovie(null)}
        />
      )}
      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
