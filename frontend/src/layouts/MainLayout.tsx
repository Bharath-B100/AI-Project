import { Outlet } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import TopNav from '../components/TopNav';

export default function MainLayout() {
  return (
    // Constrain outer container to exactly the viewport height.
    // overflow: hidden prevents the body from scrolling — only <main> scrolls.
    <div style={{ display: 'flex', height: '100vh', width: '100%', overflow: 'hidden' }}>
      <Sidebar />
      {/* Right column: TopNav (fixed height) + scrollable main */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
        <TopNav />
        {/* Only this element scrolls — sidebar stays perfectly fixed */}
        <main style={{ flex: 1, padding: '2rem', overflowY: 'auto' }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
