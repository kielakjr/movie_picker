import { useState } from 'react';

interface Props {
  movieTitle: string;
  onSubmit: (rating: number) => void;
  onClose: () => void;
}

const LABELS: Record<number, string> = {
  1: 'Terrible', 2: 'Very Bad',
  3: 'Bad',      4: 'Poor',
  5: 'Mediocre', 6: 'Decent',
  7: 'Good',     8: 'Great',
  9: 'Excellent', 10: 'Masterpiece',
};

export function RatingModal({ movieTitle, onSubmit, onClose }: Props) {
  const [rating, setRating] = useState(5);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Rate: {movieTitle}</h3>
        <div className="rating-numbers">
          {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              className={`rating-num-btn${n <= rating ? ' selected' : ''}`}
              onClick={() => setRating(n)}
            >
              {n}
            </button>
          ))}
        </div>
        <p className="rating-description">{LABELS[rating]}</p>
        <div className="modal-actions">
          <button className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" onClick={() => onSubmit(rating)}>
            Submit
          </button>
        </div>
      </div>
    </div>
  );
}
