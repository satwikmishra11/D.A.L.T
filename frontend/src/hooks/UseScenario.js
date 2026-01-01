// ========== src/hooks/useScenarios.js ==========
import { useState, useEffect, useCallback } from 'react';
import { scenarioAPI } from '../services/api';

export const useScenarios = () => {
  const [scenarios, setScenarios] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadScenarios = useCallback(async () => {
    setLoading(true);
    try {
      const response = await scenarioAPI.getAll();
      setScenarios(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
      console.error('Failed to load scenarios:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadScenarios();
  }, [loadScenarios]);

  const createScenario = async (data) => {
    try {
      const response = await scenarioAPI.create(data);
      setScenarios([...scenarios, response.data]);
      return response.data;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const startScenario = async (id) => {
    try {
      await scenarioAPI.start(id);
      await loadScenarios();
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const stopScenario = async (id) => {
    try {
      await scenarioAPI.stop(id);
      await loadScenarios();
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  return {
    scenarios,
    loading,
    error,
    loadScenarios,
    createScenario,
    startScenario,
    stopScenario,
  };
};