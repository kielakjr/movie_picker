import { useCallback, useEffect, useRef, useState } from 'react';
import type { Movie, User } from '../types';
import { api } from '../api';
import { SwipeCard } from './SwipeCard';
import { RatingModal } from './RatingModal';

interface Props {
  user: User | null;
  onRecommendationsUnlocked: () => void;
}

interface QueueItem {
  movie: Movie;
  key: number;
}

const QUEUE_MIN = 3;
const RATING_THRESHOLD = 5;
const FETCH_RETRY_LIMIT = 5;

export function SwipeView({ user, onRecommendationsUnlocked }: Props) {
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [ratingMovie, setRatingMovie] = useState<Movie | null>(null);
  const [toast, setToast] = useState('');
  const [pendingSwipe, setPendingSwipe] = useState<'left' | 'right' | null>(null);
  const [ratingCount, setRatingCount] = useState(0);

  const keyRef = useRef(0);
  const movieBuffer = useRef<Movie[]>([]);
  const pageRef = useRef(0);
  const replenishing = useRef(false);
  const topMovieRef = useRef<Movie | null>(null);
  const seenMovieIds = useRef(new Set<number>());

  topMovieRef.current = queue[0]?.movie ?? null;

  useEffect(() => {
    if (!user) {
      setRatingCount(0);
      return;
    }
    api.getUserRatings(user.id)
      .then(ratings => setRatingCount(ratings.length))
      .catch(() => setRatingCount(0));
  }, [user]);

  const fetchMovie = useCallback(async (): Promise<Movie> => {
    if (user) {
      return api.getNextMovie(user.id);
    }
    if (movieBuffer.current.length === 0) {
      const page = await api.getMovies(pageRef.current % 10, 20);
      movieBuffer.current = [...page.content];
      pageRef.current += 1;
    }
    return movieBuffer.current.shift()!;
  }, [user]);

  const fetchUniqueMovie = useCallback(async (): Promise<Movie> => {
    for (let attempt = 0; attempt < FETCH_RETRY_LIMIT; attempt++) {
      const movie = await fetchMovie();
      if (!seenMovieIds.current.has(movie.id)) {
        seenMovieIds.current.add(movie.id);
        return movie;
      }
    }
    // small pool: accept whatever backend returns
    const movie = await fetchMovie();
    seenMovieIds.current.add(movie.id);
    return movie;
  }, [fetchMovie]);

  useEffect(() => {
    movieBuffer.current = [];
    pageRef.current = 0;
    replenishing.current = false;
    seenMovieIds.current = new Set();
    setLoading(true);
    setQueue([]);
    setPendingSwipe(null);
    setRatingMovie(null);

    let cancelled = false;
    (async () => {
      const items: QueueItem[] = [];
      for (let i = 0; i < QUEUE_MIN; i++) {
        try {
          const movie = await fetchUniqueMovie();
          if (!cancelled) items.push({ movie, key: ++keyRef.current });
        } catch {
          break;
        }
      }
      if (!cancelled) {
        setQueue(items);
        setLoading(false);
      }
    })();

    return () => { cancelled = true; };
  }, [fetchUniqueMovie]);

  const replenish = useCallback(async () => {
    if (replenishing.current) return;
    replenishing.current = true;
    try {
      const movie = await fetchUniqueMovie();
      setQueue(q => [...q, { movie, key: ++keyRef.current }]);
    } catch {
      /* replenish failure is silent */
    } finally {
      replenishing.current = false;
    }
  }, [fetchUniqueMovie]);

  useEffect(() => {
    if (!loading && queue.length < QUEUE_MIN) {
      replenish();
    }
  }, [queue.length, loading, replenish]);

  const dismissTop = useCallback(() => {
    setPendingSwipe(null);
    setQueue(q => q.slice(1));
  }, []);

  const handleSwipe = useCallback((dir: 'left' | 'right') => {
    if (dir === 'left' && user && topMovieRef.current) {
      const discardedId = topMovieRef.current.id;
      seenMovieIds.current.add(discardedId);
      api.discardMovie(discardedId, user.id).catch(() => {});
      // remove any duplicate of this movie already sitting in the queue
      setQueue(q => q.filter((item, i) => i === 0 || item.movie.id !== discardedId));
    }
    if (dir === 'right' && user) {
      setRatingMovie(topMovieRef.current);
    } else if (dir === 'right' && !user) {
      setToast('Select a user to rate movies');
      setTimeout(() => setToast(''), 2500);
    }
  }, [user]);

  const handleAnimationEnd = useCallback(() => {
    dismissTop();
  }, [dismissTop]);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(''), 2500);
  };

  const handleRate = async (rating: number) => {
    if (!ratingMovie || !user) return;
    try {
      await api.createRating(ratingMovie.id, user.id, rating);
      showToast(`Rated "${ratingMovie.title}" ${rating}/10`);
      const newCount = ratingCount + 1;
      setRatingCount(newCount);
      if (newCount === RATING_THRESHOLD) {
        setTimeout(() => {
          setToast('');
          showToast('Recommendations unlocked! Showing your picks…');
          setTimeout(onRecommendationsUnlocked, 1500);
        }, 600);
      }
    } catch {
      showToast('Failed to submit rating');
    }
    setRatingMovie(null);
    setPendingSwipe('right');
  };

  const handleLikeButton = () => {
    if (user) {
      setRatingMovie(topMovieRef.current);
    } else {
      showToast('Select a user to rate movies');
      setPendingSwipe('left');
    }
  };

  const remaining = Math.max(0, RATING_THRESHOLD - ratingCount);
  const alreadyUnlocked = ratingCount >= RATING_THRESHOLD;

  if (loading) return <div className="swipe-loading">Loading movies…</div>;
  if (queue.length === 0) return <div className="empty-state">No more movies to show.</div>;

  return (
    <div className="swipe-container">
      {!user && <p className="swipe-hint">Select a user above to rate movies</p>}
      {user && !alreadyUnlocked && (
        <div className="rating-progress">
          <div className="rating-progress-bar">
            <div
              className="rating-progress-fill"
              style={{ width: `${(ratingCount / RATING_THRESHOLD) * 100}%` }}
            />
          </div>
          <span className="rating-progress-label">
            Rate {remaining} more movie{remaining !== 1 ? 's' : ''} to unlock recommendations
          </span>
        </div>
      )}
      <div className="swipe-stack">
        {queue.slice(0, 3).map(({ movie, key }, i) => (
          <SwipeCard
            key={key}
            movie={movie}
            isTop={i === 0}
            stackIndex={i}
            triggerSwipe={i === 0 ? pendingSwipe : null}
            onSwipe={handleSwipe}
            onAnimationEnd={handleAnimationEnd}
          />
        ))}
      </div>
      <div className="swipe-actions">
        <button
          className="swipe-btn swipe-btn-nope"
          onClick={() => setPendingSwipe('left')}
          title="Haven't watched"
        >
          ✕
        </button>
        <button
          className="swipe-btn swipe-btn-like"
          onClick={handleLikeButton}
          title="Rate this movie"
        >
          ♥
        </button>
      </div>
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
