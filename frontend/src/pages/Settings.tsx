import { useState, useEffect, FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { profileApi } from '../services/api';
import {
  User, Lock, Bell, Moon, Sun, Monitor,
  Shield, LogOut, Save, Eye, EyeOff,
  CheckCircle2, AlertCircle, ChevronRight,
  Palette, Settings as SettingsIcon,
} from 'lucide-react';

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
type Tab = 'profile' | 'security' | 'appearance' | 'notifications';

function SectionCard({ title, icon, children }: {
  title: string; icon: React.ReactNode; children: React.ReactNode;
}) {
  return (
    <div style={{ background: 'var(--bg-card)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', overflow: 'hidden', boxShadow: 'var(--shadow-sm)' }}>
      <div style={{ padding: '14px 20px', borderBottom: '1px solid var(--border-light)', display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ color: 'var(--primary)', display: 'flex' }}>{icon}</div>
        <h3 style={{ fontSize: '0.92rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>{title}</h3>
      </div>
      <div style={{ padding: '20px' }}>{children}</div>
    </div>
  );
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 20 }}>
      <label style={{ display: 'block', fontSize: '0.83rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>{label}</label>
      {children}
      {hint && <p style={{ margin: '5px 0 0', fontSize: '0.75rem', color: 'var(--text-muted)' }}>{hint}</p>}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '9px 12px', fontSize: '0.88rem',
  border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-xs)',
  background: 'var(--bg-secondary)', color: 'var(--text-primary)',
  boxSizing: 'border-box', outline: 'none', transition: 'border-color 0.15s',
};

function Toast({ msg, type, onClose }: { msg: string; type: 'success' | 'error'; onClose: () => void }) {
  useEffect(() => { const t = setTimeout(onClose, 3500); return () => clearTimeout(t); }, [onClose]);
  return (
    <div style={{
      position: 'fixed', bottom: 24, right: 24, zIndex: 9999,
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '12px 18px', borderRadius: 'var(--radius-sm)',
      background: type === 'success' ? '#ecfdf5' : '#fef2f2',
      border: `1px solid ${type === 'success' ? '#6ee7b7' : '#fca5a5'}`,
      color: type === 'success' ? '#059669' : '#dc2626',
      boxShadow: 'var(--shadow-lg)', fontSize: '0.88rem', fontWeight: 600,
      animation: 'fadeInUp 0.2s ease',
    }}>
      {type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
      {msg}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Tab
// ─────────────────────────────────────────────────────────────────────────────
function ProfileTab() {
  const { user, updateUser } = useAuth();
  const [name, setName]       = useState(user?.name ?? '');
  const [saving, setSaving]   = useState(false);
  const [toast, setToast]     = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  // Sync if user changes externally
  useEffect(() => { setName(user?.name ?? ''); }, [user?.name]);

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    try {
      const updated = await profileApi.updateName(name.trim());
      updateUser({ name: updated.name });
      setToast({ msg: 'Profile updated successfully!', type: 'success' });
    } catch (err: any) {
      setToast({ msg: err.response?.data?.message || 'Failed to update profile.', type: 'error' });
    } finally { setSaving(false); }
  };

  const initials = (user?.name ?? 'U').split(' ').map(p => p[0]).join('').toUpperCase().slice(0, 2);

  return (
    <>
      {toast && <Toast msg={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
      <SectionCard title="Personal Information" icon={<User size={16} />}>
        {/* Avatar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24, padding: '16px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)' }}>
          <div style={{
            width: 64, height: 64, borderRadius: '50%', flexShrink: 0,
            background: 'linear-gradient(135deg, var(--primary), var(--primary-hover))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '1.4rem', fontWeight: 800, color: '#fff', letterSpacing: '-1px',
            boxShadow: '0 4px 16px var(--primary-glow)',
          }}>
            {initials}
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--text-primary)' }}>{user?.name}</div>
            <div style={{ fontSize: '0.83rem', color: 'var(--text-secondary)', marginTop: 2 }}>{user?.email}</div>
            <div style={{ fontSize: '0.73rem', color: 'var(--text-muted)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
              <Shield size={11} /> Account ID: {user?.id}
            </div>
          </div>
        </div>

        <form onSubmit={handleSave}>
          <Field label="Display Name" hint="Your name shown across the app and in reports.">
            <input
              id="settings-name"
              value={name}
              onChange={e => setName(e.target.value)}
              required minLength={2} maxLength={120}
              style={inputStyle}
              placeholder="Your full name"
            />
          </Field>
          <Field label="Email Address" hint="Email cannot be changed here. Contact support to update.">
            <input
              value={user?.email ?? ''}
              disabled
              style={{ ...inputStyle, opacity: 0.55, cursor: 'not-allowed' }}
            />
          </Field>
          <button
            type="submit" disabled={saving || !name.trim() || name.trim() === user?.name}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 7,
              padding: '9px 22px', fontSize: '0.88rem', fontWeight: 700,
              background: 'var(--primary)', color: '#fff', border: 'none',
              borderRadius: 'var(--radius-xs)', cursor: saving ? 'wait' : 'pointer',
              opacity: (!name.trim() || name.trim() === user?.name) ? 0.5 : 1,
              transition: 'opacity 0.15s',
            }}>
            <Save size={14} /> {saving ? 'Saving…' : 'Save Changes'}
          </button>
        </form>
      </SectionCard>

      {/* Account metadata */}
      <SectionCard title="Account Details" icon={<Shield size={16} />}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {[
            { label: 'Account Type',    value: 'Project Lead' },
            { label: 'Account ID',      value: `#${user?.id ?? '—'}` },
            { label: 'Email',           value: user?.email ?? '—' },
            { label: 'Session Storage', value: localStorage.getItem('jwtToken') ? 'Persistent (Remember Me)' : 'Session only' },
          ].map(r => (
            <div key={r.label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: 'var(--bg-secondary)', borderRadius: 6, border: '1px solid var(--border-light)' }}>
              <span style={{ fontSize: '0.83rem', color: 'var(--text-secondary)', fontWeight: 500 }}>{r.label}</span>
              <span style={{ fontSize: '0.83rem', fontWeight: 700, color: 'var(--text-primary)' }}>{r.value}</span>
            </div>
          ))}
        </div>
      </SectionCard>
    </>
  );
}

function PasswordInput({
  id, value, onChange, show, onToggle, placeholder,
}: {
  id: string; value: string; onChange: (v: string) => void;
  show: boolean; onToggle: () => void; placeholder: string;
}) {
  return (
    <div style={{ position: 'relative' }}>
      <input
        id={id} type={show ? 'text' : 'password'}
        value={value} onChange={e => onChange(e.target.value)}
        placeholder={placeholder} style={{ ...inputStyle, paddingRight: 42 }}
        required autoComplete="off"
      />
      <button type="button" onClick={onToggle}
        style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', padding: 4 }}>
        {show ? <EyeOff size={15} /> : <Eye size={15} />}
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Security Tab
// ─────────────────────────────────────────────────────────────────────────────
function SecurityTab() {
  const { logout } = useAuth();
  const [cur,  setCur]   = useState('');
  const [nw,   setNw]    = useState('');
  const [conf, setConf]  = useState('');
  const [showCur,  setShowCur]  = useState(false);
  const [showNw,   setShowNw]   = useState(false);
  const [showConf, setShowConf] = useState(false);
  const [saving,   setSaving]   = useState(false);
  const [toast,    setToast]    = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  const strength = (p: string): { score: number; label: string; color: string } => {
    let s = 0;
    if (p.length >= 6)  s++;
    if (p.length >= 10) s++;
    if (/[A-Z]/.test(p)) s++;
    if (/[0-9]/.test(p)) s++;
    if (/[^A-Za-z0-9]/.test(p)) s++;
    if (s <= 1) return { score: s, label: 'Weak',   color: '#dc2626' };
    if (s <= 3) return { score: s, label: 'Fair',   color: '#d97706' };
    return             { score: s, label: 'Strong', color: '#059669' };
  };
  const str = strength(nw);

  const handleChange = async (e: FormEvent) => {
    e.preventDefault();
    if (nw !== conf) { setToast({ msg: 'New passwords do not match.', type: 'error' }); return; }
    if (nw.length < 6) { setToast({ msg: 'New password must be at least 6 characters.', type: 'error' }); return; }
    setSaving(true);
    try {
      const res = await profileApi.changePassword(cur, nw);
      setToast({ msg: res.message || 'Password changed! Please log in again.', type: 'success' });
      setCur(''); setNw(''); setConf('');
      setTimeout(logout, 2500);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to change password.';
      setToast({ msg, type: 'error' });
    } finally { setSaving(false); }
  };

  return (
    <>
      {toast && <Toast msg={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
      <SectionCard title="Change Password" icon={<Lock size={16} />}>
        <form onSubmit={handleChange}>
          <Field label="Current Password">
            <PasswordInput id="s-cur" value={cur} onChange={setCur} show={showCur} onToggle={() => setShowCur(p => !p)} placeholder="Enter current password" />
          </Field>
          <Field label="New Password">
            <PasswordInput id="s-new" value={nw} onChange={setNw} show={showNw} onToggle={() => setShowNw(p => !p)} placeholder="Enter new password (min 6 chars)" />
            {nw.length > 0 && (
              <div style={{ marginTop: 8 }}>
                <div style={{ display: 'flex', gap: 4, marginBottom: 4 }}>
                  {[1, 2, 3, 4, 5].map(i => (
                    <div key={i} style={{ flex: 1, height: 4, borderRadius: 99, background: i <= str.score ? str.color : 'var(--border-light)', transition: 'background 0.3s' }} />
                  ))}
                </div>
                <span style={{ fontSize: '0.72rem', color: str.color, fontWeight: 700 }}>{str.label}</span>
              </div>
            )}
          </Field>
          <Field label="Confirm New Password">
            <PasswordInput id="s-conf" value={conf} onChange={setConf} show={showConf} onToggle={() => setShowConf(p => !p)} placeholder="Repeat new password" />
            {conf.length > 0 && nw !== conf && (
              <p style={{ margin: '5px 0 0', fontSize: '0.75rem', color: '#dc2626' }}>Passwords do not match</p>
            )}
          </Field>
          <div style={{ padding: '10px 14px', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 6, fontSize: '0.78rem', color: '#92400e', marginBottom: 18 }}>
            ⚠ Changing your password will log you out of all sessions.
          </div>
          <button type="submit" disabled={saving || !cur || !nw || !conf}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 7, padding: '9px 22px', fontSize: '0.88rem', fontWeight: 700, background: '#dc2626', color: '#fff', border: 'none', borderRadius: 'var(--radius-xs)', cursor: saving ? 'wait' : 'pointer', opacity: (!cur || !nw || !conf) ? 0.5 : 1 }}>
            <Lock size={14} /> {saving ? 'Changing…' : 'Change Password'}
          </button>
        </form>
      </SectionCard>

      <SectionCard title="Session Management" icon={<Shield size={16} />}>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 16 }}>
          Signing out will remove your JWT token from this device and redirect you to the login page.
        </p>
        <button
          onClick={logout}
          style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '9px 22px', fontSize: '0.88rem', fontWeight: 700, background: '#fef2f2', border: '1px solid #fca5a5', color: '#dc2626', borderRadius: 'var(--radius-xs)', cursor: 'pointer' }}>
          <LogOut size={14} /> Sign Out
        </button>
      </SectionCard>
    </>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Appearance Tab
// ─────────────────────────────────────────────────────────────────────────────
type Theme = 'light' | 'dark' | 'system';
type Density = 'compact' | 'comfortable' | 'spacious';
type AccentColor = '#2563eb' | '#7c3aed' | '#059669' | '#dc2626' | '#d97706';

function AppearanceTab() {
  const [theme,   setTheme]   = useState<Theme>(() => (localStorage.getItem('theme') as Theme) || 'system');
  const [density, setDensity] = useState<Density>(() => (localStorage.getItem('density') as Density) || 'comfortable');
  const [accent,  setAccent]  = useState<AccentColor>(() => (localStorage.getItem('accent') as AccentColor) || '#2563eb');
  const [saved, setSaved]     = useState(false);

  const save = () => {
    localStorage.setItem('theme',   theme);
    localStorage.setItem('density', density);
    localStorage.setItem('accent',  accent);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const themes: { value: Theme; label: string; icon: React.ReactNode }[] = [
    { value: 'light',  label: 'Light',  icon: <Sun size={18} /> },
    { value: 'dark',   label: 'Dark',   icon: <Moon size={18} /> },
    { value: 'system', label: 'System', icon: <Monitor size={18} /> },
  ];

  const densities: { value: Density; label: string; desc: string }[] = [
    { value: 'compact',     label: 'Compact',     desc: 'Tight spacing, more content visible' },
    { value: 'comfortable', label: 'Comfortable', desc: 'Balanced spacing (default)' },
    { value: 'spacious',    label: 'Spacious',     desc: 'Extra padding for readability' },
  ];

  const accents: { color: AccentColor; name: string }[] = [
    { color: '#2563eb', name: 'Blue'   },
    { color: '#7c3aed', name: 'Violet' },
    { color: '#059669', name: 'Green'  },
    { color: '#dc2626', name: 'Red'    },
    { color: '#d97706', name: 'Amber'  },
  ];

  return (
    <SectionCard title="Appearance & Theme" icon={<Palette size={16} />}>
      {/* Theme selector */}
      <Field label="Color Theme">
        <div style={{ display: 'flex', gap: 10 }}>
          {themes.map(t => (
            <button key={t.value} onClick={() => setTheme(t.value)}
              style={{
                flex: 1, padding: '14px 10px', borderRadius: 8, cursor: 'pointer', transition: 'all 0.15s',
                border: `2px solid ${theme === t.value ? 'var(--primary)' : 'var(--border-medium)'}`,
                background: theme === t.value ? 'var(--primary-subtle)' : 'var(--bg-secondary)',
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6,
              }}>
              <span style={{ color: theme === t.value ? 'var(--primary)' : 'var(--text-muted)' }}>{t.icon}</span>
              <span style={{ fontSize: '0.78rem', fontWeight: 700, color: theme === t.value ? 'var(--primary)' : 'var(--text-secondary)' }}>{t.label}</span>
            </button>
          ))}
        </div>
      </Field>

      {/* Density */}
      <Field label="Content Density">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {densities.map(d => (
            <label key={d.value}
              style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px', borderRadius: 8, cursor: 'pointer',
                border: `1px solid ${density === d.value ? 'var(--primary)' : 'var(--border-medium)'}`,
                background: density === d.value ? 'var(--primary-subtle)' : 'var(--bg-secondary)', transition: 'all 0.15s' }}>
              <input type="radio" name="density" value={d.value} checked={density === d.value}
                onChange={() => setDensity(d.value)} style={{ accentColor: 'var(--primary)', width: 16, height: 16 }} />
              <div>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>{d.label}</div>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>{d.desc}</div>
              </div>
            </label>
          ))}
        </div>
      </Field>

      {/* Accent color */}
      <Field label="Accent Color" hint="Saves your preference locally in the browser.">
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {accents.map(a => (
            <button key={a.color} title={a.name} onClick={() => setAccent(a.color)}
              style={{
                width: 36, height: 36, borderRadius: '50%', background: a.color, cursor: 'pointer',
                border: `3px solid ${accent === a.color ? 'var(--text-primary)' : 'transparent'}`,
                outline: accent === a.color ? `2px solid ${a.color}` : 'none',
                outlineOffset: 2, transition: 'all 0.15s',
              }} />
          ))}
        </div>
      </Field>

      <button onClick={save}
        style={{ display: 'inline-flex', alignItems: 'center', gap: 7, padding: '9px 22px', fontSize: '0.88rem', fontWeight: 700, background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 'var(--radius-xs)', cursor: 'pointer' }}>
        {saved ? <><CheckCircle2 size={14} /> Saved!</> : <><Save size={14} /> Save Preferences</>}
      </button>
    </SectionCard>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Notifications Tab
// ─────────────────────────────────────────────────────────────────────────────
const NOTIF_KEY = 'notification_prefs';
type NotifPrefs = Record<string, boolean>;
const DEFAULT_PREFS: NotifPrefs = {
  task_due:       true,
  task_overdue:   true,
  risk_detected:  true,
  budget_warning: true,
  project_status: false,
  team_changes:   false,
  weekly_digest:  true,
};

function NotificationsTab() {
  const [prefs, setPrefs] = useState<NotifPrefs>(() => {
    try { return { ...DEFAULT_PREFS, ...JSON.parse(localStorage.getItem(NOTIF_KEY) || '{}') }; }
    catch { return DEFAULT_PREFS; }
  });
  const [saved, setSaved] = useState(false);

  const toggle = (key: string) => setPrefs(p => ({ ...p, [key]: !p[key] }));

  const save = () => {
    localStorage.setItem(NOTIF_KEY, JSON.stringify(prefs));
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const groups: { title: string; items: { key: string; label: string; desc: string }[] }[] = [
    {
      title: 'Task Alerts',
      items: [
        { key: 'task_due',     label: 'Task Due Soon',    desc: 'Alert 24h before a task is due' },
        { key: 'task_overdue', label: 'Overdue Tasks',    desc: 'Alert when tasks pass their due date' },
      ],
    },
    {
      title: 'Risk & Budget',
      items: [
        { key: 'risk_detected',  label: 'Risk Detected',    desc: 'AI detects a new project risk' },
        { key: 'budget_warning', label: 'Budget Warning',   desc: 'Budget usage crosses 80%' },
      ],
    },
    {
      title: 'Project & Team',
      items: [
        { key: 'project_status', label: 'Project Status Changes', desc: 'When a project is marked complete or on hold' },
        { key: 'team_changes',   label: 'Team Member Changes',    desc: 'When members are added or removed' },
      ],
    },
    {
      title: 'Reports',
      items: [
        { key: 'weekly_digest', label: 'Weekly Portfolio Digest', desc: 'Summary report every Monday morning' },
      ],
    },
  ];

  return (
    <SectionCard title="Notification Preferences" icon={<Bell size={16} />}>
      <p style={{ fontSize: '0.83rem', color: 'var(--text-secondary)', marginBottom: 20 }}>
        Control which events trigger in-app alerts. These preferences are saved locally.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        {groups.map(g => (
          <div key={g.title}>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.5px', textTransform: 'uppercase', marginBottom: 10 }}>{g.title}</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {g.items.map(item => (
                <div key={item.key}
                  onClick={() => toggle(item.key)}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 8, cursor: 'pointer', transition: 'background 0.15s' }}>
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>{item.label}</div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>{item.desc}</div>
                  </div>
                  {/* Toggle switch */}
                  <div style={{ width: 44, height: 24, borderRadius: 99, background: prefs[item.key] ? 'var(--primary)' : 'var(--border-medium)', position: 'relative', transition: 'background 0.2s', flexShrink: 0 }}>
                    <div style={{ width: 18, height: 18, borderRadius: '50%', background: '#fff', position: 'absolute', top: 3, left: prefs[item.key] ? 23 : 3, transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
      <button onClick={save} style={{ display: 'inline-flex', alignItems: 'center', gap: 7, padding: '9px 22px', fontSize: '0.88rem', fontWeight: 700, background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 'var(--radius-xs)', cursor: 'pointer', marginTop: 20 }}>
        {saved ? <><CheckCircle2 size={14} /> Saved!</> : <><Save size={14} /> Save Preferences</>}
      </button>
    </SectionCard>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Settings Page
// ─────────────────────────────────────────────────────────────────────────────
export default function Settings() {
  const [activeTab, setActiveTab] = useState<Tab>('profile');

  const tabs: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: 'profile',       label: 'Profile',       icon: <User size={16} /> },
    { id: 'security',      label: 'Security',      icon: <Lock size={16} /> },
    { id: 'appearance',    label: 'Appearance',    icon: <Palette size={16} /> },
    { id: 'notifications', label: 'Notifications', icon: <Bell size={16} /> },
  ];

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>

      {/* Header */}
      <div style={{ marginBottom: '1.75rem' }}>
        <div style={{ display: 'inline-block', padding: '4px 12px', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', borderRadius: 'var(--radius-full)', color: 'var(--primary)', fontSize: '0.76rem', fontWeight: 700, letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
          CONFIGURATION
        </div>
        <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)', margin: 0 }}>
          Workspace Settings
        </h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
          Manage your profile, security, and workspace preferences
        </p>
      </div>

      {/* Layout: sidebar nav + content */}
      <div style={{ display: 'flex', gap: 20, alignItems: 'flex-start' }}>

        {/* Sidebar navigation */}
        <div style={{ flexShrink: 0, width: 200, background: 'var(--bg-card)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', padding: 8, boxShadow: 'var(--shadow-sm)', position: 'sticky', top: 20 }}>
          {tabs.map(t => (
            <button key={t.id} onClick={() => setActiveTab(t.id)}
              style={{
                width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px',
                borderRadius: 8, border: 'none', cursor: 'pointer', textAlign: 'left', transition: 'all 0.15s',
                background: activeTab === t.id ? 'var(--primary-subtle)' : 'transparent',
                color: activeTab === t.id ? 'var(--primary)' : 'var(--text-secondary)',
                fontWeight: activeTab === t.id ? 700 : 500, fontSize: '0.85rem',
                justifyContent: 'space-between',
              }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>{t.icon}{t.label}</span>
              {activeTab === t.id && <ChevronRight size={13} />}
            </button>
          ))}

          <div style={{ margin: '12px 8px 4px', height: 1, background: 'var(--border-light)' }} />

          <div style={{ padding: '8px 12px 4px', fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
            System
          </div>
          <div style={{ padding: '8px 12px', fontSize: '0.78rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <SettingsIcon size={12} /> v1.0.0-MVP
          </div>
        </div>

        {/* Content area */}
        <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 16 }}>
          {activeTab === 'profile'       && <ProfileTab />}
          {activeTab === 'security'      && <SecurityTab />}
          {activeTab === 'appearance'    && <AppearanceTab />}
          {activeTab === 'notifications' && <NotificationsTab />}
        </div>
      </div>
    </div>
  );
}
