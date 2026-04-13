export interface Movie {
  id: number;
  title: string;
  description: string;
  genre: string;
  posterUrl: string;
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
}
