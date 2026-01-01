// ========== src/hooks/useWebSocket.js ==========
import { useEffect, useCallback } from 'react';
import websocketService from '../services/websocket';

export const useWebSocket = () => {
  useEffect(() => {
    websocketService.connect(
      () => console.log('Connected to WebSocket'),
      (error) => console.error('WebSocket error:', error)
    );

    return () => {
      websocketService.disconnect();
    };
  }, []);

  const subscribeToScenarioMetrics = useCallback((scenarioId, callback) => {
    return websocketService.subscribeToScenarioMetrics(scenarioId, callback);
  }, []);

  const subscribeToWorkerStatus = useCallback((callback) => {
    return websocketService.subscribeToWorkerStatus(callback);
  }, []);

  const unsubscribe = useCallback((topic) => {
    websocketService.unsubscribe(topic);
  }, []);

  return {
    subscribeToScenarioMetrics,
    subscribeToWorkerStatus,
    unsubscribe,
  };
};
