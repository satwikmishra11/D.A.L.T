/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Professional Enterprise Blue Palette (AWS-like but distinct)
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7', // Main Enterprise Blue
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
        },
        // Dashboard Backgrounds
        aws: {
          nav: '#232f3e',
          dark: '#161e2d',
          orange: '#ff9900',
          hover: '#374151'
        },
        // Status Colors
        success: '#10b981',
        warning: '#f59e0b',
        error: '#ef4444',
        info: '#3b82f6',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'card': '0 2px 5px rgba(0,0,0,0.05)',
        'card-hover': '0 8px 20px rgba(0,0,0,0.08)',
      }
    },
  },
  plugins: [],
}