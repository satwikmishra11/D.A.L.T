import React from 'react';
import { motion } from 'framer-motion';
import { cn } from './Button';

export const Card = ({ className, children, ...props }) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={cn(
        "bg-white rounded-lg shadow-card border border-gray-100 p-6 hover:shadow-card-hover transition-shadow duration-300",
        className
      )}
      {...props}
    >
      {children}
    </motion.div>
  );
};

export const CardHeader = ({ className, children }) => (
  <div className={cn("flex flex-col space-y-1.5 mb-4", className)}>{children}</div>
);

export const CardTitle = ({ className, children }) => (
  <h3 className={cn("text-lg font-bold text-gray-900 leading-none tracking-tight", className)}>
    {children}
  </h3>
);

export const CardDescription = ({ className, children }) => (
  <p className={cn("text-sm text-gray-500", className)}>{children}</p>
);

export const CardContent = ({ className, children }) => (
  <div className={cn("", className)}>{children}</div>
);

export const CardFooter = ({ className, children }) => (
  <div className={cn("flex items-center pt-4 border-t border-gray-100 mt-4", className)}>
    {children}
  </div>
);
