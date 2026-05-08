import React from 'react';
import { LayoutDashboard, Play, FileText, Settings, Activity, Box, Bell, Search, User } from 'lucide-react';
import CommandPalette from '../ui/CommandPalette';

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
    <div className="w-[240px] bg-[#f2f3f3] border-r border-[#eaeded] min-h-screen text-[#16191f] flex flex-col shrink-0 transition-all duration-300">
      <div className="h-[50px] flex items-center gap-3 px-4 bg-[#232f3e] text-white shrink-0">
        <div className="w-6 h-6 bg-[#ec7211] rounded-[2px] flex items-center justify-center text-white font-bold text-xs">D</div>
        <span className="text-white font-bold text-[15px] tracking-wide">D.A.L.T Core</span>
      </div>

      <div className="flex-1 px-0 py-3 space-y-0.5">
        <div className="px-4 mb-2 text-[11px] font-bold uppercase tracking-wider text-[#545b64]">AWS Services</div>
        {menuItems.map(item => (
          <div
            key={item.id}
            onClick={() => setActiveView(item.id)}
            className={`flex items-center gap-3 px-4 py-1.5 text-[13px] font-medium transition-colors cursor-pointer border-l-4 ${activeView === item.id ? 'border-[#ec7211] text-[#ec7211] bg-[#fafafa]' : 'border-transparent text-[#16191f] hover:text-[#0073bb]'}`}
          >
            {item.icon}
            <span>{item.label}</span>
          </div>
        ))}
      </div>

      <div className="p-4 border-t border-[#eaeded] bg-[#f2f3f3]">
        <div className="flex items-center gap-3 px-2 py-1 cursor-pointer transition-colors group">
          <div className="w-7 h-7 bg-[#545b64] rounded-full flex items-center justify-center text-white">
            <User size={14} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[13px] font-bold text-[#16191f] truncate group-hover:text-[#0073bb]">Admin User</div>
            <div className="text-[11px] text-[#545b64] truncate">Platform Engineer</div>
          </div>
        </div>
      </div>
    </div>
  );
};

const Header = () => {
  return (
    <header className="bg-[#232f3e] h-[50px] flex items-center justify-between px-6 shadow-sm z-10 text-white shrink-0">
      <div className="flex items-center gap-4 w-[400px]">
        <div className="relative w-full group">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-[#879596] group-focus-within:text-white transition-colors" size={16} />
          <input 
            type="text" 
            placeholder="Search services, features, scenarios (Option+S)" 
            onClick={() => alert('Opening Global Search Palette...')}
            readOnly
            className="w-full pl-9 pr-4 py-1.5 border border-[#374151] rounded-[2px] focus:outline-none focus:ring-1 focus:ring-white transition-all text-[13px] bg-[#16191f] text-white cursor-text placeholder-[#879596]"
          />
          <div className="absolute right-3 top-1/2 -translate-y-1/2 hidden md:flex gap-1">
            <kbd className="px-1.5 py-0.5 text-[10px] font-semibold text-[#879596] bg-[#232f3e] border border-[#374151] rounded-[2px]">⌥</kbd>
            <kbd className="px-1.5 py-0.5 text-[10px] font-semibold text-[#879596] bg-[#232f3e] border border-[#374151] rounded-[2px]">S</kbd>
          </div>
        </div>
      </div>
      <div className="flex items-center gap-5 text-[#d5dbdb]">
        <div className="flex items-center gap-2 px-2 py-1 bg-[#16191f] text-[#d5dbdb] rounded-[2px] text-[12px] font-bold border border-[#374151]">
          <div className="w-1.5 h-1.5 bg-[#10b981] rounded-full animate-pulse"></div>
          Operational
        </div>
        <button onClick={() => alert('No new system alerts or SLA violations.')} className="relative p-1.5 hover:text-white transition-colors">
          <Bell size={16} />
          <span className="absolute top-1 right-1 w-1.5 h-1.5 bg-[#d13212] rounded-full"></span>
        </button>
        <div className="h-5 w-px bg-[#374151]"></div>
        <div className="flex items-center gap-2 cursor-pointer hover:text-white transition-colors">
            <span className="text-[13px] font-bold text-[#d5dbdb]">Oregon</span>
            <span className="text-[11px] text-[#879596]">us-west-2</span>
        </div>
      </div>
    </header>
  );
};

const MainLayout = ({ children, activeView, setActiveView }) => {
  return (
    <div className="flex min-h-screen bg-gray-50 font-sans">
      <CommandPalette />
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
