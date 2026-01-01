// ========== src/components/WorkerStatus.jsx ==========
import React from 'react';
import { Activity, CheckCircle, XCircle, Clock } from 'lucide-react';
import { formatRelativeTime } from '../utils/formatters';

const WorkerStatus = ({ workers }) => {
  const getStatusColor = (status) => {
    const colors = {
      IDLE: 'text-gray-600 bg-gray-100',
      BUSY: 'text-green-600 bg-green-100',
      ERROR: 'text-red-600 bg-red-100',
      OFFLINE: 'text-gray-400 bg-gray-50',
    };
    return colors[status] || colors.OFFLINE;
  };

  const getStatusIcon = (status) => {
    if (status === 'BUSY') return <Activity className="w-4 h-4" />;
    if (status === 'ERROR') return <XCircle className="w-4 h-4" />;
    if (status === 'IDLE') return <CheckCircle className="w-4 h-4" />;
    return <Clock className="w-4 h-4" />;
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      <h3 className="text-lg font-bold mb-4">Worker Status</h3>
      
      {workers.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <Activity className="w-12 h-12 mx-auto mb-2 opacity-50" />
          <p>No workers available</p>
        </div>
      ) : (
        <div className="space-y-3">
          {workers.map((worker) => (
            <div
              key={worker.workerId}
              className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50"
            >
              <div className="flex items-center gap-3">
                <div className={`p-2 rounded-full ${getStatusColor(worker.status)}`}>
                  {getStatusIcon(worker.status)}
                </div>
                <div>
                  <div className="font-medium text-sm">
                    {worker.workerId.substring(0, 8)}...
                  </div>
                  <div className="text-xs text-gray-500">
                    {worker.requestsProcessed?.toLocaleString() || 0} requests
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className={`text-xs px-2 py-1 rounded ${getStatusColor(worker.status)}`}>
                  {worker.status}
                </div>
                {worker.lastHeartbeat && (
                  <div className="text-xs text-gray-500 mt-1">
                    {formatRelativeTime(worker.lastHeartbeat)}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default WorkerStatus;