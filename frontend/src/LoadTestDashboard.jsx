import React, { useState, useEffect } from 'react';
import MainLayout from './components/layout/MainLayout';
import DashboardOverview from './components/dashboard/DashboardOverview';
import ScenariosView from './components/dashboard/ScenariosView';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { GitCompare } from 'lucide-react';
import { scenarioAPI, exportAPI } from './services/api';

// ========== ComparisonView Component (Connected to Backend) ==========
const ComparisonView = () => {
  const [allScenarios, setAllScenarios] = useState([]);
  const [selectedScenario1, setSelectedScenario1] = useState('');
  const [selectedScenario2, setSelectedScenario2] = useState('');
  const [loadingList, setLoadingList] = useState(true);
  const [comparing, setComparing] = useState(false);
  const [comparisonData, setComparisonData] = useState(null);

  useEffect(() => {
    const fetchScenarios = async () => {
      try {
        setLoadingList(true);
        const res = await scenarioAPI.getAll();
        // Filter out scenarios that don't have completed executions or runs
        setAllScenarios(res.data || []);
      } catch (err) {
        // eslint-disable-next-line no-console
        console.error('Failed to load scenarios for comparison', err);
      } finally {
        setLoadingList(false);
      }
    };
    fetchScenarios();
  }, []);

  const getGrade = (stats) => {
    if (!stats) return 'N/A';
    const success = stats.successRate || 0;
    const latency = stats.avgLatencyMs || 0;
    if (success >= 99.0 && latency < 150) return 'A+';
    if (success >= 99.0 && latency < 300) return 'A';
    if (success >= 98.0 && latency < 500) return 'A-';
    if (success >= 95.0 && latency < 500) return 'B';
    if (success >= 90.0) return 'C';
    return 'F';
  };

  const handleCompare = async () => {
    if (!selectedScenario1 || !selectedScenario2) {
      alert('Please select two scenarios to compare.');
      return;
    }
    try {
      setComparing(true);
      const [r1, r2] = await Promise.all([
        exportAPI.getReport(selectedScenario1),
        exportAPI.getReport(selectedScenario2)
      ]);

      setComparisonData({
        comparisons: [
          {
            name: r1.data.title || "Scenario A",
            grade: getGrade(r1.data.stats),
            stats: {
              successRate: r1.data.stats?.successRate || 0.0,
              avgLatencyMs: r1.data.stats?.avgLatencyMs || 0.0,
              currentRps: r1.data.stats?.avgRps || 0.0
            }
          },
          {
            name: r2.data.title || "Scenario B",
            grade: getGrade(r2.data.stats),
            stats: {
              successRate: r2.data.stats?.successRate || 0.0,
              avgLatencyMs: r2.data.stats?.avgLatencyMs || 0.0,
              currentRps: r2.data.stats?.avgRps || 0.0
            }
          }
        ]
      });
    } catch (err) {
      alert(`Failed to retrieve test reports for comparison: ${err.message}`);
    } finally {
      setComparing(false);
    }
  };

  const chartData = comparisonData ? comparisonData.comparisons.map(c => ({
    name: c.name.substring(0, 20),
    successRate: c.stats.successRate,
    avgLatency: c.stats.avgLatencyMs,
    rps: c.stats.currentRps
  })) : [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Scenario Comparison</h1>
          <p className="text-sm text-gray-500 mt-1">Select two test scenarios to compare their performance side-by-side.</p>
        </div>
      </div>

      <div className="card p-6 bg-white border rounded-lg">
        <h3 className="font-bold text-gray-800 mb-4">Select Scenarios</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">First Scenario</label>
            {loadingList ? (
              <div className="text-sm text-gray-400">Loading...</div>
            ) : (
              <select 
                value={selectedScenario1}
                onChange={(e) => setSelectedScenario1(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-aws-orange focus:border-aws-orange"
              >
                <option value="">Select a scenario...</option>
                {allScenarios.map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Second Scenario</label>
            {loadingList ? (
              <div className="text-sm text-gray-400">Loading...</div>
            ) : (
              <select 
                value={selectedScenario2}
                onChange={(e) => setSelectedScenario2(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-aws-orange focus:border-aws-orange"
              >
                <option value="">Select a scenario...</option>
                {allScenarios.map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            )}
          </div>
        </div>
        <button 
          onClick={handleCompare} 
          disabled={comparing || !selectedScenario1 || !selectedScenario2}
          className="btn-primary flex items-center justify-center gap-2 w-full md:w-auto"
        >
          <GitCompare size={16} /> {comparing ? 'Comparing...' : 'Compare Scenarios'}
        </button>
      </div>

      {comparisonData && (
        <>
          {/* Comparison Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {comparisonData.comparisons.map((comparison, idx) => (
              <div key={idx} className="card relative overflow-hidden group">
                <div className={`absolute top-0 right-0 w-24 h-24 transform translate-x-8 -translate-y-8 rotate-45 ${
                   comparison.grade.startsWith('A') ? 'bg-green-100' :
                   comparison.grade.startsWith('B') ? 'bg-blue-100' : 'bg-yellow-100'
                }`}></div>
                <div className={`absolute top-4 right-4 w-10 h-10 rounded-full flex items-center justify-center text-xl font-bold shadow-sm z-10 ${
                  comparison.grade.startsWith('A') ? 'bg-green-100 text-green-700' :
                  comparison.grade.startsWith('B') ? 'bg-blue-100 text-blue-700' :
                  'bg-yellow-100 text-yellow-700'
                }`}>
                  {comparison.grade}
                </div>

                <h3 className="font-bold text-lg mb-6 text-gray-800">{comparison.name}</h3>
                
                <div className="space-y-4">
                  <div className="flex justify-between items-end border-b pb-2 border-gray-50">
                    <span className="text-sm text-gray-500">Success Rate</span>
                    <span className="text-xl font-bold text-gray-900">{comparison.stats.successRate.toFixed(1)}%</span>
                  </div>
                  <div className="flex justify-between items-end border-b pb-2 border-gray-50">
                    <span className="text-sm text-gray-500">Avg Latency</span>
                    <span className="text-xl font-bold text-gray-900">{Math.round(comparison.stats.avgLatencyMs)}<span className="text-sm font-normal text-gray-400 ml-1">ms</span></span>
                  </div>
                  <div className="flex justify-between items-end">
                    <span className="text-sm text-gray-500">Avg Throughput</span>
                    <span className="text-xl font-bold text-gray-900">{Math.round(comparison.stats.currentRps)} <span className="text-sm font-normal text-gray-400 ml-1">RPS</span></span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Comparison Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="card">
              <h3 className="text-lg font-bold mb-6 text-gray-800">Success Rate Comparison</h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData} barSize={40}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#6b7280'}} />
                  <YAxis domain={[90, 100]} axisLine={false} tickLine={false} tick={{fill: '#6b7280'}} />
                  <Tooltip cursor={{fill: 'transparent'}} />
                  <Bar dataKey="successRate" fill="#10b981" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="card">
              <h3 className="text-lg font-bold mb-6 text-gray-800">Latency Comparison (Lower is better)</h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData} barSize={40}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#6b7280'}} />
                  <YAxis axisLine={false} tickLine={false} tick={{fill: '#6b7280'}} />
                  <Tooltip cursor={{fill: 'transparent'}} />
                  <Bar dataKey="avgLatency" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

const App = () => {
  const [activeView, setActiveView] = useState('overview');

  return (
    <MainLayout activeView={activeView} setActiveView={setActiveView}>
      {activeView === 'overview' && <DashboardOverview />}
      {activeView === 'scenarios' && <ScenariosView />}
      {activeView === 'compare' && <ComparisonView />}
      
      {/* Placeholders for other views to show navigation works */}
      {activeView === 'templates' && (
        <div className="text-center py-20">
            <h2 className="text-xl font-bold text-gray-400">Templates Module Loading...</h2>
        </div>
      )}
      {activeView === 'runs' && (
        <div className="text-center py-20">
            <h2 className="text-xl font-bold text-gray-400">Test Runs History</h2>
        </div>
      )}
      {activeView === 'analytics' && (
        <div className="text-center py-20">
            <h2 className="text-xl font-bold text-gray-400">Deep Analytics & Trends</h2>
        </div>
      )}
    </MainLayout>
  );
};

export default App;