import { useState } from 'react';
import type { User } from './types';
import { UserSelect } from './components/UserSelect';
import { MovieList } from './components/MovieList';
import { Recommendations } from './components/Recommendations';
import './App.css';

type Tab = 'movies' | 'recommendations';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [tab, setTab] = useState<Tab>('movies');

  return (
    <div className="app">
      <header>
        <h1>Movie Picker</h1>
        <UserSelect selectedUser={user} onSelect={setUser} />
      </header>
      <nav className="tabs">
        <button
          className={tab === 'movies' ? 'active' : ''}
          onClick={() => setTab('movies')}
        >
          All Movies
        </button>
        <button
          className={tab === 'recommendations' ? 'active' : ''}
          onClick={() => setTab('recommendations')}
          disabled={!user}
        >
          Recommendations
        </button>
      </nav>
      <main>
        {tab === 'movies' && <MovieList user={user} />}
        {tab === 'recommendations' && user && <Recommendations user={user} />}
        {tab === 'recommendations' && !user && (
          <p className="empty-state">Select a user to see recommendations.</p>
        )}
      </main>
    </div>
  );
}

export default App;
