import { useEffect, useRef, useState } from 'react';
import type { Movie } from '../types';

interface Props {
  movie: Movie;
  isTop: boolean;
  stackIndex: number;
  triggerSwipe: 'left' | 'right' | null;
  onSwipe: (dir: 'left' | 'right') => void;
  onAnimationEnd: () => void;
}

const THRESHOLD = 100;

export function SwipeCard({ movie, isTop, stackIndex, triggerSwipe, onSwipe, onAnimationEnd }: Props) {
  const [drag, setDrag] = useState({ x: 0, y: 0, active: false });
  const [dragFlying, setDragFlying] = useState<'left' | 'right' | null>(null);
  const startRef = useRef({ x: 0, y: 0 });
  const alreadyFlying = useRef(false);

  useEffect(() => {
    if (!triggerSwipe || alreadyFlying.current) return;
    alreadyFlying.current = true;
    const id = setTimeout(onAnimationEnd, 350);
    return () => clearTimeout(id);
  }, [triggerSwipe, onAnimationEnd]);

  const handlePointerDown = (e: React.PointerEvent) => {
    if (!isTop || alreadyFlying.current) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    startRef.current = { x: e.clientX, y: e.clientY };
    setDrag({ x: 0, y: 0, active: true });
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!drag.active) return;
    setDrag({ x: e.clientX - startRef.current.x, y: e.clientY - startRef.current.y, active: true });
  };

  const handlePointerUp = () => {
    if (!drag.active) return;
    const { x } = drag;
    setDrag({ x: 0, y: 0, active: false });
    if (alreadyFlying.current) return;

    if (x > THRESHOLD) {
      onSwipe('right');
    } else if (x < -THRESHOLD) {
      alreadyFlying.current = true;
      setDragFlying('left');
      onSwipe('left');
      setTimeout(onAnimationEnd, 350);
    }
  };

  const flyDir = dragFlying ?? triggerSwipe;

  let transform: string;
  if (flyDir === 'right') {
    transform = 'translateX(150vw) rotate(30deg)';
  } else if (flyDir === 'left') {
    transform = 'translateX(-150vw) rotate(-30deg)';
  } else if (drag.active) {
    transform = `translate(${drag.x}px, ${drag.y}px) rotate(${drag.x * 0.08}deg)`;
  } else if (stackIndex === 1) {
    transform = 'scale(0.95) translateY(16px)';
  } else if (stackIndex === 2) {
    transform = 'scale(0.90) translateY(32px)';
  } else {
    transform = 'none';
  }

  const transition = drag.active
    ? 'none'
    : flyDir
    ? 'transform 0.35s ease-in'
    : 'transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)';

  const likeOpacity = drag.active
    ? Math.min(1, Math.max(0, drag.x / THRESHOLD))
    : flyDir === 'right' ? 1 : 0;
  const nopeOpacity = drag.active
    ? Math.min(1, Math.max(0, -drag.x / THRESHOLD))
    : flyDir === 'left' ? 1 : 0;

  return (
    <div
      className="swipe-card"
      style={{
        transform,
        transition,
        zIndex: 10 - stackIndex,
        cursor: isTop ? (drag.active ? 'grabbing' : 'grab') : 'default',
      }}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
    >
      {isTop && (
        <>
          <div className="swipe-stamp swipe-like" style={{ opacity: likeOpacity }}>LIKE</div>
          <div className="swipe-stamp swipe-nope" style={{ opacity: nopeOpacity }}>NOPE</div>
        </>
      )}
      {movie.posterUrl ? (
        <img className="swipe-poster" src={movie.posterUrl} alt={movie.title} draggable={false} />
      ) : (
        <div className="swipe-poster swipe-poster-placeholder">No Image</div>
      )}
      <div className="swipe-card-info">
        <h2 className="swipe-title">{movie.title}</h2>
        <span className="genre-badge">{movie.genre}</span>
        <p className="movie-description">{movie.description}</p>
      </div>
    </div>
  );
}
