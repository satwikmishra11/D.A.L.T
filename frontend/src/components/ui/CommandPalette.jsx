import React, { useState, useEffect, useRef } from 'react';
import { Search, Command, Activity, Zap, PlayCircle, Settings, Users, ArrowRight, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const actions = [
  { id: 1, title: 'Trigger New Load Test', icon: PlayCircle, shortcut: 'T', category: 'Actions', color: 'text-blue-500' },
  { id: 2, title: 'View Active Scenarios', icon: Activity, shortcut: 'S', category: 'Views', color: 'text-green-500' },
  { id: 3, title: 'Manage Worker Nodes', icon: Zap, shortcut: 'W', category: 'Views', color: 'text-yellow-500' },
  { id: 4, title: 'Configure Thresholds', icon: Settings, shortcut: 'C', category: 'Settings', color: 'text-purple-500' },
  { id: 5, title: 'Team Access Roles', icon: Users, shortcut: 'R', category: 'Settings', color: 'text-gray-500' },
];

const CommandPalette = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef(null);

  const filteredActions = actions.filter(action => 
    action.title.toLowerCase().includes(search.toLowerCase()) || 
    action.category.toLowerCase().includes(search.toLowerCase())
  );

  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setIsOpen((open) => !open);
      }
      if (e.key === 'Escape') setIsOpen(false);
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  useEffect(() => {
    if (isOpen && inputRef.current) {
      setTimeout(() => inputRef.current.focus(), 100);
      setSearch('');
      setSelectedIndex(0);
    }
  }, [isOpen]);

  useEffect(() => {
    const handleNavigation = (e) => {
      if (!isOpen) return;
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev + 1) % filteredActions.length);
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev - 1 + filteredActions.length) % filteredActions.length);
      }
      if (e.key === 'Enter' && filteredActions[selectedIndex]) {
        e.preventDefault();
        console.log('Action triggered:', filteredActions[selectedIndex].title);
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleNavigation);
    return () => window.removeEventListener('keydown', handleNavigation);
  }, [isOpen, filteredActions, selectedIndex]);

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-[15vh] px-4">
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-gray-900/40 backdrop-blur-sm"
            onClick={() => setIsOpen(false)}
          />
          
          <motion.div 
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -20 }}
            transition={{ type: 'spring', damping: 25, stiffness: 300 }}
            className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl overflow-hidden relative z-10 border border-gray-100"
          >
            <div className="flex items-center px-4 py-4 border-b border-gray-100">
              <Search className="w-5 h-5 text-gray-400 mr-3" />
              <input
                ref={inputRef}
                type="text"
                placeholder="Type a command or search..."
                className="w-full bg-transparent border-none outline-none text-lg text-gray-900 placeholder-gray-400"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setSelectedIndex(0);
                }}
              />
              <div className="flex gap-1">
                <kbd className="px-2 py-1 bg-gray-100 border border-gray-200 rounded text-xs text-gray-500 font-mono">esc</kbd>
              </div>
            </div>

            <div className="max-h-[60vh] overflow-y-auto p-2">
              {filteredActions.length === 0 ? (
                <div className="p-8 text-center text-gray-500">
                  No results found for "{search}"
                </div>
              ) : (
                <div className="space-y-1">
                  {filteredActions.map((action, index) => {
                    const isSelected = index === selectedIndex;
                    const Icon = action.icon;
                    return (
                      <div 
                        key={action.id}
                        className={`flex items-center justify-between px-4 py-3 rounded-xl cursor-pointer transition-colors ${isSelected ? 'bg-primary-50 text-primary-900' : 'hover:bg-gray-50 text-gray-700'}`}
                        onClick={() => {
                          console.log('Action triggered:', action.title);
                          setIsOpen(false);
                        }}
                        onMouseEnter={() => setSelectedIndex(index)}
                      >
                        <div className="flex items-center gap-3">
                          <div className={`p-2 rounded-lg ${isSelected ? 'bg-white shadow-sm' : 'bg-gray-100'} ${action.color}`}>
                            <Icon size={18} />
                          </div>
                          <span className="font-medium">{action.title}</span>
                          <span className="text-xs text-gray-400 ml-2 bg-gray-100 px-2 py-0.5 rounded-full">{action.category}</span>
                        </div>
                        {isSelected ? (
                          <span className="text-primary-600 flex items-center gap-1 text-sm font-medium">
                            Enter <ArrowRight size={14} />
                          </span>
                        ) : (
                          <div className="flex gap-1 opacity-50">
                            {action.shortcut && <kbd className="w-6 h-6 flex items-center justify-center bg-gray-100 border border-gray-200 rounded text-xs text-gray-500 font-mono">{action.shortcut}</kbd>}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
            
            <div className="px-4 py-3 bg-gray-50 border-t border-gray-100 text-xs text-gray-500 flex justify-between">
              <span className="flex items-center gap-2">
                <Command size={14} /> Global Command Palette
              </span>
              <span className="flex items-center gap-4">
                <span className="flex items-center gap-1">Use <kbd className="px-1.5 py-0.5 bg-white border border-gray-200 rounded">↑</kbd> <kbd className="px-1.5 py-0.5 bg-white border border-gray-200 rounded">↓</kbd> to navigate</span>
                <span className="flex items-center gap-1"><kbd className="px-1.5 py-0.5 bg-white border border-gray-200 rounded text-xs">↵</kbd> to select</span>
              </span>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};

export default CommandPalette;
