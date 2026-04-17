export interface Movie {
  id: number;
  title: string;
  description: string;
  genre: string;
  posterUrl: string;
}

export interface RecommendedMovie extends Movie {
  clusterIndex: number;
}

export interface TasteProfile {
  clusterIndex: number;
  genres: string[];
  movieCount: number;
}

export interface User {
  id: number;
  email: string;
}

export interface Rating {
  id: number;
  movieId: number;
  userId: number;
  rating: number;
  createdAt: string;
  movieTitle: string;
  posterUrl: string;
  genre: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
