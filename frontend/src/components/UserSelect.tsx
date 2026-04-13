import { useState, useEffect } from 'react';
import type { User } from '../types';
import { api } from '../api';

interface Props {
  selectedUser: User | null;
  onSelect: (user: User) => void;
}

export function UserSelect({ selectedUser, onSelect }: Props) {
  const [users, setUsers] = useState<User[]>([]);
  const [email, setEmail] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getUsers().then(setUsers).catch(() => setError('Failed to load users'));
  }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      const user = await api.createUser(email);
      setUsers((prev) => [...prev, user]);
      onSelect(user);
      setEmail('');
      setShowCreate(false);
    } catch {
      setError('Failed to create user');
    }
  }

  return (
    <div className="user-select">
      <label htmlFor="user-picker">User:</label>
      <select
        id="user-picker"
        value={selectedUser?.id ?? ''}
        onChange={(e) => {
          const user = users.find((u) => u.id === Number(e.target.value));
          if (user) onSelect(user);
        }}
      >
        <option value="">Select a user</option>
        {users.map((u) => (
          <option key={u.id} value={u.id}>
            {u.email}
          </option>
        ))}
      </select>
      <button className="btn btn-small" onClick={() => setShowCreate(!showCreate)}>
        {showCreate ? 'Cancel' : '+ New User'}
      </button>
      {showCreate && (
        <form onSubmit={handleCreate} className="create-user-form">
          <input
            type="email"
            placeholder="email@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <button type="submit" className="btn btn-primary">
            Create
          </button>
        </form>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
