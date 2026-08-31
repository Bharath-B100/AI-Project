import { NavLink, Link } from 'react-router-dom';
import { LayoutDashboard, FolderKanban, CheckSquare, FileBarChart, Settings, ChevronRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const { user } = useAuth();

  const menuItems = [
    { name: 'Dashboard', path: '/',        icon: LayoutDashboard, end: true },
    { name: 'Projects',  path: '/projects', icon: FolderKanban,   end: false },
    { name: 'Tasks',     path: '/tasks',    icon: CheckSquare,    end: false },
    { name: 'Reports',   path: '/reports',  icon: FileBarChart,   end: false },
    { name: 'Settings',  path: '/settings', icon: Settings,       end: false },
  ];

  const initials = user
    ? user.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2)
    : 'PM';

  return (
    <aside style={{
      width: 'var(--sidebar-width)',
      backgroundColor: 'var(--bg-secondary)',
      borderRight: '1px solid var(--border-light)',
      display: 'flex',
      flexDirection: 'column',
      height: '100%',        // fills the 100vh flex parent — never scrolls with content
      flexShrink: 0,         // never squeeze the sidebar narrower
      overflowY: 'hidden',   // outer sidebar never scrolls
    }}>
      {/* Logo */}
      <div style={{
        height: 'var(--header-height)',
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '0 1.5rem',
        borderBottom: '1px solid var(--border-light)',
      }}>
        <div style={{ padding: '4px', display: 'flex' }}>
          <img src="/logo.png" alt="Logo" style={{ width: '36px', height: '36px', objectFit: 'contain' }} />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: '1.05rem', letterSpacing: '-0.3px', color: 'var(--text-primary)' }}>
            AI Project Manager
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--secondary)', fontWeight: 600, letterSpacing: '0.5px' }}>
            INTELLIGENT CPM
          </div>
        </div>
      </div>

      {/* Navigation — scrolls internally if screen is too short */}
      <nav style={{ flex: 1, padding: '1.5rem 1rem', display: 'flex', flexDirection: 'column', gap: '4px', overflowY: 'auto' }}>
        {menuItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.name}
              to={item.path}
              end={item.end}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '10px 14px',
                borderRadius: 'var(--radius-sm)',
                color: isActive ? 'var(--primary)' : 'var(--text-secondary)',
                backgroundColor: isActive ? 'var(--primary-subtle)' : 'transparent',
                border: isActive ? '1px solid var(--primary-border)' : '1px solid transparent',
                textDecoration: 'none',
                fontWeight: isActive ? 700 : 500,
                fontSize: '0.92rem',
                transition: 'var(--transition-smooth)',
                boxShadow: isActive ? '0 2px 8px rgba(9, 132, 227, 0.08)' : 'none',
              })}
            >
              {({ isActive }) => (
                <>
                  <Icon size={18} style={{ color: isActive ? 'var(--primary)' : 'var(--text-muted)' }} />
                  <span style={{ flex: 1 }}>{item.name}</span>
                  {isActive && <ChevronRight size={13} style={{ opacity: 0.5 }} />}
                </>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* User profile strip at bottom */}
      <div style={{ borderTop: '1px solid var(--border-light)', padding: '12px 14px' }}>
        <Link
          to="/settings"
          style={{
            display: 'flex', alignItems: 'center', gap: 10,
            padding: '8px 10px', borderRadius: 'var(--radius-sm)',
            textDecoration: 'none', transition: 'var(--transition-smooth)',
            background: 'transparent', border: '1px solid transparent',
          }}
          onMouseEnter={e => {
            (e.currentTarget as HTMLElement).style.background = 'var(--primary-subtle)';
            (e.currentTarget as HTMLElement).style.borderColor = 'var(--primary-border)';
          }}
          onMouseLeave={e => {
            (e.currentTarget as HTMLElement).style.background = 'transparent';
            (e.currentTarget as HTMLElement).style.borderColor = 'transparent';
          }}
        >
          <div style={{
            width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
            background: 'linear-gradient(135deg, var(--primary), var(--primary-light))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '0.8rem', fontWeight: 800, color: '#fff',
            boxShadow: '0 2px 8px var(--primary-glow)',
          }}>
            {initials}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '0.83rem', fontWeight: 700, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {user?.name || 'Guest'}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>View Settings</div>
          </div>
          <Settings size={14} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
        </Link>
      </div>
    </aside>
  );
}
