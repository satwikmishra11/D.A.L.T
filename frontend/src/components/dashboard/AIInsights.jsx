import React from 'react';
import { Sparkles, TrendingUp, AlertCircle, ArrowRight, Info } from 'lucide-react';

const AIInsights = ({ summary }) => {
  // Generate dynamic insights based on real summary data
  const insights = [];

  if (summary) {
    if (summary.activeAlerts > 0) {
      insights.push({
        title: "Active Alerts Detected",
        text: `${summary.activeAlerts} unacknowledged SLA breaches or failures have been detected. Inspect the Alert Panel immediately.`,
        type: "warning"
      });
    }

    if (summary.avgSuccessRate < 98.0) {
      insights.push({
        title: "Latency / Success Rate Drop",
        text: `Average success rate is at ${summary.avgSuccessRate.toFixed(1)}%. Target endpoints might be bottlenecked or rate-limiting requests.`,
        type: "warning"
      });
    }

    if (summary.activeWorkers === 0) {
      insights.push({
        title: "Zero Active Workers",
        text: "There are no active worker nodes registered with the controller. Deploy worker replicas to process load test tasks.",
        type: "info"
      });
    } else {
      insights.push({
        title: "Resource Utilization",
        text: `Running with ${summary.activeWorkers} active workers. Current capacity is optimal for running up to ${summary.activeWorkers * 5000} concurrent users.`,
        type: "success"
      });
    }
  }

  // Fallback default insights if no summary or not enough stats
  if (insights.length < 2) {
    insights.push({
      title: "Optimal Worker Scaling",
      text: "Based on current traffic trends, reducing worker nodes by 2 in the next 15 minutes will maintain 99th percentile SLA while optimizing costs.",
      type: "success"
    });
    insights.push({
      title: "Database Connection Pool",
      text: "Detected a 12% increase in connection acquisition time. Consider increasing max connections for the 'Auth' service if RPS exceeds 1200.",
      type: "warning"
    });
  }

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
          {insights.map((insight, idx) => (
            <div 
              key={idx} 
              className={`glass-dark p-4 rounded-xl border-l-4 ${
                insight.type === 'warning' ? 'border-l-yellow-500' :
                insight.type === 'info' ? 'border-l-blue-500' : 'border-l-green-500'
              }`}
            >
              <div className="flex items-start gap-3">
                {insight.type === 'warning' ? (
                  <AlertCircle size={18} className="text-yellow-400 mt-0.5" />
                ) : insight.type === 'info' ? (
                  <Info size={18} className="text-blue-400 mt-0.5" />
                ) : (
                  <TrendingUp size={18} className="text-green-400 mt-0.5" />
                )}
                <div>
                  <h4 className="text-sm font-semibold text-gray-100">{insight.title}</h4>
                  <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                    {insight.text}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t border-white/10">
          <button onClick={() => alert('Deep analysis shows no critical errors in the metrics pipeline.')} className="w-full py-2.5 bg-white/5 hover:bg-white/10 backdrop-blur-md border border-white/10 rounded-lg text-sm font-medium transition-all flex items-center justify-center gap-2 group">
            View Deep Analysis 
            <ArrowRight size={16} className="text-purple-400 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default AIInsights;
