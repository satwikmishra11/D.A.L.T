/ ========== src/components/ExportDialog.jsx ==========
import React, { useState } from 'react';
import { X, Download, FileJson, FileText, File } from 'lucide-react';

const ExportDialog = ({ onExport, onClose }) => {
  const [selectedFormat, setSelectedFormat] = useState('json');
  const [exporting, setExporting] = useState(false);

  const formats = [
    { value: 'json', label: 'JSON', icon: FileJson, description: 'Raw data with all metrics' },
    { value: 'csv', label: 'CSV', icon: FileText, description: 'Spreadsheet format for Excel' },
    { value: 'html', label: 'HTML', icon: File, description: 'Visual report for sharing' },
  ];

  const handleExport = async () => {
    setExporting(true);
    try {
      await onExport(selectedFormat);
      onClose();
    } catch (error) {
      console.error('Export failed:', error);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full">
        <div className="flex items-center justify-between p-6 border-b">
          <h3 className="text-xl font-bold">Export Test Results</h3>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <p className="text-sm text-gray-600">
            Choose a format to export your load test results and metrics
          </p>

          <div className="space-y-3">
            {formats.map((format) => (
              <label
                key={format.value}
                className={`flex items-start gap-3 p-4 border-2 rounded-lg cursor-pointer transition ${
                  selectedFormat === format.value
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <input
                  type="radio"
                  name="format"
                  value={format.value}
                  checked={selectedFormat === format.value}
                  onChange={(e) => setSelectedFormat(e.target.value)}
                  className="mt-1"
                />
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <format.icon className="w-5 h-5 text-gray-600" />
                    <span className="font-semibold">{format.label}</span>
                  </div>
                  <p className="text-sm text-gray-600">{format.description}</p>
                </div>
              </label>
            ))}
          </div>
        </div>

        <div className="flex gap-3 p-6 border-t bg-gray-50 rounded-b-xl">
          <button onClick={onClose} className="btn-secondary flex-1">
            Cancel
          </button>
          <button
            onClick={handleExport}
            disabled={exporting}
            className="btn-primary flex-1 flex items-center justify-center gap-2"
          >
            <Download className="w-4 h-4" />
            {exporting ? 'Exporting...' : 'Export'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExportDialog;