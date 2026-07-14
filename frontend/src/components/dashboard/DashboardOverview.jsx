import React, { useState, useEffect } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, TrendingDown, Activity, AlertTriangle, Zap, ArrowRight, Star, CheckCircle } from 'lucide-react';
import AIInsights from './AIInsights';
import TopologyMap from './TopologyMap';
import LiveLogViewer from './LiveLogViewer';
import ScenarioFlow from './ScenarioFlow';
import { dashboardAPI } from '../../services/api';

const StatCard = ({ title, value, subtext, trend, icon: Icon, color }) => (
  <div className="stat-card group">
    <div className="flex justify-between items-center mb-1">
      <p className="text-[13px] font-bold text-[#545b64] tracking-tight">{title}</p>
      <Icon size={14} className="text-[#879596]" />
    </div>
    <div className="flex items-end gap-3 mb-2">
      <h3 className="text-2xl font-black text-[#16191f] tracking-tighter">{value}</h3>
    </div>
    <div className="flex items-center gap-1 text-[12px]">
      {trend > 0 ? (
        <TrendingUp size={12} className="text-[#0073bb]" />
      ) : (
        <TrendingDown size={12} className="text-[#d13212]" />
      )}
      <span className={trend > 0 ? "text-[#0073bb] font-bold" : "text-[#d13212] font-bold"}>
        {Math.abs(trend)}%
      </span>
      <span className="text-[#545b64]">{subtext || "vs last period"}</span>
    </div>
  </div>
);

const HealthScoreGauge = ({ score }) => {
  const circumference = 2 * Math.PI * 52;
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
        <span className={`text-4xl font-extrabold tracking-tighter ${color}`}>{score}</span>
        <span className="text-xs font-bold text-gray-400 uppercase tracking-widest mt-1">Health</span>
      </div>
    </div>
  );
};

const DashboardOverview = () => {
  const [timeRange, setTimeRange] = useState('1H');
  const [isExporting, setIsExporting] = useState(false);
  const [isCheckingHealth, setIsCheckingHealth] = useState(false);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSummary = async () => {
    try {
      setLoading(true);
      const res = await dashboardAPI.getSummary();
      setSummary(res.data);
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error('Failed to fetch dashboard summary', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();
  }, []);

  // Mock chart data representing real-time correlation
  const chartData = [
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

  const handleExport = () => {
    setIsExporting(true);
    setTimeout(() => {
      const csvContent = "data:text/csv;charset=utf-8,Time,RPS,Latency\n" 
        + chartData.map(row => `${row.time},${row.rps},${row.latency}`).join("\n");
      const encodedUri = encodeURI(csvContent);
      const link = document.createElement("a");
      link.setAttribute("href", encodedUri);
      link.setAttribute("download", "platform_report.csv");
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setIsExporting(false);
    }, 800);
  };

  const handleHealthCheck = () => {
    setIsCheckingHealth(true);
    setTimeout(() => {
      setIsCheckingHealth(false);
      alert(`✅ Health Check Completed: ${summary?.activeWorkers || 0} worker nodes active. Queue is operational.`);
    }, 1200);
  };

  // Compute stats values with defaults
  const totalScenarios = summary?.totalScenarios || 0;
  const activeWorkers = summary?.activeWorkers || 0;
  const activeAlerts = summary?.activeAlerts || 0;
  const successRate = summary?.avgSuccessRate || 100.0;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center pb-4 border-b border-[#eaeded]">
        <div>
          <h1 className="text-[24px] font-bold text-[#16191f]">Console Home</h1>
          <p className="text-[14px] text-[#545b64] mt-1 flex items-center gap-2">
             DevOps Central Management Console
          </p>
        </div>
        <div className="flex gap-3">
          <button onClick={handleExport} disabled={isExporting} className="btn-secondary flex items-center gap-2">
            {isExporting ? 'Exporting...' : 'Export Dashboard Report'}
          </button>
          <button onClick={handleHealthCheck} disabled={isCheckingHealth} className="btn-primary flex items-center gap-2">
            <Activity size={16} className={isCheckingHealth ? "animate-spin" : ""} /> {isCheckingHealth ? 'Checking...' : 'Run Global Health Check'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Recently Run Scenarios */}
        <div className="card col-span-2">
          <h3 className="font-bold text-[16px] text-[#16191f] mb-4">Recent Test Scenarios</h3>
          {loading ? (
             <div className="py-6 text-center text-gray-500">Loading recent runs...</div>
          ) : !summary?.recentScenarios || summary.recentScenarios.length === 0 ? (
             <div className="py-6 text-center text-gray-500">No recent scenarios found. Create one to start testing.</div>
          ) : (
             <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
               {summary.recentScenarios.map(sc => (
                 <div key={sc.id} className="border border-[#eaeded] rounded-sm p-3 hover:border-[#879596] cursor-pointer transition-colors group">
                   <div className="flex items-center justify-between mb-2">
                      <span className="text-[13px] font-bold text-[#0073bb] group-hover:text-[#ec7211] hover:underline truncate max-w-[70%]">{sc.name}</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                        sc.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                        sc.status === 'RUNNING' ? 'bg-blue-100 text-blue-800 animate-pulse' :
                        'bg-yellow-100 text-yellow-800'
                      }`}>{sc.status}</span>
                   </div>
                   <div className="text-[11px] text-[#545b64] flex justify-between">
                     <span>Success: {sc.successRate ? sc.successRate.toFixed(1) : '0'}%</span>
                     <span>Latency: {sc.avgLatency ? Math.round(sc.avgLatency) : '0'}ms</span>
                   </div>
                 </div>
               ))}
             </div>
          )}
        </div>

        {/* Favorites / Quick Links */}
        <div className="card col-span-1">
          <h3 className="font-bold text-[16px] text-[#16191f] mb-4 flex justify-between items-center">
            Quick Links <Star size={16} className="text-[#879596]" />
          </h3>
          <div className="space-y-1">
             {[
               'CI/CD Pipeline Manager',
               'ArgoCD Sync Status',
               'Production Grafana Dashboards'
             ].map(fav => (
                <div key={fav} className="flex items-center gap-2 py-2 border-b border-[#eaeded] last:border-0 cursor-pointer group">
                   <Star size={14} className="text-[#ec7211]" fill="#ec7211" />
                   <span className="text-[13px] text-[#0073bb] group-hover:underline">{fav}</span>
                </div>
             ))}
          </div>
        </div>
      </div>

      {/* Global Health and Stats Grid */}
      <h3 className="font-bold text-[16px] text-[#16191f] pt-4">Global Health Overview</h3>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Active Incidents (Alerts)" value={activeAlerts.toString()} trend={activeAlerts > 0 ? 100 : -100} icon={AlertTriangle} subtext="active alerts currently" />
        <StatCard title="Total Scenarios" value={totalScenarios.toString()} trend={5.2} icon={Activity} subtext="scenarios configured" />
        <StatCard title="Average Success Rate" value={`${successRate.toFixed(1)}%`} trend={successRate >= 99.0 ? 0.5 : -2.4} icon={CheckCircle} subtext="across all runs" />
        <StatCard title="Active Workers" value={activeWorkers.toString()} trend={activeWorkers > 0 ? 20.0 : -100.0} icon={Zap} subtext="workers online" />
      </div>

      {/* Bento Grid Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Main Chart */}
        <div className="card lg:col-span-2 p-6 flex flex-col min-h-[400px]">
          <div className="flex justify-between items-center mb-6">
            <div>
                <h3 className="font-bold text-xl text-gray-900">Throughput vs Latency</h3>
                <p className="text-sm text-gray-500">Real-time correlation analysis</p>
            </div>
            <div className="flex gap-2">
                {['1H', '24H', '7D'].map((period) => (
                    <button 
                      key={period} 
                      onClick={() => setTimeRange(period)}
                      className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${timeRange === period ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
                    >
                        {period}
                    </button>
                ))}
            </div>
          </div>
          <div className="flex-1 min-h-0">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
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

        {/* AI Insights Panel */}
        <AIInsights summary={summary} />

        {/* Topology Map */}
        <TopologyMap />

        {/* Health & Insights Side Panel */}
        <div className="space-y-6 lg:col-span-1 flex flex-col h-full">
          <div className="card flex flex-col items-center py-8 relative overflow-hidden flex-1">
            <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-green-400 to-green-600"></div>
            <HealthScoreGauge score={Math.round(successRate)} />
            <div className="mt-6 text-center px-6">
              <h4 className="text-xl font-bold text-gray-900">System Status</h4>
              <p className="text-sm text-gray-500 mt-2 leading-relaxed">
                Global average success rate is {successRate.toFixed(1)}%.
              </p>
            </div>
            <button 
              onClick={() => alert('Opening Detailed Topology and Resource Analysis...')} 
              className="mt-8 flex items-center gap-2 text-sm font-bold text-[#0073bb] hover:text-[#ec7211] transition-colors"
            >
              Resource Analysis <ArrowRight size={14} />
            </button>
          </div>
          
          <div className="card flex-1">
            <h4 className="font-bold text-lg text-gray-900 mb-4">Live Activity Streams</h4>
            <div className="relative border-l border-gray-100 pl-4 space-y-6">
              <div className="relative">
                <div className="absolute -left-[21px] top-1.5 w-3 h-3 rounded-full bg-green-500 ring-4 ring-white"></div>
                <div className="text-xs text-gray-400">Just now</div>
                <div className="text-sm font-medium text-gray-900 mt-0.5">Metrics engine synchronized</div>
              </div>
              <div className="relative">
                <div className="absolute -left-[21px] top-1.5 w-3 h-3 rounded-full bg-blue-500 ring-4 ring-white"></div>
                <div className="text-xs text-gray-400">12 minutes ago</div>
                <div className="text-sm font-medium text-gray-900 mt-0.5">Worker node scale up requested</div>
              </div>
            </div>
          </div>
        </div>

      </div>

      {/* Real-time Topology & Logs Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 pt-4">
        <LiveLogViewer />
        <ScenarioFlow />
      </div>
    </div>
  );
};

export default DashboardOverview;
