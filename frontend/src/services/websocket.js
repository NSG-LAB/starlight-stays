// WebSocket and STOMP client listener for Starlight Stays
// Connects to http://localhost:8080/ws-starlight and listens to /topic/payments

const WS_URL = 'http://localhost:8080/ws-starlight';
const TOPIC_PAYMENTS = '/topic/payments';

let stompClient = null;
let socketInstance = null;
let connectionStatus = 'DISCONNECTED'; // CONNECTED | CONNECTING | DISCONNECTED | ERROR
let reconnectTimeout = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 10;

const eventListeners = [];
const statusListeners = [];

export function onPaymentEvent(callback) {
  eventListeners.push(callback);
}

export function onConnectionStatusChange(callback) {
  statusListeners.push(callback);
}

function setStatus(status, details = null) {
  connectionStatus = status;
  statusListeners.forEach(fn => {
    try { fn(status, details); } catch (e) { console.error('Status listener error', e); }
  });
}

function dispatchEvent(eventData) {
  eventListeners.forEach(fn => {
    try { fn(eventData); } catch (e) { console.error('Event listener error', e); }
  });
}

export function getStatus() {
  return connectionStatus;
}

export function connectWebSocket() {
  if (stompClient && stompClient.connected) {
    return;
  }

  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }

  setStatus('CONNECTING');

  try {
    // Rely on window.SockJS and window.Stomp from CDN or imported script
    const SockJSConstructor = window.SockJS;
    const StompObject = window.Stomp;

    if (!SockJSConstructor || !StompObject) {
      console.warn('SockJS or Stomp not yet loaded on window, waiting...');
      setTimeout(connectWebSocket, 500);
      return;
    }

    socketInstance = new SockJSConstructor(WS_URL);
    stompClient = StompObject.over(socketInstance);
    
    // Disable noisy frame logs in production, keep clean
    stompClient.debug = () => {};

    stompClient.connect(
      {},
      frame => {
        reconnectAttempts = 0;
        setStatus('CONNECTED', { frame });
        console.log('⚡ [STOMP] Connected to API Gateway at', WS_URL);

        // Subscribe to payments topic
        stompClient.subscribe(TOPIC_PAYMENTS, message => {
          try {
            const parsed = JSON.parse(message.body);
            console.log('💳 [STOMP] Incoming Payment Broadcast:', parsed);
            dispatchEvent({
              ...parsed,
              rawBody: message.body,
              receivedAt: new Date().toLocaleTimeString(),
            });
          } catch (e) {
            console.warn('Received non-JSON message:', message.body);
            dispatchEvent({
              status: 'INFO',
              bookingReference: message.body,
              timestamp: new Date().toISOString(),
              receivedAt: new Date().toLocaleTimeString(),
            });
          }
        });
      },
      error => {
        console.error('STOMP Connection error:', error);
        setStatus('ERROR', { error });
        scheduleReconnect();
      }
    );

    socketInstance.onclose = () => {
      if (connectionStatus === 'CONNECTED') {
        setStatus('DISCONNECTED');
        scheduleReconnect();
      }
    };
  } catch (err) {
    console.error('Failed to initialize WebSocket client:', err);
    setStatus('ERROR', { error: err.message });
    scheduleReconnect();
  }
}

function scheduleReconnect() {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    setStatus('DISCONNECTED', { reason: 'Max reconnect attempts reached' });
    return;
  }
  reconnectAttempts++;
  const delay = Math.min(1000 * Math.pow(1.5, reconnectAttempts), 10000);
  console.log(`Scheduling reconnect in ${Math.round(delay / 1000)}s (attempt ${reconnectAttempts})...`);
  reconnectTimeout = setTimeout(() => {
    connectWebSocket();
  }, delay);
}

export function disconnectWebSocket() {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
  if (stompClient) {
    try {
      stompClient.disconnect(() => {
        console.log('Disconnected from STOMP broker');
      });
    } catch (e) {}
    stompClient = null;
  }
  setStatus('DISCONNECTED');
}
