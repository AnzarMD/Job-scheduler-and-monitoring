import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../store/authStore';

export function useWebSocket(onMessage) {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);
  const reconnectTimerRef = useRef(null);
  const user = useAuthStore((state) => state.user);
  const token = useAuthStore((state) => state.token);

  const connect = useCallback(() => {
    // Don't connect if no user/token (not logged in)
    if (!user?.tenantId || !token) return;

    // Create a new STOMP client
    // STOMP is the protocol layer on top of WebSocket that gives us pub/sub
    const client = new Client({
      // SockJS is the transport layer — provides fallback if WebSocket is blocked
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),

      // How long to wait before trying to reconnect after a disconnect
      reconnectDelay: 5000,

      onConnect: () => {
        setConnected(true);
        console.log('[WebSocket] Connected');

        // Subscribe to THIS tenant's topic only
        // Each tenant gets their own topic so they only see their own job updates
        client.subscribe(
          `/topic/executions/${user.tenantId}`,
          (message) => {
            try {
              const data = JSON.parse(message.body);
              // Call the callback provided by the component using this hook
              if (onMessage) onMessage(data);
            } catch (e) {
              console.error('[WebSocket] Failed to parse message', e);
            }
          }
        );
      },

      onDisconnect: () => {
        setConnected(false);
        console.log('[WebSocket] Disconnected');
      },

      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error', frame);
        setConnected(false);
      },
    });

    client.activate(); // Start the connection
    clientRef.current = client;
  }, [user?.tenantId, token, onMessage]);

  useEffect(() => {
    connect();

    // Cleanup: deactivate the WebSocket when the component unmounts
    // This prevents memory leaks and dangling connections
    return () => {
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [connect]);

  return { connected };
}