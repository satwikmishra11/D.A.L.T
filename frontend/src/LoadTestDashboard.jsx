
import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { TrendingUp, TrendingDown, GitCompare, History, Users, DollarSign, Zap, Award, Target, Clock } from 'lucide-react';

// ========== ComparisonView Component ==========
const ComparisonView = ({ scenarios }) => {
  const [comparisonData, setComparisonData] = useState(null);
  const [loading, setLoading] = useState(false);

  const compareScenarios = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v1/compare', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scenarioIds: scenarios.map(s => s.id) })
      });
      const data = await response.json();
      setComparisonData(data);
    } catch (error) {
      console.error('Comparison failed:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (scenarios.length >= 2) {
      compareScenarios();
    }
  }, [scenarios]);

  if (loading) {
    return <div className="flex items-center justify-center p-12">
      <div className="spinner"></div>
    </div>;
  }

  if (!comparisonData) {
    return <div className="text-center p-12 text-gray-500">
      Select at least 2 scenarios to compare
    </div>;
  }

  const chartData = comparisonData.comparisons.map(c => ({
    name: c.name.substring(0, 20),
    successRate: c.stats.successRate,
    avgLatency: c.stats.avgLatencyMs,
    rps: c.stats.currentRps
  }));

  return (
    <div className="space-y-6">
      {/* Comparison Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {comparisonData.comparisons.map((comparison, idx) => (
          <div key={idx} className="bg-white rounded-lg shadow-sm p-6 relative">
            {/* Grade Badge */}
            <div className={`absolute top-4 right-4 w-12 h-12 rounded-full flex items-center justify-center text-xl font-bold ${
              comparison.grade.startsWith('A') ? 'bg-green-100 text-green-800' :
              comparison.grade.startsWith('B') ? 'bg-blue-100 text-blue-800' :
              'bg-yellow-100 text-yellow-800'
            }`}>
              {comparison.grade}
            </div>

            <h3 className="font-bold text-lg mb-4 pr-16">{comparison.name}</h3>
            
            <div className="space-y-3">
              <div>
                <div className="text-sm text-gray-600">Success Rate</div>
                <div className="text-2xl font-bold">{comparison.stats.successRate.toFixed(2)}%</div>
              </div>
              <div>
                <div className="text-sm text-gray-600">Avg Latency</div>
                <div className="text-2xl font-bold">{Math.round(comparison.stats.avgLatencyMs)}ms</div>
              </div>
              <div>
                <div className="text-sm text-gray-600">Throughput</div>
                <div className="text-2xl font-bold">{Math.round(comparison.stats.currentRps)} RPS</div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Comparison Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Success Rate Comparison */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-bold mb-4">Success Rate Comparison</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis domain={[0, 100]} />
              <Tooltip />
              <Bar dataKey="successRate" fill="#10b981" name="Success Rate %" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Latency Comparison */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-bold mb-4">Latency Comparison</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="avgLatency" fill="#3b82f6" name="Avg Latency (ms)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Insights */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
          <Target className="w-5 h-5" />
          Key Insights
        </h3>
        <ul className="space-y-2">
          {comparisonData.insights.map((insight, idx) => (
            <li key={idx} className="flex items-start gap-2">
              <Award className="w-4 h-4 text-blue-600 mt-1" />
              <span>{insight}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Recommendation */}
      <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg p-6 border-l-4 border-blue-500">
        <h3 className="font-bold text-lg mb-2">🏆 Winner</h3>
        <p className="text-gray-700">{comparisonData.recommendation}</p>
      </div>
    </div>
  );
};

// ========== TrendsView Component ==========
const TrendsView = ({ userId }) => {
  const [trends, setTrends] = useState(null);
  const [period, setPeriod] = useState(30);

  useEffect(() => {
    loadTrends();
  }, [period]);

  const loadTrends = async () => {
    try {
      const response = await fetch(`/api/v1/history/trends?days=${period}`);
      const data = await response.json();
      setTrends(data);
    } catch (error) {
      console.error('Failed to load trends:', error);
    }
  };

  if (!trends) {
    return <div className="flex items-center justify-center p-12">
      <div className="spinner"></div>
    </div>;
  }

  const getTrendIcon = (trend) => {
    if (trend === 'IMPROVING') return <TrendingUp className="w-5 h-5 text-green-600" />;
    if (trend === 'DEGRADING') return <TrendingDown className="w-5 h-5 text-red-600" />;
    return <div className="w-5 h-5 text-gray-400">—</div>;
  };

  return (
    <div className="space-y-6">
      {/* Period Selector */}
      <div className="flex gap-2">
        {[7, 30, 90].map(days => (
          <button
            key={days}
            onClick={() => setPeriod(days)}
            className={`px-4 py-2 rounded-lg ${
              period === days ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'
            }`}
          >
            {days} Days
          </button>
        ))}
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="stat-card">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-600">Total Tests</span>
            <History className="w-5 h-5 text-blue-600" />
          </div>
          <div className="text-3xl font-bold">{trends.totalTests}</div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-600">Success Rate Trend</span>
            {getTrendIcon(trends.trends.successRate)}
          </div>
          <div className="text-2xl font-bold">{trends.trends.successRate}</div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-600">Latency Trend</span>
            {getTrendIcon(trends.trends.latency)}
          </div>
          <div className="text-2xl font-bold">{trends.trends.latency}</div>
        </div>
      </div>

      {/* Trend Chart */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-bold mb-4">Performance Over Time</h3>
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={trends.dailyMetrics}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis yAxisId="left" />
            <YAxis yAxisId="right" orientation="right" />
            <Tooltip />
            <Legend />
            <Line 
              yAxisId="left"
              type="monotone" 
              dataKey="avgSuccessRate" 
              stroke="#10b981" 
              name="Success Rate %"
              strokeWidth={2}
            />
            <Line 
              yAxisId="right"
              type="monotone" 
              dataKey="avgLatency" 
              stroke="#3b82f6" 
              name="Avg Latency (ms)"
              strokeWidth={2}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

// ========== TemplatesView Component ==========
const TemplatesView = () => {
  const [templates, setTemplates] = useState([]);
  const [category, setCategory] = useState('all');

  useEffect(() => {
    loadTemplates();
  }, [category]);

  const loadTemplates = async () => {
    try {
      const response = await fetch('/api/v1/templates/public');
      const data = await response.json();
      setTemplates(data);
    } catch (error) {
      console.error('Failed to load templates:', error);
    }
  };

  const applyTemplate = async (templateId) => {
    try {
      const response = await fetch(`/api/v1/templates/${templateId}/apply`, {
        method: 'POST'
      });
      const scenario = await response.json();
      // Navigate to scenario or show success
      console.log('Template applied:', scenario);
    } catch (error) {
      console.error('Failed to apply template:', error);
    }
  };

  const categories = [
    { value: 'all', label: 'All Templates', icon: '📋' },
    { value: 'API_TEST', label: 'API Tests', icon: '🔌' },
    { value: 'READ_TEST', label: 'Read Heavy', icon: '📖' },
    { value: 'WRITE_TEST', label: 'Write Heavy', icon: '✍️' },
  ];

  return (
    <div className="space-y-6">
      {/* Category Filter */}
      <div className="flex gap-2 overflow-x-auto">
        {categories.map(cat => (
          <button
            key={cat.value}
            onClick={() => setCategory(cat.value)}
            className={`px-4 py-2 rounded-lg whitespace-nowrap ${
              category === cat.value ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700'
            }`}
          >
            {cat.icon} {cat.label}
          </button>
        ))}
      </div>

      {/* Templates Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {templates.map(template => (
          <div key={template.id} className="bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition">
            <div className="flex items-start justify-between mb-3">
              <h3 className="font-bold text-lg">{template.name}</h3>
              <span className="text-xs px-2 py-1 bg-blue-100 text-blue-800 rounded">
                {template.category}
              </span>
            </div>
            
            <p className="text-sm text-gray-600 mb-4">{template.description}</p>
            
            <div className="space-y-2 mb-4 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Method:</span>
                <span className="font-medium">{template.method}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Load Type:</span>
                <span className="font-medium">{template.loadProfile.type}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Used:</span>
                <span className="font-medium">{template.usageCount}× times</span>
              </div>
            </div>
            
            <button
              onClick={() => applyTemplate(template.id)}
              className="w-full btn-primary"
            >
              Use Template
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

// ========== CostEstimator Component ==========
const CostEstimator = ({ scenario }) => {
  const [estimate, setEstimate] = useState(null);

  useEffect(() => {
    if (scenario) {
      calculateCost();
    }
  }, [scenario]);

  const calculateCost = async () => {
    try {
      const response = await fetch('/api/v1/cost/estimate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(scenario)
      });
      const data = await response.json();
      setEstimate(data);
    } catch (error) {
      console.error('Failed to calculate cost:', error);
    }
  };

  if (!estimate) return null;

  return (
    <div className="bg-gradient-to-br from-green-50 to-blue-50 rounded-lg p-6 border border-green-200">
      <div className="flex items-center gap-2 mb-4">
        <DollarSign className="w-6 h-6 text-green-600" />
        <h3 className="text-lg font-bold">Estimated Cost</h3>
      </div>

      <div className="text-4xl font-bold text-green-600 mb-4">
        ${estimate.totalCost.toFixed(2)}
      </div>

      <div className="space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-600">Worker Infrastructure:</span>
          <span className="font-medium">${estimate.workerCost.toFixed(2)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-600">Request Processing:</span>
          <span className="font-medium">${estimate.requestCost.toFixed(2)}</span>
        </div>
        <div className="flex justify-between pt-2 border-t">
          <span className="text-gray-600">Estimated Requests:</span>
          <span className="font-medium">{estimate.estimatedRequests.toLocaleString()}</span>
        </div>
      </div>
    </div>
  );
};

// ========== Main Load Test Dashboard ==========
const LoadTestDashboard = () => {
  const [activeView, setActiveView] = useState('overview');

  const views = [
    { id: 'overview', label: 'Overview', icon: <Zap /> },
    { id: 'compare', label: 'Compare', icon: <GitCompare /> },
    { id: 'trends', label: 'Trends', icon: <TrendingUp /> },
    { id: 'templates', label: 'Templates', icon: <Target /> },
    { id: 'team', label: 'Team', icon: <Users /> },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Enhanced Navigation */}
      <nav className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center gap-6">
            {views.map(view => (
              <button
                key={view.id}
                onClick={() => setActiveView(view.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg transition ${
                  activeView === view.id
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                {view.icon}
                <span>{view.label}</span>
              </button>
            ))}
          </div>
        </div>
      </nav>

      {/* Content Area */}
      <div className="max-w-7xl mx-auto px-6 py-6">
        {activeView === 'compare' && <ComparisonView scenarios={[]} />}
        {activeView === 'trends' && <TrendsView userId="user123" />}
        {activeView === 'templates' && <TemplatesView />}
      </div>
    </div>
  );
};

export default LoadTestDashboard;