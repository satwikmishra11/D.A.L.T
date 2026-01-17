import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, TrendingDown, Activity, AlertTriangle, CheckCircle, Clock, Zap, ArrowRight } from 'lucide-react';

const StatCard = ({ title, value, subtext, trend, icon: Icon, color }) => (
  <div className="card hover:shadow-card-hover transition-all duration-300 transform hover:-translate-y-1 group border-l-4" style={{ borderLeftColor: `var(--color-${color}-500)` }}>
    <div className="flex justify-between items-start mb-4">
      <div>
        <p className="text-sm font-medium text-gray-500 tracking-wide uppercase">{title}</p>
        <h3 className="text-3xl font-bold mt-1 text-gray-900 tracking-tight">{value}</h3>
      </div>
      <div className={`p-3 rounded-xl bg-${color}-50 text-${color}-600 group-hover:bg-${color}-100 transition-colors shadow-sm`}>
        <Icon size={22} strokeWidth={2.5} />
      </div>
    </div>
    <div className="flex items-center gap-2 text-sm bg-gray-50 p-2 rounded-lg -mx-2 -mb-2">
      {trend > 0 ? (
        <TrendingUp size={16} className="text-green-500" />
      ) : (
        <TrendingDown size={16} className="text-red-500" />
      )}
      <span className={trend > 0 ? "text-green-600 font-bold" : "text-red-600 font-bold"}>
        {Math.abs(trend)}%
      </span>
      <span className="text-gray-400 font-medium">vs last week</span>
    </div>
  </div>
);

const HealthScoreGauge = ({ score }) => {
  const circumference = 2 * Math.PI * 52; // increased radius
  const offset = circumference - (score / 100) * circumference;
  
  let color = 'text-green-500';
  let gradientId = 'grad-green';
  if (score < 70) { color = 'text-yellow-500'; gradientId = 'grad-yellow'; }
  if (score < 50) { color = 'text-red-500'; gradientId = 'grad-red'; }

  return (
    <div className="relative w-40 h-40 flex items-center justify-center">
        <svg className="w-full h-full transform -rotate-90">
        <defs>
            <linearGradient id="grad-green" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#22c55e" />
                <stop offset="100%" stopColor="#86efac" />
            </linearGradient>
            <linearGradient id="grad-yellow" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#eab308" />
                <stop offset="100%" stopColor="#fde047" />
            </linearGradient>
             <linearGradient id="grad-red" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#ef4444" />
                <stop offset="100%" stopColor="#fca5a5" />
            </linearGradient>
        </defs>
        <circle cx="80" cy="80" r="52" stroke="currentColor" strokeWidth="10" fill="transparent" className="text-gray-100" />
        <circle cx="80" cy="80" r="52" stroke={`url(#${gradientId})`} strokeWidth="10" fill="transparent" 
          strokeDasharray={circumference} 
          strokeDashoffset={offset} 
          strokeLinecap="round"
          className="transition-all duration-1000 ease-out" 
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="text-4xl font-extrabold text-gray-800 tracking-tighter">{score}</span>
        <span className="text-xs font-bold text-gray-400 uppercase tracking-widest mt-1">Health</span>
      </div>
    </div>
  );
};

const DashboardOverview = ({ analyticsData }) => {
  // Mock data for the chart
  const data = [
    { time: '10:00', rps: 400, latency: 240 },
    { time: '10:05', rps: 300, latency: 139 },
    { time: '10:10', rps: 550, latency: 380 },
    { time: '10:15', rps: 500, latency: 300 },
    { time: '10:20', rps: 700, latency: 450 },
    { time: '10:25', rps: 600, latency: 370 },
    { time: '10:30', rps: 800, latency: 500 },
    { time: '10:35', rps: 750, latency: 480 },
    { time: '10:40', rps: 900, latency: 200 },
  ];

  return (
    <div className="space-y-8">
      <div className="flex justify-between items-end pb-4 border-b border-gray-100">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">Platform Overview</h1>
          <p className="text-gray-500 mt-2 flex items-center gap-2">
            <Clock size={16} /> Last updated: Just now
          </p>
        </div>
        <div className="flex gap-4">
          <button className="btn-secondary flex items-center gap-2 shadow-sm hover:shadow">
            Export Report
          </button>
          <button className="btn-primary flex items-center gap-2 shadow-md hover:shadow-lg transform hover:-translate-y-0.5 transition-all">
            <Activity size={18} /> Run Health Check
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Active Scenarios" value="12" trend={8.5} icon={Zap} color="blue" />
        <StatCard title="Total Requests" value="2.4M" trend={12.3} icon={Activity} color="green" />
        <StatCard title="Avg Latency" value="45ms" trend={-2.4} icon={CheckCircle} color="purple" />
        <StatCard title="Error Rate" value="0.12%" trend={-5.1} icon={AlertTriangle} color="red" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Chart */}
        <div className="card lg:col-span-2 p-6">
          <div className="flex justify-between items-center mb-8">
            <div>
                <h3 className="font-bold text-xl text-gray-900">Throughput vs Latency</h3>
                <p className="text-sm text-gray-500">Real-time correlation analysis</p>
            </div>
            <div className="flex gap-2">
                {['1H', '24H', '7D'].map((period) => (
                    <button key={period} className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${period === '1H' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
                        {period}
                    </button>
                ))}
            </div>
          </div>
          <div className="h-96">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorRps" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorLatency" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} />
                <Tooltip 
                  contentStyle={{
                      backgroundColor: 'rgba(255, 255, 255, 0.95)', 
                      borderRadius: '12px', 
                      border: 'none', 
                      boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
                      padding: '12px'
                  }}
                  itemStyle={{ fontSize: '13px', fontWeight: 600 }}
                  labelStyle={{ color: '#6b7280', marginBottom: '8px', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="rps" stroke="#0ea5e9" strokeWidth={3} fillOpacity={1} fill="url(#colorRps)" name="Requests/sec" />
                <Area type="monotone" dataKey="latency" stroke="#8b5cf6" strokeWidth={3} fillOpacity={1} fill="url(#colorLatency)" name="Latency (ms)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Health & Insights Side Panel */}
        <div className="space-y-8">
          <div className="card flex flex-col items-center py-10 relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-green-400 to-green-600"></div>
            <HealthScoreGauge score={85} />
            <div className="mt-8 text-center px-6">
              <h4 className="text-xl font-bold text-gray-900">System Healthy</h4>
              <p className="text-sm text-gray-500 mt-2 leading-relaxed">
                All critical systems are operational. Global latency is within 99th percentile SLA limits.
              </p>
            </div>
            <button className="mt-8 text-aws-orange text-sm font-bold hover:text-yellow-600 flex items-center gap-1 transition-colors">
              View Detailed Analysis <ArrowRight size={14} />
            </button>
          </div>

          <div className="card">
            <div className="flex justify-between items-center mb-6">
                <h3 className="font-bold text-gray-900">Recent Alerts</h3>
                <span className="bg-red-100 text-red-700 text-xs font-bold px-2 py-1 rounded-full">3 New</span>
            </div>
            <div className="space-y-4">
              {[1, 2, 3].map((_, i) => (
                <div key={i} className="flex gap-4 items-start p-4 bg-red-50 rounded-xl border border-red-100 hover:bg-red-100/50 transition-colors cursor-pointer">
                  <div className="bg-red-200 p-2 rounded-lg">
                    <AlertTriangle size={18} className="text-red-600" />
                  </div>
                  <div>
                    <h5 className="text-sm font-bold text-gray-900">High Latency Detected</h5>
                    <p className="text-xs text-gray-600 mt-1 leading-snug">Scenario "Checkout Flow" breached 500ms threshold.</p>
                    <span className="text-[10px] text-gray-400 mt-2 block font-medium">2 minutes ago</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardOverview;
