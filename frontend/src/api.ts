import type { Movie, User, Rating, Page } from './types';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.json();
}

export const api = {
  getMovies: (page = 0, size = 20) =>
    request<Page<Movie>>(`/api/movies?page=${page}&size=${size}`),

  getUsers: () => request<User[]>('/api/users'),

  createUser: (email: string) =>
    request<User>('/api/users', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  createRating: (movieId: number, userId: number, rating: number) =>
    request<Rating>('/api/ratings', {
      method: 'POST',
      body: JSON.stringify({ movieId, userId, rating }),
    }),

  getUserRatings: (userId: number) =>
    request<Rating[]>(`/api/ratings/user/${userId}`),

  getRecommendations: (userId: number) =>
    request<Movie[]>(`/recommendations/${userId}`),

  getNextMovie: (userId: number) =>
    request<Movie>(`/recommendations/movies/next/${userId}`),
};
