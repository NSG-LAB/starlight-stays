// API Service for Starlight Stays
// Central edge routing through Spring Cloud API Gateway on port 8080

const GATEWAY_BASE = 'http://localhost:8080';
const EUREKA_BASE = 'http://localhost:8761';
const TOKEN_KEY = 'starlight_jwt_token';
const USER_KEY = 'starlight_user';

let currentToken = localStorage.getItem(TOKEN_KEY) || '';
let currentUser = localStorage.getItem(USER_KEY) || '';

// Fallback & Event listeners
const circuitBreakerListeners = [];
const authStateListeners = [];

export function onCircuitBreakerFallback(callback) {
  circuitBreakerListeners.push(callback);
}

export function onAuthStateChange(callback) {
  authStateListeners.push(callback);
}

function notifyCircuitBreaker(details) {
  circuitBreakerListeners.forEach(fn => {
    try { fn(details); } catch (e) { console.error('Error in CB listener', e); }
  });
}

function notifyAuthChange(user) {
  authStateListeners.forEach(fn => {
    try { fn(user); } catch (e) { console.error('Error in Auth listener', e); }
  });
}

export function getToken() {
  return currentToken;
}

export function getCurrentUser() {
  return currentUser;
}

export function isAuthenticated() {
  return Boolean(currentToken);
}

export function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

export function setSession(token, username) {
  currentToken = token;
  currentUser = username;
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, username);
  notifyAuthChange(username);
}

export function logout() {
  currentToken = '';
  currentUser = '';
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  notifyAuthChange(null);
}

/**
 * Enhanced fetch wrapper:
 * 1. Appends JWT Bearer token to protected calls
 * 2. Catches 503 circuit breaker fallback responses
 * 3. Catches 401 unauthorized responses
 */
export async function apiRequest(path, options = {}) {
  const url = path.startsWith('http') ? path : `${GATEWAY_BASE}${path}`;
  const headers = new Headers(options.headers || {});

  // Append token if available and not already set
  if (currentToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${currentToken}`);
  }

  // Default content type if body is object and method not GET
  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
    options.body = JSON.stringify(options.body);
  }

  const fetchOptions = {
    ...options,
    headers,
  };

  try {
    const response = await fetch(url, fetchOptions);

    // Handle 503 Circuit Breaker Service Unavailable
    if (response.status === 503) {
      let bodyText = '';
      try {
        bodyText = await response.text();
      } catch (e) {}
      
      const details = {
        status: 503,
        path,
        message: bodyText || 'Service Unavailable (Circuit Breaker Activated)',
        timestamp: new Date().toISOString(),
      };
      notifyCircuitBreaker(details);
      
      const error = new Error(`503 Circuit Breaker Triggered: ${details.message}`);
      error.status = 503;
      error.details = details;
      throw error;
    }

    // Handle 401 Unauthorized
    if (response.status === 401) {
      logout();
      const error = new Error('Session expired or unauthorized. Please log in.');
      error.status = 401;
      throw error;
    }

    return response;
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('Failed to fetch')) {
      const details = {
        status: 503,
        path,
        message: 'Network error or downstream service unreachable.',
        timestamp: new Date().toISOString(),
      };
      notifyCircuitBreaker(details);
    }
    throw err;
  }
}

// 1. Authentication
export async function login(username, password) {
  const query = new URLSearchParams({ username, password }).toString();
  const response = await fetch(`${GATEWAY_BASE}/api/users/login?${query}`, {
    method: 'POST',
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(errText || 'Invalid credentials');
  }

  const token = await response.text();
  setSession(token, username);
  return { token, username };
}

// 2. Room Catalog
export async function fetchRooms() {
  const response = await apiRequest('/api/rooms');
  if (!response.ok) {
    throw new Error(`Failed to load rooms: ${response.statusText}`);
  }
  return response.json();
}

export async function fetchRoomById(id) {
  const response = await apiRequest(`/api/rooms/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch room #${id}`);
  }
  return response.json();
}

// 3. Bookings
export async function createBooking({ roomId, guestName, checkInDate, checkOutDate, guestCount }) {
  const response = await apiRequest('/api/bookings', {
    method: 'POST',
    body: {
      roomId: Number(roomId),
      guestName,
      checkInDate,
      checkOutDate,
      guestCount: Number(guestCount) || 1,
    },
  });

  if (response.status === 409) {
    const conflictMsg = await response.text();
    const error = new Error(conflictMsg || 'Room is unavailable for selected dates.');
    error.status = 409;
    throw error;
  }

  if (!response.ok) {
    const err = await response.text();
    throw new Error(err || 'Failed to create booking');
  }

  return response.json();
}

export async function getUserBookings(guestName = 'admin') {
  const response = await apiRequest(`/api/bookings/user/${encodeURIComponent(guestName)}`);
  if (!response.ok) {
    throw new Error('Failed to fetch reservations');
  }
  return response.json();
}

export async function cancelBooking(id) {
  const response = await apiRequest(`/api/bookings/${id}/cancel`, {
    method: 'PUT',
  });
  if (!response.ok) {
    throw new Error('Failed to cancel booking');
  }
  return response.text();
}

// 4. Asynchronous AMQP Booking Event simulation
export async function simulateBookingEvent(customPayload = null) {
  const response = await apiRequest('/api/bookings/test-event', {
    method: 'POST',
    body: customPayload,
  });
  if (!response.ok) {
    throw new Error('Failed to dispatch AMQP test event');
  }
  return response.text();
}

// 5. Resilience & Circuit Breaker testing
export async function testCircuitBreakerFallback() {
  const response = await apiRequest('/fallback/bookings', {
    method: 'POST',
  });
  return response.text();
}

export async function testInternalCircuitBreaker() {
  const response = await apiRequest('/api/bookings/test-cb', {
    method: 'GET',
  });
  return response.text();
}

// 6. Notifications & Vouchers (Phase 1)
export async function fetchNotifications(guestName = 'admin') {
  const response = await apiRequest(`/api/notifications/user/${encodeURIComponent(guestName)}`);
  if (!response.ok) {
    throw new Error('Failed to fetch notifications');
  }
  return response.json();
}

// 7. Guest Reviews & Ratings (Phase 3)
export async function fetchReviews(roomId) {
  const response = await apiRequest(`/api/reviews/room/${roomId}`);
  if (!response.ok) {
    return [];
  }
  return response.json();
}

export async function createReview({ roomId, guestName, rating, comment }) {
  const response = await apiRequest('/api/reviews', {
    method: 'POST',
    body: {
      roomId: Number(roomId),
      guestName,
      rating: Number(rating),
      comment
    }
  });
  if (!response.ok) {
    const err = await response.text();
    throw new Error(err || 'Failed to submit review');
  }
  return response.json();
}

// 8. Admin Analytics & All Bookings (Phase 4)
export async function fetchAllBookings() {
  const response = await apiRequest('/api/bookings');
  if (!response.ok) {
    throw new Error('Failed to fetch all bookings');
  }
  return response.json();
}

// 9. Intelligent AI Concierge (Phase 3)
export async function getConciergeRecommendation({ travelStyle, guestCount, budgetPreference, preferredAmenities = [] }) {
  const response = await apiRequest('/api/concierge/recommend', {
    method: 'POST',
    body: {
      travelStyle,
      guestCount: Number(guestCount) || 2,
      budgetPreference,
      preferredAmenities
    }
  });
  if (!response.ok) {
    const err = await response.text();
    throw new Error(err || 'Failed to generate AI concierge recommendation');
  }
  return response.json();
}

// 10. Discovery Server Health
export async function fetchEurekaServices() {
  try {
    const res = await fetch(`${EUREKA_BASE}/eureka/apps`, {
      headers: { Accept: 'application/json' },
    });
    if (!res.ok) return null;
    const data = await res.json();
    return data.applications?.application || [];
  } catch (err) {
    console.warn('Could not query Eureka directly:', err);
    return null;
  }
}
