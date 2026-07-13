import React, { useState, useEffect } from 'react';
import { Plus, Play, Square, Edit, FileText, CheckCircle, Clock } from 'lucide-react';
import { Button } from '../ui/Button';
import { DataTable } from '../ui/DataTable';
import { Tabs } from '../ui/Tabs';
import { Modal } from '../ui/Modal';
import { Card, CardContent } from '../ui/Card';
import { scenarioAPI } from '../../services/api';
import ScenarioForm from '../ScenarioForm';

const ScenariosView = () => {
  const [activeTab, setActiveTab] = useState('all');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [scenarios, setScenarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingScenario, setEditingScenario] = useState(null);
  
  // Default new scenario template
  const defaultNewScenario = {
    name: '',
    description: '',
    targetUrl: '',
    method: 'GET',
    headers: {},
    body: '',
    durationSeconds: 60,
    numWorkers: 1,
    loadProfile: {
      type: 'CONSTANT',
      initialRps: 10,
      targetRps: 10,
      rampUpSeconds: 0,
      bursts: []
    },
    slaConfig: {
      minSuccessRate: 99.0,
      maxAvgLatencyMs: 500.0,
      maxP95LatencyMs: 1000.0,
      maxP99LatencyMs: 2000.0,
      maxErrorRate: 1.0
    },
    alerts: [],
    ignoreTlsErrors: true
  };

  const [currentFormScenario, setCurrentFormScenario] = useState(defaultNewScenario);

  const loadScenarios = async () => {
    try {
      setLoading(true);
      const res = await scenarioAPI.getAll();
      setScenarios(res.data);
    } catch (err) {
      console.error('Failed to load scenarios', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadScenarios();
  }, []);

  const handleStart = async (id) => {
    try {
      await scenarioAPI.start(id);
      alert('Load test execution started successfully!');
      loadScenarios();
    } catch (err) {
      alert(`Failed to start load test: ${err.response?.data?.message || err.message}`);
    }
  };

  const handleStop = async (id) => {
    try {
      await scenarioAPI.stop(id);
      alert('Load test execution stopped!');
      loadScenarios();
    } catch (err) {
      alert(`Failed to stop load test: ${err.response?.data?.message || err.message}`);
    }
  };

  const handleCreateOrUpdate = async () => {
    try {
      await scenarioAPI.create(currentFormScenario);
      setIsModalOpen(false);
      loadScenarios();
    } catch (err) {
      alert(`Failed to save scenario: ${err.response?.data?.message || err.message}`);
    }
  };

  const tabs = [
    { id: 'all', label: 'All Scenarios', icon: FileText },
    { id: 'approved', label: 'Approved', icon: CheckCircle },
    { id: 'draft', label: 'Drafts', icon: Edit },
  ];

  const filteredData = activeTab === 'all' 
    ? scenarios 
    : scenarios.filter(s => s.status?.toLowerCase() === activeTab);

  const columns = [
    { 
      key: 'name', 
      header: 'Scenario Name',
      render: (row) => (
        <div>
          <div className="font-bold text-gray-900">{row.name}</div>
          <div className="text-xs text-gray-500 font-mono mt-0.5">{row.method} {row.targetUrl}</div>
        </div>
      )
    },
    { 
      key: 'status', 
      header: 'Status',
      render: (row) => (
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
          row.status === 'APPROVED' ? 'bg-green-100 text-green-800' :
          row.status === 'DRAFT' ? 'bg-gray-100 text-gray-800' :
          'bg-yellow-100 text-yellow-800'
        }`}>
          {row.status}
        </span>
      )
    },
    { key: 'users', header: 'Virtual Users', render: (row) => row.numWorkers?.toLocaleString() || '1' },
    { 
      key: 'lastRun', 
      header: 'Last Execution', 
      render: (row) => (
        <div className="flex items-center gap-1 text-gray-500">
          <Clock size={14} /> {row.lastExecutedAt ? new Date(row.lastExecutedAt).toLocaleDateString() : 'Never'}
        </div>
      )
    },
    {
      key: 'actions',
      header: '',
      render: (row) => (
        <div className="flex items-center gap-2">
          {row.running ? (
            <Button onClick={() => handleStop(row.id)} variant="ghost" size="sm" icon={Square} className="text-red-600 hover:bg-red-50" />
          ) : (
            <Button onClick={() => handleStart(row.id)} variant="ghost" size="sm" icon={Play} disabled={row.status !== 'APPROVED'} />
          )}
          <Button onClick={() => {
            setCurrentFormScenario(row);
            setEditingScenario(row.id);
            setIsModalOpen(true);
          }} variant="ghost" size="sm" icon={Edit} />
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Test Scenarios</h1>
          <p className="text-gray-500 mt-1">Manage and execute your load testing configurations.</p>
        </div>
        <Button onClick={() => {
          setCurrentFormScenario(defaultNewScenario);
          setEditingScenario(null);
          setIsModalOpen(true);
        }} icon={Plus}>
          New Scenario
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
            <div className="p-6 pb-0">
                <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />
            </div>
            <div className="p-6 pt-0">
                {loading ? (
                    <div className="py-10 text-center text-gray-500">Loading scenarios...</div>
                ) : (
                    <DataTable 
                        columns={columns} 
                        data={filteredData} 
                    />
                )}
            </div>
        </CardContent>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingScenario ? "Edit Scenario" : "Create New Scenario"}
        className="max-w-4xl"
        footer={
          <div className="flex justify-end gap-2 p-4 border-t w-full">
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button onClick={handleCreateOrUpdate}>{editingScenario ? "Save Changes" : "Create Scenario"}</Button>
          </div>
        }
      >
        <div className="max-h-[60vh] overflow-y-auto p-2">
          <ScenarioForm 
            scenario={currentFormScenario} 
            onChange={setCurrentFormScenario}
            creating={!editingScenario}
          />
        </div>
      </Modal>
    </div>
  );
};

export default ScenariosView;
