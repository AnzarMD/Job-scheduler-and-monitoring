import { NavLink } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const navItems = [
  { to: '/dashboard', label: '📊 Dashboard' },
  { to: '/jobs',      label: '⚙️ Jobs' },
  { to: '/alerts',    label: '🔔 Alerts' },
];

// isLive is passed down from DashboardPage via App
// We use a simple prop here — Zustand would be overkill for one boolean
export default function Sidebar({ isLive = false }) {
  const { user, logout } = useAuthStore();

  return (
    <div className="w-64 min-h-screen bg-gray-900 text-white flex flex-col">
      <div className="p-6 border-b border-gray-700">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-blue-400">☁️ CloudFlow</h1>
          {/* Live indicator dot */}
          <div className="flex items-center gap-1.5">
            <div className={`w-2 h-2 rounded-full ${
              isLive
                ? 'bg-green-400 animate-pulse'  // pulsing green = connected
                : 'bg-gray-500'                  // gray = disconnected
            }`} />
            <span className={`text-xs ${isLive ? 'text-green-400' : 'text-gray-500'}`}>
              {isLive ? 'Live' : 'Offline'}
            </span>
          </div>
        </div>
        <p className="text-xs text-gray-400 mt-1">{user?.companyName}</p>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `block px-4 py-2 rounded-lg text-sm transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-300 hover:bg-gray-800'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-gray-700">
        <p className="text-xs text-gray-400 mb-2">{user?.email}</p>
        <button
          onClick={logout}
          className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-gray-800 rounded-lg"
        >
          🚪 Logout
        </button>
      </div>
    </div>
  );
}