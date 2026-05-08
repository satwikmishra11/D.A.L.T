// ========== src/utils/constants.js ==========
export const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];

export const LOAD_PROFILE_TYPES = [
  { value: 'CONSTANT', label: 'Constant Load' },
  { value: 'RAMP', label: 'Ramp Up' },
  { value: 'BURST', label: 'Burst Traffic' },
  { value: 'SPIKE', label: 'Spike Test' },
];

export const ENVIRONMENTS = [
  { value: 'dev', label: 'Development' },
  { value: 'staging', label: 'Staging' },
  { value: 'prod', label: 'Production' },
];

export const ALERT_CHANNELS = [
  { value: 'EMAIL', label: 'Email' },
  { value: 'SLACK', label: 'Slack' },
  { value: 'WEBHOOK', label: 'Webhook' },
  { value: 'SMS', label: 'SMS' },
  { value: 'PAGERDUTY', label: 'PagerDuty' },
];

export const ALERT_TYPES = [
  { value: 'SLA_VIOLATION', label: 'SLA Violation' },
  { value: 'HIGH_ERROR_RATE', label: 'High Error Rate' },
  { value: 'HIGH_LATENCY', label: 'High Latency' },
  { value: 'WORKER_FAILURE', label: 'Worker Failure' },
  { value: 'TEST_COMPLETED', label: 'Test Completed' },
  { value: 'TEST_FAILED', label: 'Test Failed' },
];

export const STATUS_COLORS = {
  DRAFT: 'bg-gray-500',
  SCHEDULED: 'bg-purple-500',
  QUEUED: 'bg-yellow-500',
  RUNNING: 'bg-green-500',
  PAUSED: 'bg-orange-500',
  COMPLETED: 'bg-blue-500',
  FAILED: 'bg-red-500',
  CANCELLED: 'bg-gray-400',
  TIMEOUT: 'bg-red-400',
};

export const CHART_COLORS = {
  primary: '#3b82f6',
  success: '#10b981',
  warning: '#f59e0b',
  danger: '#ef4444',
  purple: '#8b5cf6',
  teal: '#14b8a6',
  pink: '#ec4899',
};

export const DEFAULT_SLA_CONFIG = {
  minSuccessRate: 99.0,
  maxAvgLatencyMs: 500,
  maxP95LatencyMs: 1000,
  maxP99LatencyMs: 2000,
  maxErrorRate: 1.0,
};
