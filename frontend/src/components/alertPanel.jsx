import React from 'react';
import { AlertTriangle, CheckCircle, X } from 'lucide-react';
import { formatRelativeTime } from '../utils/formatters';

const AlertPanel = ({ alerts, onAcknowledge }) => {
  if (alerts.length === 0) return null;

  const getSeverityColor = (severity) => {
    const colors = {
      CRITICAL: 'bg-red-100 border-red-500 text-red-900',
      ERROR: 'bg-red-50 border-red-400 text-red-800',
      WARNING: 'bg-yellow-50 border-yellow-400 text-yellow-800',
      INFO: 'bg-blue-50 border-blue-400 text-blue-800',
    };
    return colors[severity] || colors.INFO;
  };

  const getSeverityIcon = (severity) => {
    if (severity === 'CRITICAL' || severity === 'ERROR' || severity === 'WARNING') {
      return <AlertTriangle className="w-5 h-5" />;
    }
    return <CheckCircle className="w-5 h-5" />;
  };

  return (
    <div className="space-y-3">
      {alerts.map((alert) => (
        <div
          key={alert.id}
          className={`border-l-4 rounded-lg p-4 ${getSeverityColor(alert.severity)}`}
        >
          <div className="flex items-start justify-between">
            <div className="flex items-start gap-3 flex-1">
              {getSeverityIcon(alert.severity)}
              <div className="flex-1">
                <h4 className="font-semibold">{alert.title}</h4>
                <p className="text-sm mt-1">{alert.message}</p>
                <p className="text-xs mt-2 opacity-75">
                  {formatRelativeTime(alert.createdAt)}
                </p>
              </div>
            </div>
            <button
              onClick={() => onAcknowledge(alert.id)}
              className="p-1 hover:bg-black hover:bg-opacity-10 rounded"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      ))}
    </div>
  );
};

export default AlertPanel;
