import React from 'react';
import { motion } from 'framer-motion';
import { cn } from './Button';

export const Tabs = ({ tabs, activeTab, onChange }) => {
  return (
    <div className="flex gap-2 border-b border-gray-200 w-full mb-6">
      {tabs.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={cn(
              "relative px-4 py-3 text-sm font-medium transition-colors outline-none",
              isActive ? "text-aws-nav" : "text-gray-500 hover:text-gray-700 hover:bg-gray-50 rounded-t-lg"
            )}
          >
            <div className="flex items-center gap-2">
              {tab.icon && <tab.icon size={16} />}
              {tab.label}
            </div>
            {isActive && (
              <motion.div
                layoutId="activeTabIndicator"
                className="absolute bottom-0 left-0 right-0 h-0.5 bg-aws-orange"
              />
            )}
          </button>
        );
      })}
    </div>
  );
};
