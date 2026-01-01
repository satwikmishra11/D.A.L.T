// ========== src/services/websocket.js ==========
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
  }

  connect(onConnect, onError) {
    const socket = new SockJS('http://localhost:8080/ws');
    
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected');
        if (onConnect) onConnect();
      },
      onStompError: (frame) => {
        console.error('WebSocket error:', frame);
        if (onError) onError(frame);
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }

  subscribe(topic, callback) {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected');
      return null;
    }

    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Failed to parse message:', error);
      }
    });

    this.subscriptions.set(topic, subscription);
    return subscription;
  }

  unsubscribe(topic) {
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  subscribeToScenarioMetrics(scenarioId, callback) {
    return this.subscribe(`/topic/metrics/${scenarioId}`, callback);
  }

  subscribeToWorkerStatus(callback) {
    return this.subscribe('/topic/workers/status', callback);
  }

  subscribeToAlerts(callback) {
    return this.subscribe('/topic/alerts', callback);
  }
}

export default new WebSocketService();
