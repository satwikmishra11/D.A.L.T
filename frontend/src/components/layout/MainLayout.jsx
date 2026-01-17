import React from 'react';
import { LayoutDashboard, Play, FileText, Settings, Activity, Box, Bell, Search, User } from 'lucide-react';

const Sidebar = ({ activeView, setActiveView }) => {
  const menuItems = [
    { id: 'overview', label: 'Dashboard', icon: <LayoutDashboard size={20} /> },
    { id: 'scenarios', label: 'Scenarios', icon: <FileText size={20} /> },
    { id: 'compare', label: 'Compare', icon: <Activity size={20} /> },
    { id: 'templates', label: 'Templates', icon: <Box size={20} /> },
    { id: 'runs', label: 'Test Runs', icon: <Play size={20} /> },
    { id: 'settings', label: 'Settings', icon: <Settings size={20} /> },
  ];

  return (
    <div className="w-64 bg-aws-nav min-h-screen text-gray-300 flex flex-col shrink-0 transition-all duration-300">
      <div className="p-6 flex items-center gap-3 border-b border-gray-700 bg-aws-dark">
        <div className="w-8 h-8 bg-gradient-to-br from-aws-orange to-yellow-500 rounded flex items-center justify-center text-white font-bold text-xl shadow-lg">D</div>
        <span className="text-white font-bold text-lg tracking-wide">D.A.L.T</span>
      </div>

      <div className="flex-1 px-3 py-6 space-y-1">
        <div className="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-gray-500">Platform</div>
        {menuItems.map(item => (
          <div
            key={item.id}
            onClick={() => setActiveView(item.id)}
            className={`nav-item ${activeView === item.id ? 'active' : ''}`}
          >
            {item.icon}
            <span className="font-medium">{item.label}</span>
          </div>
        ))}
      </div>

      <div className="p-4 border-t border-gray-700 bg-aws-dark">
        <div className="flex items-center gap-3 px-3 py-2 hover:bg-aws-hover rounded-lg cursor-pointer transition-colors">
          <div className="w-9 h-9 bg-gray-600 rounded-full flex items-center justify-center text-white border-2 border-gray-500">
            <User size={18} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-sm font-medium text-white truncate">Admin User</div>
            <div className="text-xs text-gray-400 truncate">Platform Engineer</div>
          </div>
        </div>
      </div>
    </div>
  );
};

const Header = () => {
  return (
    <header className="bg-white border-b h-16 flex items-center justify-between px-8 shadow-sm z-10">
      <div className="flex items-center gap-4 w-96">
        <div className="relative w-full group">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-aws-orange transition-colors" size={18} />
          <input 
            type="text" 
            placeholder="Search scenarios, run IDs..." 
            className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-aws-orange/20 focus:border-aws-orange transition-all text-sm bg-gray-50 focus:bg-white"
          />
        </div>
      </div>
      <div className="flex items-center gap-6 text-gray-600">
        <div className="flex items-center gap-2 px-3 py-1.5 bg-green-50 text-green-700 rounded-full text-xs font-medium border border-green-100">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
          System Operational
        </div>
        <button className="relative p-2 hover:bg-gray-100 rounded-full transition-colors">
          <Bell size={20} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
        </button>
        <div className="h-6 w-px bg-gray-200"></div>
        <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-blue-500"></span>
            <span className="text-sm font-medium text-gray-700">us-east-1</span>
        </div>
      </div>
    </header>
  );
};

const MainLayout = ({ children, activeView, setActiveView }) => {
  return (
    <div className="flex min-h-screen bg-gray-50 font-sans">
      <Sidebar activeView={activeView} setActiveView={setActiveView} />
      <div className="flex-1 flex flex-col min-w-0">
        <Header />
        <main className="flex-1 p-8 overflow-y-auto overflow-x-hidden">
          <div className="max-w-7xl mx-auto animate-in fade-in duration-500">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};

export default MainLayout;
