import React, { useState } from 'react';
import { Plus, Play, Edit, Trash2, FileText, CheckCircle, Clock } from 'lucide-react';
import { Button } from '../ui/Button';
import { DataTable } from '../ui/DataTable';
import { Tabs } from '../ui/Tabs';
import { Modal } from '../ui/Modal';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../ui/Card';

const ScenariosView = () => {
  const [activeTab, setActiveTab] = useState('all');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [scenarios, setScenarios] = useState([
    { id: '1', name: 'Checkout Flow - Black Friday', method: 'POST', target: 'https://api.shop.com/checkout', status: 'APPROVED', lastRun: '2 hours ago', users: 5000 },
    { id: '2', name: 'User Registration Spike', method: 'POST', target: 'https://api.shop.com/register', status: 'DRAFT', lastRun: 'Never', users: 1000 },
    { id: '3', name: 'Product Search Load', method: 'GET', target: 'https://api.shop.com/search', status: 'APPROVED', lastRun: '1 day ago', users: 2500 },
    { id: '4', name: 'Cart Update Stress', method: 'PUT', target: 'https://api.shop.com/cart', status: 'PENDING', lastRun: '5 days ago', users: 3000 },
  ]);

  const tabs = [
    { id: 'all', label: 'All Scenarios', icon: FileText },
    { id: 'approved', label: 'Approved', icon: CheckCircle },
    { id: 'draft', label: 'Drafts', icon: Edit },
  ];

  const filteredData = activeTab === 'all' 
    ? scenarios 
    : scenarios.filter(s => s.status.toLowerCase() === activeTab);

  const columns = [
    { 
      key: 'name', 
      header: 'Scenario Name',
      render: (row) => (
        <div>
          <div className="font-bold text-gray-900">{row.name}</div>
          <div className="text-xs text-gray-500 font-mono mt-0.5">{row.method} {row.target}</div>
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
    { key: 'users', header: 'Virtual Users', render: (row) => row.users.toLocaleString() },
    { 
      key: 'lastRun', 
      header: 'Last Execution', 
      render: (row) => (
        <div className="flex items-center gap-1 text-gray-500">
          <Clock size={14} /> {row.lastRun}
        </div>
      )
    },
    {
      key: 'actions',
      header: '',
      render: () => (
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" icon={Play} />
          <Button variant="ghost" size="sm" icon={Edit} />
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
        <Button onClick={() => setIsModalOpen(true)} icon={Plus}>
          New Scenario
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
            <div className="p-6 pb-0">
                <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />
            </div>
            <div className="p-6 pt-0">
                <DataTable 
                    columns={columns} 
                    data={filteredData} 
                />
            </div>
        </CardContent>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create New Scenario"
        footer={
          <>
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button onClick={() => setIsModalOpen(false)}>Create Scenario</Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Scenario Name</label>
            <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-aws-orange focus:border-aws-orange" placeholder="e.g. Black Friday Checkout" />
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Method</label>
                <select className="w-full px-3 py-2 border border-gray-300 rounded-md">
                    <option>GET</option>
                    <option>POST</option>
                    <option>PUT</option>
                    <option>DELETE</option>
                </select>
             </div>
             <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Virtual Users</label>
                <input type="number" className="w-full px-3 py-2 border border-gray-300 rounded-md" placeholder="1000" />
             </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Target URL</label>
            <input type="url" className="w-full px-3 py-2 border border-gray-300 rounded-md" placeholder="https://api.example.com/v1/..." />
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default ScenariosView;
