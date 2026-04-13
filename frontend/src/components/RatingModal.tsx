import { useState } from 'react';

interface Props {
  movieTitle: string;
  onSubmit: (rating: number) => void;
  onClose: () => void;
}

export function RatingModal({ movieTitle, onSubmit, onClose }: Props) {
  const [rating, setRating] = useState(5);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Rate: {movieTitle}</h3>
        <div className="rating-input">
          <input
            type="range"
            min={1}
            max={10}
            value={rating}
            onChange={(e) => setRating(Number(e.target.value))}
          />
          <span className="rating-value">{rating}/10</span>
        </div>
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
