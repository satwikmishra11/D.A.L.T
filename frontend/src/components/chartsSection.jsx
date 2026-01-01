// ========== src/components/ChartsSection.jsx ==========
import React from 'react';
import LatencyChart from './LatencyChart';
import ThroughputChart from './ThroughputChart';
import StatusCodeChart from './StatusCodeChart';

const ChartsSection = ({ latencyData, throughputData, stats }) => {
  return (
    <div className="space-y-6">
      {/* Latency Chart */}
      {latencyData && latencyData.length > 0 && (
        <LatencyChart data={latencyData} />
      )}

      {/* Throughput Chart */}
      {throughputData && throughputData.length > 0 && (
        <ThroughputChart data={throughputData} />
      )}

      {/* Status Code Distribution */}
      {stats?.statusCodeDistribution && (
        <StatusCodeChart statusCodeDistribution={stats.statusCodeDistribution} />
      )}
    </div>
  );
};

export default ChartsSection;