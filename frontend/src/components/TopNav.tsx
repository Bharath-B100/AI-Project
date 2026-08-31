import { Bell, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function TopNav() {
  const { user, logout } = useAuth();
  
  const initials = user 
    ? user.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) 
    : 'PM';
  const displayName = user ? user.name : 'Project Manager';

  return (
    <header style={{
      height: 'var(--header-height)',
      borderBottom: '1px solid var(--border-light)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 2rem',
      backgroundColor: 'var(--bg-primary)',
      flexShrink: 0,   // never shrink below its natural height
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>System Active:</span>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          background: 'var(--primary-subtle)',
          border: '1px solid var(--primary-border)',
          padding: '3px 10px',
          borderRadius: 'var(--radius-full)',
          fontSize: '0.75rem',
          color: 'var(--primary)',
          fontWeight: 700
        }}>
          <span style={{ width: '7px', height: '7px', backgroundColor: 'var(--accent-emerald)', borderRadius: '50%', boxShadow: '0 0 6px var(--accent-emerald)' }}></span>
          Intelligent Engine
        </div>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '5px',
          background: 'var(--secondary-subtle)',
          border: '1px solid var(--secondary-border)',
          padding: '3px 10px',
          borderRadius: 'var(--radius-full)',
          fontSize: '0.75rem',
          color: 'var(--secondary)',
          fontWeight: 700
        }}>
          CPM Online
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        <button 
          style={{
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border-light)',
            color: 'var(--text-secondary)',
            cursor: 'pointer',
            display: 'flex',
            padding: '8px',
            borderRadius: 'var(--radius-sm)',
            transition: 'var(--transition-smooth)'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = 'var(--primary)';
            e.currentTarget.style.borderColor = 'var(--primary)';
            e.currentTarget.style.background = 'var(--primary-subtle)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = 'var(--text-secondary)';
            e.currentTarget.style.borderColor = 'var(--border-light)';
            e.currentTarget.style.background = 'var(--bg-secondary)';
          }}
        >
          <Bell size={18} />
        </button>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          borderLeft: '1px solid var(--border-light)',
          paddingLeft: '1.5rem'
        }}>
          <div style={{
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: '0.85rem',
            color: 'white',
            boxShadow: '0 2px 10px var(--primary-glow)'
          }}>
            {initials}
          </div>
          <div>
            <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{displayName}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Project Lead</div>
          </div>
          <button 
            onClick={logout} 
            title="Log Out"
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-secondary)',
              cursor: 'pointer',
              display: 'flex',
              marginLeft: '0.5rem',
              padding: '6px',
              borderRadius: '6px',
              transition: 'var(--transition-smooth)'
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#ff7675')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-secondary)')}
          >
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </header>
  );
}
