import { useState } from 'react';
import type { User } from './types';
import { UserSelect } from './components/UserSelect';
import { SwipeView } from './components/SwipeView';
import { Recommendations } from './components/Recommendations';
import { MyRatings } from './components/MyRatings';

type Tab = 'discover' | 'recommendations' | 'ratings';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [tab, setTab] = useState<Tab>('discover');

  return (
    <div className="app">
      <header>
        <h1>Movie <span>Picker</span></h1>
        <UserSelect selectedUser={user} onSelect={setUser} />
      </header>
      <nav className="tabs">
        <button
          className={tab === 'discover' ? 'active' : ''}
          onClick={() => setTab('discover')}
        >
          Discover
        </button>
        <button
          className={tab === 'recommendations' ? 'active' : ''}
          onClick={() => setTab('recommendations')}
          disabled={!user}
        >
          Recommendations
        </button>
        <button
          className={tab === 'ratings' ? 'active' : ''}
          onClick={() => setTab('ratings')}
          disabled={!user}
        >
          My Ratings
        </button>
      </nav>
      <main>
        {tab === 'discover' && (
          <SwipeView user={user} onRecommendationsUnlocked={() => setTab('recommendations')} />
        )}
        {tab === 'recommendations' && user && <Recommendations user={user} />}
        {tab === 'recommendations' && !user && (
          <p className="empty-state">Select a user to see recommendations.</p>
        )}
        {tab === 'ratings' && user && <MyRatings user={user} />}
        {tab === 'ratings' && !user && (
          <p className="empty-state">Select a user to see ratings.</p>
        )}
      </main>
    </div>
  );
}

export default App;
