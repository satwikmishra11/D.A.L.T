import React from 'react';
import { Sparkles, TrendingUp, AlertCircle, ArrowRight } from 'lucide-react';

const AIInsights = () => {
  return (
    <div className="card lg:col-span-1 relative overflow-hidden bg-gradient-to-br from-gray-900 to-aws-dark text-white shadow-2xl border-none">
      {/* Background Animated Blobs */}
      <div className="absolute top-0 right-0 -mr-20 -mt-20 w-64 h-64 bg-primary-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob"></div>
      <div className="absolute bottom-0 left-0 -ml-20 -mb-20 w-64 h-64 bg-purple-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob" style={{ animationDelay: '2s' }}></div>
      
      <div className="relative z-10 flex flex-col h-full">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-white/10 backdrop-blur-md rounded-lg">
              <Sparkles size={20} className="text-purple-400" />
            </div>
            <h3 className="font-bold text-xl tracking-tight">AI Insights</h3>
          </div>
          <span className="px-3 py-1 bg-white/10 backdrop-blur-sm rounded-full text-xs font-semibold text-purple-200 border border-white/10">
            Real-time Analysis
          </span>
        </div>

        <div className="flex-grow space-y-4">
          <div className="glass-dark p-4 rounded-xl">
            <div className="flex items-start gap-3">
              <TrendingUp size={18} className="text-green-400 mt-0.5" />
              <div>
                <h4 className="text-sm font-semibold text-gray-100">Optimal Worker Scaling</h4>
                <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                  Based on current traffic trends, reducing worker nodes by 2 in the next 15 minutes will maintain 99th percentile SLA while optimizing costs.
                </p>
              </div>
            </div>
          </div>

          <div className="glass-dark p-4 rounded-xl border-l-4 border-l-yellow-500">
            <div className="flex items-start gap-3">
              <AlertCircle size={18} className="text-yellow-400 mt-0.5" />
              <div>
                <h4 className="text-sm font-semibold text-gray-100">Database Connection Pool</h4>
                <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                  Detected a 12% increase in connection acquisition time. Consider increasing max connections for the "Auth" service if RPS exceeds 1200.
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-white/10">
          <button className="w-full py-2.5 bg-white/5 hover:bg-white/10 backdrop-blur-md border border-white/10 rounded-lg text-sm font-medium transition-all flex items-center justify-center gap-2 group">
            View Deep Analysis 
            <ArrowRight size={16} className="text-purple-400 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default AIInsights;
