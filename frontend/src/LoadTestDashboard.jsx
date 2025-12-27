import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Play, Square, RefreshCw, AlertTriangle, CheckCircle, Activity } from 'lucide-react';

const API_BASE = 'http://localhost:8080/api/v1';

const LoadTestDashboard = () => {
  const [scenarios, setScenarios] = useState([]);
  const [selectedScenario, setSelectedScenario] = useState(null);
  const [stats, setStats] = useState(null);
  const [workerStatus, setWorkerStatus] = useState({ activeWorkerCount: 0 });
  const [latencyData, setLatencyData] = useState([]);
  const [creating, setCreating] = useState(false);

  // New scenario form
  const [newScenario, setNewScenario] = useState({
    name: '',
    targetUrl: 'https://httpbin.org/get',
    method: 'GET',
    durationSeconds: 60,
    numWorkers: 2,
    loadProfile: {
      type: 'CONSTANT',
      initialRps: 100,
      targetRps: 100,
      rampUpSeconds: 0
    }
  });

  useEffect(() => {
    loadScenarios();
    loadWorkerStatus();
    
    const interval = setInterval(() => {
      if (selectedScenario?.status === 'RUNNING') {
        loadStats(selectedScenario.id);
      }
      loadWorkerStatus();
    }, 2000);

    return () => clearInterval(interval);
  }, [selectedScenario]);

  const loadScenarios = async () => {
    try {
      const response = await fetch(`${API_BASE}/scenarios`, {
        headers: { 'X-User-Id': 'demo-user' }
      });
      const data = await response.json();
      setScenarios(data);
    } catch (error) {
      console.error('Failed to load scenarios:', error);
    }
  };

  const loadStats = async (scenarioId) => {
    try {
      const response = await fetch(`${API_BASE}/scenarios/${scenarioId}/stats/realtime?lastNSeconds=30`);
      const data = await response.json();
      setStats(data);
      
      // Update latency chart
      setLatencyData(prev => {
        const newData = [...prev, {
          time: new Date().toLocaleTimeString(),
          p50: data.p50LatencyMs,
          p95: data.p95LatencyMs,
          p99: data.p99LatencyMs
        }];
        return newData.slice(-20); // Keep last 20 points
      });
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  const loadWorkerStatus = async () => {
    try {
      const response = await fetch(`${API_BASE}/workers/status`);
      const data = await response.json();
      setWorkerStatus(data);
    } catch (error) {
      console.error('Failed to load worker status:', error);
    }
  };

  const createScenario = async () => {
    try {
      setCreating(true);
      const response = await fetch(`${API_BASE}/scenarios`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': 'demo-user'
        },
        body: JSON.stringify(newScenario)
      });
      
      const created = await response.json();
      setScenarios([...scenarios, created]);
      setNewScenario({
        name: '',
        targetUrl: 'https://httpbin.org/get',
        method: 'GET',
        durationSeconds: 60,
        numWorkers: 2,
        loadProfile: {
          type: 'CONSTANT',
          initialRps: 100,
          targetRps: 100,
          rampUpSeconds: 0
        }
      });
    } catch (error) {
      console.error('Failed to create scenario:', error);
    } finally {
      setCreating(false);
    }
  };

  const startScenario = async (scenarioId) => {
    try {
      await fetch(`${API_BASE}/scenarios/${scenarioId}/start`, { method: 'POST' });
      await loadScenarios();
    } catch (error) {
      console.error('Failed to start scenario:', error);
    }
  };

  const stopScenario = async (scenarioId) => {
    try {
      await fetch(`${API_BASE}/scenarios/${scenarioId}/stop`, { method: 'POST' });
      await loadScenarios();
    } catch (error) {
      console.error('Failed to stop scenario:', error);
    }
  };

  const statusColors = {
    DRAFT: 'bg-gray-500',
    QUEUED: 'bg-yellow-500',
    RUNNING: 'bg-green-500',
    COMPLETED: 'bg-blue-500',
    FAILED: 'bg-red-500',
    CANCELLED: 'bg-gray-400'
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Load Test Platform</h1>
              <p className="text-gray-600 mt-1">Distributed API testing & observability</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="text-right">
                <div className="text-sm text-gray-600">Active Workers</div>
                <div className="text-2xl font-bold text-green-600 flex items-center gap-2">
                  <Activity className="w-6 h-6" />
                  {workerStatus.activeWorkerCount}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Panel - Scenarios */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-xl font-bold mb-4">Test Scenarios</h2>
              
              {/* Create New Scenario */}
              <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                <h3 className="font-semibold mb-3">Create New Test</h3>
                <div className="space-y-3">
                  <input
                    type="text"
                    placeholder="Scenario name"
                    className="w-full px-3 py-2 border rounded"
                    value={newScenario.name}
                    onChange={e => setNewScenario({...newScenario, name: e.target.value})}
                  />
                  <input
                    type="text"
                    placeholder="Target URL"
                    className="w-full px-3 py-2 border rounded"
                    value={newScenario.targetUrl}
                    onChange={e => setNewScenario({...newScenario, targetUrl: e.target.value})}
                  />
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="number"
                      placeholder="RPS"
                      className="px-3 py-2 border rounded"
                      value={newScenario.loadProfile.initialRps}
                      onChange={e => setNewScenario({
                        ...newScenario,
                        loadProfile: {...newScenario.loadProfile, initialRps: parseInt(e.target.value)}
                      })}
                    />
                    <input
                      type="number"
                      placeholder="Duration (s)"
                      className="px-3 py-2 border rounded"
                      value={newScenario.durationSeconds}
                      onChange={e => setNewScenario({...newScenario, durationSeconds: parseInt(e.target.value)})}
                    />
                  </div>
                  <button
                    onClick={createScenario}
                    disabled={creating || !newScenario.name}
                    className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:bg-gray-400"
                  >
                    {creating ? 'Creating...' : 'Create Scenario'}
                  </button>
                </div>
              </div>

              {/* Scenario List */}
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {scenarios.map(scenario => (
                  <div
                    key={scenario.id}
                    className={`p-3 rounded-lg border-2 cursor-pointer transition ${
                      selectedScenario?.id === scenario.id 
                        ? 'border-blue-500 bg-blue-50' 
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                    onClick={() => {
                      setSelectedScenario(scenario);
                      loadStats(scenario.id);
                    }}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-semibold">{scenario.name}</span>
                      <span className={`px-2 py-1 rounded text-xs text-white ${statusColors[scenario.status]}`}>
                        {scenario.status}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600">
                      {scenario.loadProfile.initialRps} RPS • {scenario.durationSeconds}s
                    </div>
                    <div className="flex gap-2 mt-2">
                      {scenario.status === 'DRAFT' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            startScenario(scenario.id);
                          }}
                          className="flex items-center gap-1 px-2 py-1 bg-green-500 text-white rounded text-xs hover:bg-green-600"
                        >
                          <Play className="w-3 h-3" /> Start
                        </button>
                      )}
                      {scenario.status === 'RUNNING' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            stopScenario(scenario.id);
                          }}
                          className="flex items-center gap-1 px-2 py-1 bg-red-500 text-white rounded text-xs hover:bg-red-600"
                        >
                          <Square className="w-3 h-3" /> Stop
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right Panel - Metrics */}
          <div className="lg:col-span-2 space-y-6">
            {selectedScenario && stats ? (
              <>
                {/* Stats Overview */}
                <div className="grid grid-cols-4 gap-4">
                  <div className="bg-white rounded-lg shadow-sm p-4">
                    <div className="text-sm text-gray-600">Total Requests</div>
                    <div className="text-2xl font-bold">{stats.totalRequests.toLocaleString()}</div>
                  </div>
                  <div className="bg-white rounded-lg shadow-sm p-4">
                    <div className="text-sm text-gray-600">Success Rate</div>
                    <div className="text-2xl font-bold text-green-600">{stats.successRate.toFixed(1)}%</div>
                  </div>
                  <div className="bg-white rounded-lg shadow-sm p-4">
                    <div className="text-sm text-gray-600">Avg Latency</div>
                    <div className="text-2xl font-bold">{stats.avgLatencyMs.toFixed(0)}ms</div>
                  </div>
                  <div className="bg-white rounded-lg shadow-sm p-4">
                    <div className="text-sm text-gray-600">Current RPS</div>
                    <div className="text-2xl font-bold text-blue-600">{stats.currentRps.toFixed(0)}</div>
                  </div>
                </div>

                {/* Latency Chart */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                  <h3 className="text-lg font-bold mb-4">Latency Percentiles</h3>
                  <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={latencyData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="time" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Line type="monotone" dataKey="p50" stroke="#3b82f6" name="P50" />
                      <Line type="monotone" dataKey="p95" stroke="#f59e0b" name="P95" />
                      <Line type="monotone" dataKey="p99" stroke="#ef4444" name="P99" />
                    </LineChart>
                  </ResponsiveContainer>
                </div>

                {/* Status Code Distribution */}
                {stats.statusCodeDistribution && Object.keys(stats.statusCodeDistribution).length > 0 && (
                  <div className="bg-white rounded-lg shadow-sm p-6">
                    <h3 className="text-lg font-bold mb-4">Status Code Distribution</h3>
                    <ResponsiveContainer width="100%" height={200}>
                      <BarChart data={Object.entries(stats.statusCodeDistribution).map(([code, count]) => ({
                        code,
                        count
                      }))}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="code" />
                        <YAxis />
                        <Tooltip />
                        <Bar dataKey="count" fill="#3b82f6" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                )}

                {/* Detailed Metrics */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                  <h3 className="text-lg font-bold mb-4">Detailed Metrics</h3>
                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <div className="text-sm text-gray-600">P50 Latency</div>
                      <div className="text-xl font-semibold">{stats.p50LatencyMs.toFixed(0)}ms</div>
                    </div>
                    <div>
                      <div className="text-sm text-gray-600">P95 Latency</div>
                      <div className="text-xl font-semibold">{stats.p95LatencyMs.toFixed(0)}ms</div>
                    </div>
                    <div>
                      <div className="text-sm text-gray-600">P99 Latency</div>
                      <div className="text-xl font-semibold">{stats.p99LatencyMs.toFixed(0)}ms</div>
                    </div>
                    <div>
                      <div className="text-sm text-gray-600">Min Latency</div>
                      <div className="text-xl font-semibold">{stats.minLatencyMs.toFixed(0)}ms</div>
                    </div>
                    <div>
                      <div className="text-sm text-gray-600">Max Latency</div>
                      <div className="text-xl font-semibold">{stats.maxLatencyMs.toFixed(0)}ms</div>
                    </div>
                    <div>
                      <div className="text-sm text-gray-600">Failed Requests</div>
                      <div className="text-xl font-semibold text-red-600">{stats.failedRequests.toLocaleString()}</div>
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <div className="bg-white rounded-lg shadow-sm p-12 text-center">
                <Activity className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-600">Select a scenario to view metrics</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoadTestDashboard;