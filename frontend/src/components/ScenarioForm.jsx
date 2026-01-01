// ========== src/components/ScenarioForm.jsx ==========
import React, { useState } from 'react';
import { X, Plus, Trash2 } from 'lucide-react';
import { HTTP_METHODS, LOAD_PROFILE_TYPES, ENVIRONMENTS, ALERT_CHANNELS } from '../utils/constants';

const ScenarioForm = ({ scenario, onChange, onSubmit, onCancel, creating }) => {
  const [activeTab, setActiveTab] = useState('basic');

  const updateField = (field, value) => {
    onChange({ ...scenario, [field]: value });
  };

  const updateLoadProfile = (field, value) => {
    onChange({
      ...scenario,
      loadProfile: { ...scenario.loadProfile, [field]: value }
    });
  };

  const updateSlaConfig = (field, value) => {
    onChange({
      ...scenario,
      slaConfig: { ...scenario.slaConfig, [field]: value }
    });
  };

  const addHeader = () => {
    const key = prompt('Header name:');
    const value = prompt('Header value:');
    if (key && value) {
      onChange({
        ...scenario,
        headers: { ...scenario.headers, [key]: value }
      });
    }
  };

  const removeHeader = (key) => {
    const newHeaders = { ...scenario.headers };
    delete newHeaders[key];
    onChange({ ...scenario, headers: newHeaders });
  };

  const addBurst = () => {
    const bursts = scenario.loadProfile.bursts || [];
    updateLoadProfile('bursts', [
      ...bursts,
      { startSecond: 0, durationSeconds: 5, rps: 1000 }
    ]);
  };

  const removeBurst = (index) => {
    const bursts = [...scenario.loadProfile.bursts];
    bursts.splice(index, 1);
    updateLoadProfile('bursts', bursts);
  };

  const updateBurst = (index, field, value) => {
    const bursts = [...scenario.loadProfile.bursts];
    bursts[index] = { ...bursts[index], [field]: parseInt(value) };
    updateLoadProfile('bursts', bursts);
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">Create Load Test Scenario</h2>
        <button onClick={onCancel} className="p-2 hover:bg-gray-100 rounded-lg">
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b">
        {['basic', 'load', 'sla', 'alerts'].map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 font-medium border-b-2 transition ${
              activeTab === tab
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-600 hover:text-gray-900'
            }`}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)} Config
          </button>
        ))}
      </div>

      <div className="space-y-6">
        {/* Basic Configuration */}
        {activeTab === 'basic' && (
          <div className="space-y-4">
            <div>
              <label className="label">Scenario Name *</label>
              <input
                type="text"
                className="input-field"
                value={scenario.name}
                onChange={(e) => updateField('name', e.target.value)}
                placeholder="e.g., API Health Check"
              />
            </div>

            <div>
              <label className="label">Description</label>
              <textarea
                className="input-field"
                rows="3"
                value={scenario.description}
                onChange={(e) => updateField('description', e.target.value)}
                placeholder="Optional description of this test scenario"
              />
            </div>

            <div>
              <label className="label">Target URL *</label>
              <input
                type="url"
                className="input-field"
                value={scenario.targetUrl}
                onChange={(e) => updateField('targetUrl', e.target.value)}
                placeholder="https://api.example.com/endpoint"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">HTTP Method *</label>
                <select
                  className="input-field"
                  value={scenario.method}
                  onChange={(e) => updateField('method', e.target.value)}
                >
                  {HTTP_METHODS.map(method => (
                    <option key={method} value={method}>{method}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="label">Environment</label>
                <select
                  className="input-field"
                  value={scenario.environment}
                  onChange={(e) => updateField('environment', e.target.value)}
                >
                  {ENVIRONMENTS.map(env => (
                    <option key={env.value} value={env.value}>{env.label}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Headers */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="label">Request Headers</label>
                <button onClick={addHeader} className="text-sm text-blue-600 hover:text-blue-700">
                  + Add Header
                </button>
              </div>
              {Object.entries(scenario.headers || {}).map(([key, value]) => (
                <div key={key} className="flex gap-2 mb-2">
                  <input type="text" value={key} disabled className="input-field flex-1" />
                  <input type="text" value={value} disabled className="input-field flex-1" />
                  <button onClick={() => removeHeader(key)} className="p-2 text-red-600 hover:bg-red-50 rounded">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>

            {/* Request Body */}
            {['POST', 'PUT', 'PATCH'].includes(scenario.method) && (
              <div>
                <label className="label">Request Body</label>
                <textarea
                  className="input-field font-mono text-sm"
                  rows="6"
                  value={scenario.body}
                  onChange={(e) => updateField('body', e.target.value)}
                  placeholder='{"key": "value"}'
                />
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">Duration (seconds) *</label>
                <input
                  type="number"
                  className="input-field"
                  value={scenario.durationSeconds}
                  onChange={(e) => updateField('durationSeconds', parseInt(e.target.value))}
                  min="1"
                  max="7200"
                />
              </div>

              <div>
                <label className="label">Number of Workers *</label>
                <input
                  type="number"
                  className="input-field"
                  value={scenario.numWorkers}
                  onChange={(e) => updateField('numWorkers', parseInt(e.target.value))}
                  min="1"
                  max="100"
                />
              </div>
            </div>
          </div>
        )}

        {/* Load Profile Configuration */}
        {activeTab === 'load' && (
          <div className="space-y-4">
            <div>
              <label className="label">Load Profile Type *</label>
              <select
                className="input-field"
                value={scenario.loadProfile.type}
                onChange={(e) => updateLoadProfile('type', e.target.value)}
              >
                {LOAD_PROFILE_TYPES.map(type => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">Initial RPS *</label>
                <input
                  type="number"
                  className="input-field"
                  value={scenario.loadProfile.initialRps}
                  onChange={(e) => updateLoadProfile('initialRps', parseInt(e.target.value))}
                  min="1"
                />
              </div>

              {scenario.loadProfile.type === 'RAMP' && (
                <>
                  <div>
                    <label className="label">Target RPS *</label>
                    <input
                      type="number"
                      className="input-field"
                      value={scenario.loadProfile.targetRps}
                      onChange={(e) => updateLoadProfile('targetRps', parseInt(e.target.value))}
                      min="1"
                    />
                  </div>
                  <div>
                    <label className="label">Ramp Up Duration (seconds)</label>
                    <input
                      type="number"
                      className="input-field"
                      value={scenario.loadProfile.rampUpSeconds}
                      onChange={(e) => updateLoadProfile('rampUpSeconds', parseInt(e.target.value))}
                      min="0"
                    />
                  </div>
                </>
              )}
            </div>

            {/* Burst Configuration */}
            {(scenario.loadProfile.type === 'BURST' || scenario.loadProfile.type === 'SPIKE') && (
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="label">Traffic Bursts</label>
                  <button onClick={addBurst} className="text-sm text-blue-600 hover:text-blue-700">
                    + Add Burst
                  </button>
                </div>
                {(scenario.loadProfile.bursts || []).map((burst, index) => (
                  <div key={index} className="flex gap-2 mb-2 items-end">
                    <div className="flex-1">
                      <label className="text-xs text-gray-600">Start (s)</label>
                      <input
                        type="number"
                        className="input-field"
                        value={burst.startSecond}
                        onChange={(e) => updateBurst(index, 'startSecond', e.target.value)}
                      />
                    </div>
                    <div className="flex-1">
                      <label className="text-xs text-gray-600">Duration (s)</label>
                      <input
                        type="number"
                        className="input-field"
                        value={burst.durationSeconds}
                        onChange={(e) => updateBurst(index, 'durationSeconds', e.target.value)}
                      />
                    </div>
                    <div className="flex-1">
                      <label className="text-xs text-gray-600">RPS</label>
                      <input
                        type="number"
                        className="input-field"
                        value={burst.rps}
                        onChange={(e) => updateBurst(index, 'rps', e.target.value)}
                      />
                    </div>
                    <button onClick={() => removeBurst(index)} className="p-2 text-red-600 hover:bg-red-50 rounded">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* SLA Configuration */}
        {activeTab === 'sla' && (
          <div className="space-y-4">
            <div>
              <label className="label">Minimum Success Rate (%)</label>
              <input
                type="number"
                className="input-field"
                value={scenario.slaConfig.minSuccessRate}
                onChange={(e) => updateSlaConfig('minSuccessRate', parseFloat(e.target.value))}
                min="0"
                max="100"
                step="0.1"
              />
            </div>

            <div>
              <label className="label">Max Average Latency (ms)</label>
              <input
                type="number"
                className="input-field"
                value={scenario.slaConfig.maxAvgLatencyMs}
                onChange={(e) => updateSlaConfig('maxAvgLatencyMs', parseFloat(e.target.value))}
                min="0"
              />
            </div>

            <div>
              <label className="label">Max P95 Latency (ms)</label>
              <input
                type="number"
                className="input-field"
                value={scenario.slaConfig.maxP95LatencyMs}
                onChange={(e) => updateSlaConfig('maxP95LatencyMs', parseFloat(e.target.value))}
                min="0"
              />
            </div>

            <div>
              <label className="label">Max P99 Latency (ms)</label>
              <input
                type="number"
                className="input-field"
                value={scenario.slaConfig.maxP99LatencyMs}
                onChange={(e) => updateSlaConfig('maxP99LatencyMs', parseFloat(e.target.value))}
                min="0"
              />
            </div>

            <div>
              <label className="label">Max Error Rate (%)</label>
              <input
                type="number"
                className="input-field"
                value={scenario.slaConfig.maxErrorRate}
                onChange={(e) => updateSlaConfig('maxErrorRate', parseFloat(e.target.value))}
                min="0"
                max="100"
                step="0.1"
              />
            </div>
          </div>
        )}

        {/* Alerts Configuration */}
        {activeTab === 'alerts' && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Configure notifications for test completion and SLA violations
            </p>
            
            <div>
              <label className="label">Notification Channel</label>
              <select className="input-field">
                {ALERT_CHANNELS.map(channel => (
                  <option key={channel.value} value={channel.value}>{channel.label}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="label">Recipient (Email/URL)</label>
              <input
                type="text"
                className="input-field"
                placeholder="email@example.com or webhook URL"
              />
            </div>
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div className="flex gap-3 mt-8 pt-6 border-t">
        <button
          onClick={onCancel}
          className="btn-secondary flex-1"
        >
          Cancel
        </button>
        <button
          onClick={onSubmit}
          disabled={creating || !scenario.name || !scenario.targetUrl}
          className="btn-primary flex-1"
        >
          {creating ? 'Creating...' : 'Create Scenario'}
        </button>
      </div>
    </div>
  );
};

export default ScenarioForm;