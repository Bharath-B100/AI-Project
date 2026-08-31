import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { setAuthToken, registerUnauthorizedCallback, profileApi } from '../services/api';

export interface User {
  id: number;
  name: string;
  email: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  loading: boolean;                                        // true while validating token on boot
  login: (email: string, password: string, rememberMe?: boolean) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  updateUser: (updated: Partial<User>) => void;
  error: string | null;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // ── Read persisted session ─────────────────────────────────────────────────
  const [token, setToken] = useState<string | null>(() => {
    const t = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
    if (t) setAuthToken(t);
    return t;
  });

  const [user, setUser] = useState<User | null>(() => {
    const u = localStorage.getItem('user') || sessionStorage.getItem('user');
    return u ? JSON.parse(u) : null;
  });

  const [error,   setError]   = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);   // starts true

  // ── Logout clears everything ───────────────────────────────────────────────
  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    setError(null);
    setAuthToken(null);
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('user');
    sessionStorage.removeItem('jwtToken');
    sessionStorage.removeItem('user');
  }, []);

  // Wire the 401 interceptor to the logout function
  useEffect(() => { registerUnauthorizedCallback(logout); }, [logout]);

  // ── Validate token on app boot: sync user from backend ────────────────────
  useEffect(() => {
    const activeToken = token || localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
    if (!activeToken) {
      setLoading(false);
      return;
    }

    setAuthToken(activeToken);

    // Sync latest user metadata in the background
    profileApi.getMe()
      .then(profile => {
        const freshUser: User = { id: profile.id, name: profile.name, email: profile.email };
        setUser(freshUser);
        const storage = localStorage.getItem('jwtToken') ? localStorage : sessionStorage;
        storage.setItem('user', JSON.stringify(freshUser));
      })
      .catch((err) => {
        // Only if the token is truly invalid/expired (401) do we clear the session
        if (err?.response?.status === 401) {
          logout();
        }
        // If it's a network glitch or server restart, keep the valid session from storage!
      })
      .finally(() => setLoading(false));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // run once on mount only

  // ── Login ──────────────────────────────────────────────────────────────────
  const login = async (email: string, password: string, rememberMe = true) => {
    try {
      setError(null);
      const { data } = await axios.post('/api/v1/auth/login', { email, password });
      const storage = rememberMe ? localStorage : sessionStorage;
      storage.setItem('jwtToken', data.token);
      storage.setItem('user', JSON.stringify(data.user));
      setAuthToken(data.token);
      setToken(data.token);
      setUser(data.user);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
      throw err;
    }
  };

  // ── Register ───────────────────────────────────────────────────────────────
  const register = async (name: string, email: string, password: string) => {
    try {
      setError(null);
      const { data } = await axios.post('/api/v1/auth/register', { name, email, password });
      localStorage.setItem('jwtToken', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
      setAuthToken(data.token);
      setToken(data.token);
      setUser(data.user);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
      throw err;
    }
  };

  // ── Update user in state AND whichever storage holds the session ───────────
  const updateUser = (updated: Partial<User>) => {
    setUser(prev => {
      if (!prev) return prev;
      const next = { ...prev, ...updated };
      if (localStorage.getItem('jwtToken')) localStorage.setItem('user', JSON.stringify(next));
      else sessionStorage.setItem('user', JSON.stringify(next));
      return next;
    });
  };

  const clearError = () => setError(null);

  return (
    <AuthContext.Provider value={{
      isAuthenticated: !!token,
      user, token, loading,
      login, register, logout, updateUser,
      error, clearError,
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
};
