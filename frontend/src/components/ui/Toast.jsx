import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react';
import { cn } from './Button';

const icons = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const colors = {
  success: "bg-green-50 border-green-200 text-green-800",
  error: "bg-red-50 border-red-200 text-red-800",
  warning: "bg-yellow-50 border-yellow-200 text-yellow-800",
  info: "bg-blue-50 border-blue-200 text-blue-800",
};

export const Toast = ({ type = "info", message, onClose }) => {
  const Icon = icons[type];
  
  return (
    <motion.div
      initial={{ opacity: 0, x: 50 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, scale: 0.9 }}
      className={cn(
        "flex items-start gap-3 p-4 rounded-lg shadow-lg border max-w-sm w-full pointer-events-auto",
        colors[type]
      )}
    >
      <Icon size={20} className="shrink-0 mt-0.5" />
      <div className="flex-1 text-sm font-medium">{message}</div>
      <button onClick={onClose} className="opacity-70 hover:opacity-100 transition-opacity">
        <X size={16} />
      </button>
    </motion.div>
  );
};

export const ToastProvider = ({ toasts, removeToast }) => {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
      <AnimatePresence>
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            {...toast}
            onClose={() => removeToast(toast.id)}
          />
        ))}
      </AnimatePresence>
    </div>
  );
};
