interface LogoLoaderProps {
  message?: string;
}

export default function LogoLoader({ message = 'Loading...' }: LogoLoaderProps) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem', width: '100%', height: '100%' }}>
      <img src="/logo.png" alt="Loading" className="logo-loader" style={{ width: '80px', height: '80px', marginBottom: '1rem', objectFit: 'contain' }} />
      <div style={{ color: 'var(--text-secondary)', fontWeight: 500, fontSize: '1rem', letterSpacing: '0.5px' }}>
        {message}
      </div>
    </div>
  );
}
