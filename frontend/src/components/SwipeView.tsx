import { useCallback, useEffect, useRef, useState } from 'react';
import type { Movie, User } from '../types';
import { api } from '../api';
import { SwipeCard } from './SwipeCard';
import { RatingModal } from './RatingModal';

interface Props {
  user: User | null;
}

interface QueueItem {
  movie: Movie;
  key: number;
}

const QUEUE_MIN = 3;

export function SwipeView({ user }: Props) {
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [ratingMovie, setRatingMovie] = useState<Movie | null>(null);
  const [toast, setToast] = useState('');
  const [pendingSwipe, setPendingSwipe] = useState<'left' | 'right' | null>(null);

  const keyRef = useRef(0);
  const movieBuffer = useRef<Movie[]>([]);
  const pageRef = useRef(0);
  const replenishing = useRef(false);
  const topMovieRef = useRef<Movie | null>(null);

  topMovieRef.current = queue[0]?.movie ?? null;

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

  useEffect(() => {
    movieBuffer.current = [];
    pageRef.current = 0;
    replenishing.current = false;
    setLoading(true);
    setQueue([]);
    setPendingSwipe(null);
    setRatingMovie(null);

    let cancelled = false;
    (async () => {
      const items: QueueItem[] = [];
      for (let i = 0; i < QUEUE_MIN; i++) {
        try {
          const movie = await fetchMovie();
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
  }, [fetchMovie]);

  const replenish = useCallback(async () => {
    if (replenishing.current) return;
    replenishing.current = true;
    try {
      const movie = await fetchMovie();
      setQueue(q => [...q, { movie, key: ++keyRef.current }]);
    } catch {
    } finally {
      replenishing.current = false;
    }
  }, [fetchMovie]);

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
    if (dir === 'right' && user) {
      setRatingMovie(topMovieRef.current);
    } else if (dir === 'right') {
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

  if (loading) return <div className="swipe-loading">Loading movies…</div>;
  if (queue.length === 0) return <div className="empty-state">No more movies to show.</div>;

  return (
    <div className="swipe-container">
      {!user && <p className="swipe-hint">Select a user above to rate movies</p>}
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
          title="Skip"
        >
          ✕
        </button>
        <button
          className="swipe-btn swipe-btn-like"
          onClick={handleLikeButton}
          title="Like"
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
