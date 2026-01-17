import React from 'react';
import { motion } from 'framer-motion';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { Loader2 } from 'lucide-react';

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

const variants = {
  primary: "bg-aws-orange text-white hover:bg-yellow-500 shadow-sm border border-transparent",
  secondary: "bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 shadow-sm",
  outline: "bg-transparent text-gray-700 border border-gray-300 hover:bg-gray-50",
  ghost: "bg-transparent text-gray-600 hover:bg-gray-100 hover:text-gray-900",
  danger: "bg-red-600 text-white hover:bg-red-700 shadow-sm border border-transparent",
};

const sizes = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-4 py-2 text-sm",
  lg: "px-6 py-3 text-base",
};

export const Button = React.forwardRef(({ 
  className, 
  variant = "primary", 
  size = "md", 
  isLoading = false, 
  icon: Icon,
  children, 
  ...props 
}, ref) => {
  return (
    <motion.button
      ref={ref}
      whileTap={{ scale: 0.98 }}
      className={cn(
        "inline-flex items-center justify-center rounded-md font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-aws-orange/50 disabled:opacity-50 disabled:pointer-events-none",
        variants[variant],
        sizes[size],
        className
      )}
      disabled={isLoading || props.disabled}
      {...props}
    >
      {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
      {!isLoading && Icon && <Icon className="mr-2 h-4 w-4" />}
      {children}
    </motion.button>
  );
});

Button.displayName = "Button";
