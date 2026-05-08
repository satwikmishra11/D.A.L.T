import React, { useState } from 'react';
import { LayoutDashboard, Play, FileText, Settings, Activity, Box, Bell, Search, User, Terminal, ChevronRight } from 'lucide-react';
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
    <div className="w-[240px] bg-[#f2f3f3] border-r border-[#eaeded] min-h-[calc(100vh-40px)] text-[#16191f] flex flex-col shrink-0 transition-all duration-300">
      <div className="px-4 py-3 border-b border-[#eaeded]">
        <h2 className="text-[16px] font-bold text-[#16191f]">Platform Services</h2>
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

const Header = ({ onToggleShell }) => {
  return (
    <header className="bg-[#232f3e] h-[40px] flex items-center justify-between px-4 z-50 text-white shrink-0 sticky top-0">
      <div className="flex items-center gap-6">
        <div className="flex items-center gap-2 cursor-pointer hover:bg-[#16191f] px-2 py-1 rounded-[2px] transition-colors">
          <div className="w-5 h-5 bg-[#ec7211] rounded-[2px] flex items-center justify-center text-white font-bold text-[10px]">DC</div>
          <span className="text-white font-bold text-[14px] tracking-wide">DevOps Central</span>
        </div>
        
        <div className="relative w-[500px] group hidden md:block">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[#879596] group-focus-within:text-white transition-colors" size={14} />
          <input 
            type="text" 
            placeholder="Search services, features, scenarios (Option+S)" 
            onClick={() => alert('Opening Global Search Palette...')}
            readOnly
            className="w-full pl-8 pr-4 py-1 border border-[#374151] rounded-[2px] focus:outline-none focus:border-[#0073bb] focus:ring-1 focus:ring-[#0073bb] transition-all text-[13px] bg-[#16191f] text-white cursor-text placeholder-[#879596]"
          />
          <div className="absolute right-2 top-1/2 -translate-y-1/2 hidden md:flex gap-1">
            <kbd className="px-1.5 py-0.5 text-[10px] font-semibold text-[#879596] bg-[#232f3e] border border-[#374151] rounded-[2px]">⌥</kbd>
            <kbd className="px-1.5 py-0.5 text-[10px] font-semibold text-[#879596] bg-[#232f3e] border border-[#374151] rounded-[2px]">S</kbd>
          </div>
        </div>
      </div>
      <div className="flex items-center gap-3 text-[#d5dbdb]">
        <button onClick={onToggleShell} className="p-1.5 hover:text-white hover:bg-[#16191f] rounded-[2px] transition-colors" title="AWS CloudShell">
          <Terminal size={16} />
        </button>
        <button onClick={() => alert('No new system alerts.')} className="relative p-1.5 hover:text-white hover:bg-[#16191f] rounded-[2px] transition-colors">
          <Bell size={16} />
          <span className="absolute top-1 right-1 w-1.5 h-1.5 bg-[#d13212] rounded-full"></span>
        </button>
        <div className="h-4 w-px bg-[#374151] mx-1"></div>
        <div className="flex items-center gap-1 cursor-pointer hover:text-white hover:bg-[#16191f] px-2 py-1 rounded-[2px] transition-colors">
            <span className="text-[13px] font-bold text-[#d5dbdb]">Oregon</span>
            <span className="text-[11px] text-[#879596]">us-west-2</span>
        </div>
        <div className="flex items-center gap-1 cursor-pointer hover:text-white hover:bg-[#16191f] px-2 py-1 rounded-[2px] transition-colors">
            <span className="text-[13px] font-bold text-[#d5dbdb]">admin-user</span>
        </div>
      </div>
    </header>
  );
};

const CloudShell = ({ isOpen, onClose }) => {
  if (!isOpen) return null;
  return (
    <div className="h-[300px] bg-[#16191f] border-t border-[#374151] flex flex-col shrink-0 font-mono">
      <div className="flex items-center justify-between px-4 py-1.5 bg-[#232f3e] border-b border-[#374151]">
        <span className="text-[12px] font-bold text-white flex items-center gap-2">
          <Terminal size={14} /> DevOps Shell (us-west-2)
        </span>
        <button onClick={onClose} className="text-[#879596] hover:text-white">✕</button>
      </div>
      <div className="flex-1 p-3 text-[13px] text-[#d5dbdb] overflow-y-auto">
        <div className="text-[#879596] mb-2">Welcome to DevOps Central Shell. Authenticated as admin-user.</div>
        <div className="flex items-center gap-2">
          <span className="text-[#ec7211]">~ $</span>
          <span className="animate-pulse w-2 h-4 bg-[#d5dbdb]"></span>
        </div>
      </div>
    </div>
  );
};

const Breadcrumbs = ({ activeView }) => {
  const views = {
    'overview': 'Dashboard',
    'scenarios': 'Test Scenarios',
    'compare': 'Scenario Comparison',
    'templates': 'Templates',
    'runs': 'Test Runs History',
    'settings': 'Platform Settings'
  };
  return (
    <div className="flex items-center gap-2 text-[13px] text-[#545b64] mb-5">
      <span className="hover:underline cursor-pointer text-[#0073bb]">DevOps Central</span>
      <ChevronRight size={14} />
      <span className="text-[#16191f] font-bold">{views[activeView] || 'Overview'}</span>
    </div>
  );
};

const MainLayout = ({ children, activeView, setActiveView }) => {
  const [isShellOpen, setIsShellOpen] = useState(false);

  return (
    <div className="flex flex-col min-h-screen bg-white font-sans">
      <Header onToggleShell={() => setIsShellOpen(!isShellOpen)} />
      <div className="flex flex-1 overflow-hidden">
        <CommandPalette />
        <Sidebar activeView={activeView} setActiveView={setActiveView} />
        <div className="flex-1 flex flex-col min-w-0 bg-white relative">
          <main className="flex-1 p-6 overflow-y-auto overflow-x-hidden relative z-0">
            <div className="max-w-[1200px] mx-auto animate-in fade-in duration-500">
              <Breadcrumbs activeView={activeView} />
              {children}
            </div>
          </main>
          <CloudShell isOpen={isShellOpen} onClose={() => setIsShellOpen(false)} />
        </div>
      </div>
    </div>
  );
};

export default MainLayout;
