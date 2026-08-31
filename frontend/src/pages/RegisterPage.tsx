import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { register, error, clearError, isAuthenticated } = useAuth();
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    clearError();
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/');
    }
  }, [isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !email || !password) return;
    try {
      setLoading(true);
      await register(name, email, password);
      navigate('/');
    } catch (err) {
      // Handled by AuthContext error state
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh', padding: '1rem' }}>
      <div className="glass-panel" style={{ padding: '2.75rem', width: '100%', maxWidth: '440px', borderColor: 'var(--primary-border)', boxShadow: '0 15px 45px rgba(0,0,0,0.5), 0 0 30px rgba(9, 132, 227, 0.15)' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{ display: 'inline-block', padding: '4px 12px', background: 'var(--secondary-subtle)', border: '1px solid var(--secondary-border)', borderRadius: 'var(--radius-full)', color: 'var(--secondary-light)', fontSize: '0.78rem', fontWeight: 600, letterSpacing: '0.5px', marginBottom: '0.75rem' }}>
            REGISTRATION
          </div>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
            <img src="/logo.png" alt="Logo" style={{ width: '64px', height: '64px', objectFit: 'contain' }} />
          </div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)' }}>Create Account</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
            Get started with AI Project Manager
          </p>
        </div>

        {error && (
          <div style={{
            backgroundColor: 'var(--accent-crimson-subtle)',
            border: '1px solid rgba(214, 48, 49, 0.3)',
            borderRadius: 'var(--radius-sm)',
            padding: '0.85rem 1rem',
            color: '#ff7675',
            fontSize: '0.875rem',
            marginBottom: '1.5rem',
            fontWeight: 500
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label htmlFor="name" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Full Name</label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              style={{
                padding: '0.85rem 1rem',
                fontSize: '0.95rem'
              }}
              placeholder="John Doe"
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label htmlFor="email" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Email Address</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              style={{
                padding: '0.85rem 1rem',
                fontSize: '0.95rem'
              }}
              placeholder="john@example.com"
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <label htmlFor="password" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              style={{
                padding: '0.85rem 1rem',
                fontSize: '0.95rem'
              }}
              placeholder="•••••••• (min 6 chars)"
            />
          </div>

          <button
            type="submit"
            className="btn btn-coral"
            disabled={loading}
            style={{ width: '100%', padding: '0.85rem', marginTop: '0.5rem', opacity: loading ? 0.7 : 1, fontSize: '1rem' }}
          >
            {loading ? 'Creating Account...' : 'Sign Up'}
          </button>
        </form>

        <p style={{ marginTop: '1.75rem', textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary-light)', textDecoration: 'none', fontWeight: 600, transition: 'var(--transition-smooth)' }}>
            ← Back to Sign In
          </Link>
        </p>
      </div>
    </div>
  );
}
