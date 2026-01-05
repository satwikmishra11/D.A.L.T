// ========== src/services/api.js ==========
import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
  getCurrentUser: () => api.get('/auth/me'),
};

export const scenarioAPI = {
  create: (data) => api.post('/scenarios', data),
  getAll: () => api.get('/scenarios'),
  getById: (id) => api.get(`/scenarios/${id}`),
  start: (id) => api.post(`/scenarios/${id}/start`),
  stop: (id) => api.post(`/scenarios/${id}/stop`),
  getStats: (id, lastNSeconds) => 
    api.get(`/scenarios/${id}/stats/realtime?lastNSeconds=${lastNSeconds}`),
  getBottlenecks: (id) => api.get(`/scenarios/${id}/bottlenecks`),
};

export const workerAPI = {
  getStatus: () => api.get('/workers/status'),
  getHeartbeat: (workerId) => api.get(`/workers/${workerId}/heartbeat`),
};

export const alertAPI = {
  getUnacknowledged: () => api.get('/alerts/unacknowledged'),
  getForScenario: (scenarioId) => api.get(`/alerts/scenario/${scenarioId}`),
  acknowledge: (alertId) => api.post(`/alerts/${alertId}/acknowledge`),
};

export const rollbackScenario = (scenarioId, targetVersion) =>
  api.post(`/scenarios/${scenarioId}/versions/rollback`, {
    targetVersion,
  });

export const exportAPI = {
  toJson: (scenarioId) => 
    api.get(`/export/${scenarioId}/json`, { responseType: 'blob' }),
  toCsv: (scenarioId) => 
    api.get(`/export/${scenarioId}/csv`, { responseType: 'blob' }),
  toHtml: (scenarioId) => 
    api.get(`/export/${scenarioId}/html`, { responseType: 'blob' }),
  getReport: (scenarioId) => api.get(`/export/${scenarioId}/report`),
};

export const dashboardAPI = {
  getSummary: () => api.get('/dashboard/summary'),
  getSystemHealth: () => api.get('/dashboard/system-health'),
};

export default api;