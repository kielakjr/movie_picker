import { useState, useEffect } from 'react';
import type { Rating, User } from '../types';
import { api } from '../api';

interface Props {
  user: User;
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

export function MyRatings({ user }: Props) {
  const [data, setData] = useState<{ userId: number; ratings: Rating[] } | null>(null);
  const [error, setError] = useState('');

  const loading = data?.userId !== user.id;
  const ratings = loading ? [] : (data?.ratings ?? []);

  useEffect(() => {
    let active = true;
    api
      .getUserRatings(user.id)
      .then((r) => {
        if (active) {
          const sorted = [...r].sort((a, b) => b.rating - a.rating);
          setData({ userId: user.id, ratings: sorted });
          setError('');
        }
      })
      .catch(() => {
        if (active) setError('Failed to load ratings');
      });
    return () => {
      active = false;
    };
  }, [user.id]);

  if (error) return <p className="error">{error}</p>;

  if (loading) return <SkeletonGrid />;

  if (ratings.length === 0) {
    return <p className="empty-state">No ratings yet. Rate some movies to get started!</p>;
  }

  return (
    <div className="movie-grid">
      {ratings.map((r) => (
        <div key={r.id} className="movie-card">
          <div className="movie-poster-wrapper">
            {r.posterUrl ? (
              <img src={r.posterUrl} alt={r.movieTitle} className="movie-poster" />
            ) : (
              <div className="movie-poster placeholder">No Poster</div>
            )}
            <span className="rating-badge">★ {r.rating}/10</span>
          </div>
          <div className="movie-info">
            <h3>{r.movieTitle}</h3>
            <span className="genre-badge">{r.genre}</span>
            <p className="rating-date">{new Date(r.createdAt).toLocaleDateString()}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
