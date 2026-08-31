import { useState, useEffect, FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Eye, EyeOff, Lock, Mail, ShieldAlert } from 'lucide-react';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  
  // Security states
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [lockoutSeconds, setLockoutSeconds] = useState(0);
  const [validationError, setValidationError] = useState('');

  const { login, error, clearError, isAuthenticated } = useAuth();
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

  // Lockout timer countdown
  useEffect(() => {
    if (lockoutSeconds > 0) {
      const timer = setTimeout(() => setLockoutSeconds(lockoutSeconds - 1), 1000);
      return () => clearTimeout(timer);
    } else if (lockoutSeconds === 0 && failedAttempts >= 3) {
      // Reset failed attempts after lockout period
      setFailedAttempts(0);
    }
  }, [lockoutSeconds, failedAttempts]);

  const validateEmail = (emailStr: string) => {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(emailStr);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');
    clearError();

    if (lockoutSeconds > 0) return;

    if (!email || !password) {
      setValidationError('Please fill in all fields.');
      return;
    }

    if (!validateEmail(email)) {
      setValidationError('Please enter a valid email address.');
      return;
    }

    try {
      setLoading(true);
      await login(email, password, rememberMe);
      navigate('/');
    } catch (err) {
      const newAttempts = failedAttempts + 1;
      setFailedAttempts(newAttempts);
      if (newAttempts >= 3) {
        setLockoutSeconds(30); // 30-second lockout after 3 failed attempts
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', width: '100%', backgroundColor: 'var(--bg-primary)' }}>
      {/* Left Panel: Branding & Premium Illustration */}
      <div style={{
        flex: 1,
        display: 'flex',
        background: 'linear-gradient(135deg, var(--primary) 0%, #0c2461 100%)',
        flexDirection: 'column',
        justifyContent: 'space-between',
        padding: '4rem',
        color: 'white',
        position: 'relative',
        overflow: 'hidden'
      }} className="login-left-panel">
        
        {/* Abstract Background Shapes */}
        <div style={{ position: 'absolute', top: '-10%', right: '-10%', width: '500px', height: '500px', borderRadius: '50%', background: 'radial-gradient(circle, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 70%)' }} />
        <div style={{ position: 'absolute', bottom: '-20%', left: '-10%', width: '600px', height: '600px', borderRadius: '50%', background: 'radial-gradient(circle, rgba(56, 189, 248, 0.15) 0%, rgba(56, 189, 248, 0) 70%)' }} />
        
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
            <img src="/logo.png" alt="Logo" style={{ width: '48px', height: '48px', filter: 'brightness(0) invert(1)' }} />
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.5px', margin: 0 }}>AI Project Manager</h1>
          </div>
          
          <div style={{ marginTop: '30vh', maxWidth: '480px' }}>
            <h2 style={{ fontSize: '3rem', fontWeight: 800, lineHeight: 1.1, marginBottom: '1.5rem', letterSpacing: '-1px' }}>
              Manage your projects with intelligent precision.
            </h2>
            <p style={{ fontSize: '1.1rem', color: 'rgba(255,255,255,0.8)', lineHeight: 1.6, fontWeight: 400 }}>
              Leverage the Critical Path Method, proactive risk detection, and intelligent resource allocation to deliver on time, every time.
            </p>
          </div>
        </div>
        
        <div style={{ position: 'relative', zIndex: 1, fontSize: '0.9rem', color: 'rgba(255,255,255,0.6)' }}>
          © {new Date().getFullYear()} AI Project Manager. All rights reserved.
        </div>
      </div>

      {/* Right Panel: Login Form */}
      <div style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        padding: '3rem',
        maxWidth: '600px',
        margin: '0 auto',
        width: '100%'
      }}>
        
        <div style={{ width: '100%', maxWidth: '420px', margin: '0 auto' }}>
          {/* Mobile Logo */}
          <div className="mobile-logo-wrapper" style={{ display: 'none', justifyContent: 'center', marginBottom: '2rem' }}>
            <img src="/logo.png" alt="Logo" style={{ width: '64px', height: '64px', objectFit: 'contain' }} />
          </div>

          <div style={{ marginBottom: '2.5rem' }}>
            <h2 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              Welcome back
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
              Please enter your details to sign in to your account.
            </p>
          </div>

          {(error || validationError) && !lockoutSeconds && (
            <div style={{
              backgroundColor: 'var(--accent-crimson-subtle)',
              border: '1px solid rgba(214, 48, 49, 0.3)',
              borderRadius: 'var(--radius-sm)',
              padding: '1rem',
              color: 'var(--accent-crimson)',
              fontSize: '0.9rem',
              marginBottom: '1.5rem',
              fontWeight: 500,
              display: 'flex',
              alignItems: 'flex-start',
              gap: '10px'
            }}>
              <ShieldAlert size={18} style={{ marginTop: '2px', flexShrink: 0 }} />
              <div>{validationError || error}</div>
            </div>
          )}

          {lockoutSeconds > 0 && (
            <div style={{
              backgroundColor: '#fff3cd',
              border: '1px solid #ffeeba',
              borderRadius: 'var(--radius-sm)',
              padding: '1rem',
              color: '#856404',
              fontSize: '0.9rem',
              marginBottom: '1.5rem',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}>
              <ShieldAlert size={20} />
              Too many failed attempts. Try again in {lockoutSeconds}s.
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            
            {/* Email Field */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', position: 'relative' }}>
              <label htmlFor="email" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Email Address</label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <Mail size={18} style={{ position: 'absolute', left: '14px', color: 'var(--text-muted)' }} />
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={lockoutSeconds > 0 || loading}
                  required
                  style={{
                    width: '100%',
                    padding: '0.85rem 1rem 0.85rem 40px',
                    fontSize: '0.95rem',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-medium)',
                    backgroundColor: lockoutSeconds > 0 ? 'var(--bg-surface)' : '#fff'
                  }}
                  placeholder="demo@example.com"
                />
              </div>
            </div>

            {/* Password Field */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', position: 'relative' }}>
              <label htmlFor="password" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Password</label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <Lock size={18} style={{ position: 'absolute', left: '14px', color: 'var(--text-muted)' }} />
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={lockoutSeconds > 0 || loading}
                  required
                  style={{
                    width: '100%',
                    padding: '0.85rem 40px 0.85rem 40px',
                    fontSize: '0.95rem',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-medium)',
                    backgroundColor: lockoutSeconds > 0 ? 'var(--bg-surface)' : '#fff'
                  }}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                  style={{
                    position: 'absolute',
                    right: '10px',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    padding: '4px',
                    display: 'flex'
                  }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Remember Me & Forgot Password */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.88rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                <input 
                  type="checkbox" 
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  disabled={lockoutSeconds > 0 || loading}
                  style={{ width: '16px', height: '16px', accentColor: 'var(--primary)', cursor: 'pointer' }}
                />
                Remember me
              </label>
              
              <a href="#" onClick={(e) => { e.preventDefault(); alert('Password reset flow not implemented in this demo.'); }} style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--primary)', textDecoration: 'none' }}>
                Forgot password?
              </a>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="btn btn-primary"
              disabled={lockoutSeconds > 0 || loading}
              style={{ 
                width: '100%', 
                padding: '0.9rem', 
                marginTop: '0.5rem', 
                opacity: (loading || lockoutSeconds > 0) ? 0.7 : 1, 
                fontSize: '1rem',
                justifyContent: 'center',
                boxShadow: (loading || lockoutSeconds > 0) ? 'none' : '0 8px 20px rgba(9, 132, 227, 0.25)'
              }}
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>
          </form>

          <p style={{ marginTop: '2.5rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
            Don't have an account?{' '}
            <Link to="/register" style={{ color: 'var(--primary)', textDecoration: 'none', fontWeight: 700, transition: 'var(--transition-smooth)' }}>
              Create an account
            </Link>
          </p>
        </div>
      </div>
      
      {/* Add a tiny style injection for responsive layout if needed */}
      <style>{`
        @media (max-width: 768px) {
          .login-left-panel { display: none !important; }
          .mobile-logo-wrapper { display: flex !important; }
        }
      `}</style>
    </div>
  );
}
