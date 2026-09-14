import './index.css';
import {
  login,
  logout,
  isAuthenticated,
  getCurrentUser,
  fetchRooms,
  createBooking,
  getUserBookings,
  cancelBooking,
  simulateBookingEvent,
  testCircuitBreakerFallback,
  fetchEurekaServices,
  onCircuitBreakerFallback,
  onAuthStateChange,
} from './services/api.js';

import {
  connectWebSocket,
  disconnectWebSocket,
  onPaymentEvent,
  onConnectionStatusChange,
} from './services/websocket.js';

// Assets
import penthouseImg from './assets/penthouse.jpg';
import nebulaImg from './assets/nebula.jpg';
import auroraImg from './assets/aurora.jpg';
import waterfrontImg from './assets/waterfront.jpg';

// State
let allRooms = [];
let liveEventsCount = 0;
let currentBookingStep = 1;
let selectedRoom = null;

// Curated image and amenity metadata matching the luxury rooms
const propertyMeta = {
  1: {
    image: penthouseImg,
    amenities: ['Sky Observatory', 'Heated Plunge Pool', 'Personal Butler', 'High-Speed Starlink'],
    badge: 'Signature Penthouse',
  },
  2: {
    image: nebulaImg,
    amenities: ['Glass Infinity Pool', 'Bioluminescent Garden', 'Sauna & Spa', 'Private Heli-Pad'],
    badge: 'Architectural Villa',
  },
  3: {
    image: auroraImg,
    amenities: ['Northern Lights Roof', 'Cedar Hot Tub', 'Nordic Sauna', 'Fireplace'],
    badge: 'Alpine Sky Chalet',
  },
  4: {
    image: penthouseImg,
    amenities: ['Skyline Balcony', 'Smart Automation', 'Espresso Bar', 'Soundproof Studio'],
    badge: 'Executive Modern',
  },
  5: {
    image: waterfrontImg,
    amenities: ['Overwater Deck', 'Glass Floor Lagoon', 'Hammock Net', 'Private Yacht Dock'],
    badge: 'Lagoon Retreat',
  },
  6: {
    image: nebulaImg,
    amenities: ['3 Master Suites', 'Private Chef', 'Starlight Terrace', 'Cinema Room'],
    badge: 'Presidential Estate',
  },
};

// ---------------- DOM Elements ----------------
const roomsContainer = document.getElementById('rooms-container');
const searchQuery = document.getElementById('search-query');
const filterType = document.getElementById('filter-type');
const filterPrice = document.getElementById('filter-price');
const filterAvailability = document.getElementById('filter-availability');
const btnRefreshRooms = document.getElementById('btn-refresh-rooms');

// Auth elements
const btnOpenLogin = document.getElementById('btn-open-login');
const userLoggedInWrap = document.getElementById('user-logged-in-wrap');
const navUsername = document.getElementById('nav-username');
const navAvatar = document.getElementById('nav-avatar');
const btnLogout = document.getElementById('btn-logout');
const loginModal = document.getElementById('login-modal');
const loginForm = document.getElementById('login-form');
const btnCloseLoginModal = document.getElementById('btn-close-login-modal');
const loginErrorAlert = document.getElementById('login-error-alert');

// Booking modal elements
const btnOpenBookingModal = document.getElementById('btn-open-booking-modal');
const bookingModal = document.getElementById('booking-modal');
const bookingForm = document.getElementById('booking-form');
const btnCloseBookingModal = document.getElementById('btn-close-booking-modal');
const btnCancelBooking = document.getElementById('btn-cancel-booking');
const modalRoomName = document.getElementById('modal-room-name');
const bookingRoomId = document.getElementById('booking-room-id');
const bookingRoomSelect = document.getElementById('booking-room-select');
const bookingPropertyBadge = document.getElementById('booking-property-badge');
const bookingRateDisplay = document.getElementById('booking-rate-display');
const bookingGuestCount = document.getElementById('booking-guest-count');
const bookingVipAddon = document.getElementById('booking-vip-addon');
const bookingCheckin = document.getElementById('booking-checkin');
const bookingCheckout = document.getElementById('booking-checkout');
const bookingGuestName = document.getElementById('booking-guest-name');
const bookingGuestEmail = document.getElementById('booking-guest-email');
const bookingGuestPhone = document.getElementById('booking-guest-phone');
const btnSubmitBooking = document.getElementById('btn-submit-booking');
const bookingConflictAlert = document.getElementById('booking-conflict-alert');
const cbSimulateDecline = document.getElementById('cb-simulate-decline');

// My Bookings Drawer Elements
const btnOpenMyBookings = document.getElementById('btn-open-my-bookings');
const myBookingsModal = document.getElementById('my-bookings-modal');
const btnCloseMyBookingsModal = document.getElementById('btn-close-my-bookings-modal');
const btnCloseMyBookingsFooter = document.getElementById('btn-close-my-bookings-footer');
const btnRefreshMyBookings = document.getElementById('btn-refresh-my-bookings');
const myBookingsList = document.getElementById('my-bookings-list');
const myBookingsLoading = document.getElementById('my-bookings-loading');
const myBookingsEmpty = document.getElementById('my-bookings-empty');

// Step Containers & Indicators
const stepBubble1 = document.getElementById('step-bubble-1');
const stepBubble2 = document.getElementById('step-bubble-2');
const stepBubble3 = document.getElementById('step-bubble-3');
const bookingStep1 = document.getElementById('booking-step-1');
const bookingStep2 = document.getElementById('booking-step-2');
const bookingStep3 = document.getElementById('booking-step-3');
const btnModalPrev = document.getElementById('btn-modal-prev');
const btnModalNext = document.getElementById('btn-modal-next');

// Pricing Breakdown elements
const priceRateCalc = document.getElementById('price-rate-calc');
const priceSubtotal = document.getElementById('price-subtotal');
const priceAddon = document.getElementById('price-addon');
const priceTax = document.getElementById('price-tax');
const priceGrandTotal = document.getElementById('price-grand-total');

// Summary elements (Step 3)
const summaryPropertyTitle = document.getElementById('summary-property-title');
const summaryBadge = document.getElementById('summary-badge');
const summaryDatesDisplay = document.getElementById('summary-dates-display');
const summaryGuestsDisplay = document.getElementById('summary-guests-display');
const summaryNameDisplay = document.getElementById('summary-name-display');
const summaryTotalDisplay = document.getElementById('summary-total-display');

// Resilience & Circuit Breaker elements
const cbBanner = document.getElementById('circuit-breaker-banner');
const cbBannerMessage = document.getElementById('cb-banner-message');
const btnRetryCb = document.getElementById('btn-retry-cb');
const btnDismissCb = document.getElementById('btn-dismiss-cb');
const btnTestCb = document.getElementById('btn-test-cb');
const btnTriggerCbTest = document.getElementById('btn-trigger-cbTest') || document.getElementById('btn-trigger-cb-test');
const btnTriggerAmqpTest = document.getElementById('btn-trigger-amqp-test');
const liveEventFeed = document.getElementById('live-event-feed');
const btnClearEvents = document.getElementById('btn-clear-events');
const btnRefreshHealth = document.getElementById('btn-refresh-health');

// Status Pills & Stats
const wsStatusDot = document.getElementById('ws-status-dot');
const wsStatusText = document.getElementById('ws-status-text');
const statTotalRooms = document.getElementById('stat-total-rooms');
const statActiveRate = document.getElementById('stat-active-rate');
const statEventsCount = document.getElementById('stat-events-count');

// Toast shelf
const toastShelf = document.getElementById('toast-shelf');

// ---------------- Toast Notifications ----------------
export function showToast(title, message, type = 'success', duration = 5000) {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const iconMap = {
    success: '✅',
    warning: '⚠️',
    danger: '⚡',
    info: '💳',
  };

  toast.innerHTML = `
    <div class="toast-icon">${iconMap[type] || '✨'}</div>
    <div class="toast-body">
      <div class="toast-title">${title}</div>
      <div class="toast-msg">${message}</div>
    </div>
  `;

  toastShelf.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('toast-out');
    setTimeout(() => toast.remove(), 450);
  }, duration);
}

// ---------------- Authentication Handling ----------------
function updateAuthUI(user) {
  if (user) {
    userLoggedInWrap.classList.remove('hidden');
    btnOpenLogin.classList.add('hidden');
    navUsername.textContent = user;
    navAvatar.textContent = user.charAt(0).toUpperCase();
  } else {
    userLoggedInWrap.classList.add('hidden');
    btnOpenLogin.classList.remove('hidden');
  }
}

onAuthStateChange(user => {
  updateAuthUI(user);
});

// Auto-login default admin for smooth pairing
async function ensureAuthenticated() {
  if (!isAuthenticated()) {
    try {
      console.log('Authenticating with default credentials...');
      await login('admin', 'password');
      showToast('Authenticated', 'Obtained JWT Bearer Token from User Service', 'success', 3000);
    } catch (e) {
      console.warn('Auto-login failed, prompting modal', e);
      loginModal.classList.remove('hidden');
    }
  } else {
    updateAuthUI(getCurrentUser());
  }
}

// ---------------- Room Catalog & Rendering ----------------
async function loadRooms() {
  try {
    roomsContainer.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">🪐</div>
        <h3>Loading Luxury Celestial Stays...</h3>
        <p style="color: var(--text-muted); margin-top: 6px;">GET /api/rooms via API Gateway (Port 8080)</p>
      </div>
    `;

    const rooms = await fetchRooms();
    allRooms = rooms;
    renderRooms(allRooms);
    updateStats(allRooms);
    populateRoomSelectDropdown(allRooms);
  } catch (err) {
    console.error('Error fetching rooms:', err);
    roomsContainer.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">⚠️</div>
        <h3>Unable to load rooms from Room Service</h3>
        <p style="color: #fda4af; margin-top: 6px;">${err.message}</p>
        <button id="btn-retry-load-rooms" class="btn btn-primary" style="margin-top: 16px;">Retry Connection</button>
      </div>
    `;
    const retryBtn = document.getElementById('btn-retry-load-rooms');
    if (retryBtn) retryBtn.addEventListener('click', loadRooms);
  }
}

function renderRooms(rooms) {
  const query = searchQuery ? searchQuery.value.trim().toLowerCase() : '';
  const typeFilter = filterType ? filterType.value : 'ALL';
  const priceFilter = filterPrice ? filterPrice.value : 'ALL';
  const availFilter = filterAvailability ? filterAvailability.value : 'ALL';

  const filtered = rooms.filter(room => {
    // 1. Text Query
    const matchesQuery = !query || 
      room.propertyName.toLowerCase().includes(query) ||
      room.roomType.toLowerCase().includes(query);

    // 2. Category Filter
    const matchesType = typeFilter === 'ALL' ||
      room.roomType.toLowerCase().includes(typeFilter.toLowerCase());

    // 3. Price Point Filter
    const rate = Number(room.nightlyRate) || 0;
    let matchesPrice = true;
    if (priceFilter === 'UNDER_400') {
      matchesPrice = rate < 400;
    } else if (priceFilter === '400_700') {
      matchesPrice = rate >= 400 && rate <= 700;
    } else if (priceFilter === 'ABOVE_700') {
      matchesPrice = rate > 700;
    }

    // 4. Availability Filter
    const isAvail = room.available !== false;
    const matchesAvail = availFilter === 'ALL' ||
      (availFilter === 'AVAILABLE' ? isAvail : !isAvail);

    return matchesQuery && matchesType && matchesPrice && matchesAvail;
  });

  if (filtered.length === 0) {
    roomsContainer.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">🔍</div>
        <h3>No Stays Match Your Search</h3>
        <p style="color: var(--text-muted); margin-top: 6px;">Try adjusting your keywords, price points, or category filters</p>
      </div>
    `;
    return;
  }

  roomsContainer.innerHTML = filtered.map(room => {
    const meta = propertyMeta[room.id] || {
      image: penthouseImg,
      amenities: ['Starlight Views', 'Ultra-Fast Wi-Fi', 'Luxury Linens', 'Concierge'],
      badge: 'Luxury Suite',
    };

    const isAvail = room.available !== false;

    return `
      <article class="room-card" data-room-id="${room.id}">
        <div class="card-img-wrap">
          <img src="${meta.image}" alt="${room.propertyName}" class="card-img" loading="lazy" />
          <div class="card-badges">
            <span class="badge-tag ${isAvail ? 'available' : 'booked'}">
              ${isAvail ? '● Available' : '○ Reserved'}
            </span>
            <span class="badge-id">#${room.id}</span>
          </div>
        </div>

        <div class="card-body">
          <div class="card-type">${meta.badge} • ${room.roomType}</div>
          <h2 class="card-title">${room.propertyName}</h2>

          <div class="card-amenities">
            ${meta.amenities.map(a => `<span class="amenity-chip">${a}</span>`).join('')}
          </div>

          <div class="card-footer">
            <div class="rate-wrap">
              <span class="rate-amount">$${Number(room.nightlyRate).toFixed(2)}</span>
              <span class="rate-unit">per night • taxes incl.</span>
            </div>

            <button class="btn btn-primary btn-book-room" data-room-id="${room.id}">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
              Reserve Now
            </button>
          </div>
        </div>
      </article>
    `;
  }).join('');

  // Attach booking buttons
  document.querySelectorAll('.btn-book-room').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const roomId = Number(btn.getAttribute('data-room-id'));
      openBookingModal(roomId);
    });
  });
}

function populateRoomSelectDropdown(rooms) {
  if (!bookingRoomSelect) return;
  bookingRoomSelect.innerHTML = rooms.map(r => {
    const meta = propertyMeta[r.id] || { badge: r.roomType };
    return `<option value="${r.id}" data-rate="${r.nightlyRate}" data-name="${r.propertyName}" data-badge="${meta.badge}">
      ${r.propertyName} — $${Number(r.nightlyRate).toFixed(0)}/nt (${meta.badge})
    </option>`;
  }).join('');

  bookingRoomSelect.addEventListener('change', () => {
    const selectedOpt = bookingRoomSelect.options[bookingRoomSelect.selectedIndex];
    if (selectedOpt) {
      const roomId = Number(selectedOpt.value);
      selectedRoom = allRooms.find(r => r.id === roomId) || null;
      updateSelectedRoomUI();
    }
  });
}

function updateStats(rooms) {
  if (statTotalRooms) statTotalRooms.textContent = rooms.length;
  const availableCount = rooms.filter(r => r.available !== false).length;
  const rate = rooms.length ? Math.round((availableCount / rooms.length) * 100) : 100;
  if (statActiveRate) statActiveRate.textContent = `${rate}%`;
}

// ---------------- Multi-Step Reservation Workflow ----------------
function openBookingModal(targetRoomId = null) {
  if (!isAuthenticated()) {
    showToast('Sign In Required', 'Please authenticate to dispatch bookings', 'warning');
    loginModal.classList.remove('hidden');
    return;
  }

  // Find target room or default to first
  if (targetRoomId) {
    selectedRoom = allRooms.find(r => r.id === targetRoomId) || allRooms[0];
  } else if (!selectedRoom && allRooms.length > 0) {
    selectedRoom = allRooms[0];
  }

  if (selectedRoom && bookingRoomSelect) {
    bookingRoomSelect.value = selectedRoom.id;
  }
  updateSelectedRoomUI();

  // Initialize dates: tomorrow -> +3 days
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const future = new Date();
  future.setDate(future.getDate() + 4);

  bookingCheckin.value = tomorrow.toISOString().split('T')[0];
  bookingCheckout.value = future.toISOString().split('T')[0];
  bookingConflictAlert.classList.add('hidden');

  goToBookingStep(1);
  bookingModal.classList.remove('hidden');
}

function updateSelectedRoomUI() {
  if (!selectedRoom) return;
  const meta = propertyMeta[selectedRoom.id] || { badge: selectedRoom.roomType };
  bookingRoomId.value = selectedRoom.id;
  modalRoomName.textContent = `Reserve ${selectedRoom.propertyName}`;
  bookingPropertyBadge.textContent = `${meta.badge} • ${selectedRoom.roomType}`;
  bookingRateDisplay.innerHTML = `$${Number(selectedRoom.nightlyRate).toFixed(2)} <span style="font-size: 0.8rem; color: var(--text-muted); font-weight: 400;">/ night</span>`;
}

function calculatePricing() {
  if (!selectedRoom) return { nights: 0, subtotal: 0, addon: 0, tax: 0, grandTotal: 0 };
  
  const checkIn = new Date(bookingCheckin.value);
  const checkOut = new Date(bookingCheckout.value);
  const diffTime = Math.max(0, checkOut - checkIn);
  const nights = Math.max(1, Math.round(diffTime / (1000 * 60 * 60 * 24)));

  const nightlyRate = Number(selectedRoom.nightlyRate) || 0;
  const subtotal = nightlyRate * nights;

  let addonCost = 0;
  if (bookingVipAddon) {
    const addon = bookingVipAddon.value;
    if (addon === 'HELI') addonCost = 350;
    else if (addon === 'ASTRONOMER') addonCost = 200;
    else if (addon === 'CHEF') addonCost = 450;
  }

  const tax = Math.round((subtotal + addonCost) * 0.12);
  const grandTotal = subtotal + addonCost + tax;

  return { nights, nightlyRate, subtotal, addonCost, tax, grandTotal };
}

function updatePricingUI() {
  const calc = calculatePricing();
  priceRateCalc.textContent = `$${calc.nightlyRate.toFixed(2)} x ${calc.nights} nights`;
  priceSubtotal.textContent = `$${calc.subtotal.toFixed(2)}`;
  priceAddon.textContent = `$${calc.addonCost.toFixed(2)}`;
  priceTax.textContent = `$${calc.tax.toFixed(2)}`;
  priceGrandTotal.textContent = `$${calc.grandTotal.toFixed(2)}`;

  // Update Step 3 Summary
  if (selectedRoom) {
    const meta = propertyMeta[selectedRoom.id] || { badge: selectedRoom.roomType };
    summaryPropertyTitle.textContent = `${selectedRoom.propertyName} (#${selectedRoom.id})`;
    summaryBadge.textContent = `${meta.badge}`;
  }
  summaryDatesDisplay.textContent = `${bookingCheckin.value} to ${bookingCheckout.value} (${calc.nights} nights)`;
  summaryGuestsDisplay.textContent = `${bookingGuestCount.value} Guests`;
  summaryNameDisplay.textContent = bookingGuestName.value.trim() || 'Alice Vance';
  summaryTotalDisplay.textContent = `$${calc.grandTotal.toFixed(2)}`;
}

function goToBookingStep(step) {
  currentBookingStep = step;

  // Step Bubbles
  stepBubble1.className = 'step-bubble' + (step === 1 ? ' active' : (step > 1 ? ' completed' : ''));
  stepBubble2.className = 'step-bubble' + (step === 2 ? ' active' : (step > 2 ? ' completed' : ''));
  stepBubble3.className = 'step-bubble' + (step === 3 ? ' active' : '');

  // Step Containers
  bookingStep1.classList.toggle('hidden', step !== 1);
  bookingStep2.classList.toggle('hidden', step !== 2);
  bookingStep3.classList.toggle('hidden', step !== 3);

  // Footer Buttons
  if (step === 1) {
    btnModalPrev.classList.add('hidden');
    btnModalNext.classList.remove('hidden');
    btnModalNext.textContent = 'Continue to Guest Details →';
    btnSubmitBooking.classList.add('hidden');
  } else if (step === 2) {
    btnModalPrev.classList.remove('hidden');
    btnModalNext.classList.remove('hidden');
    btnModalNext.textContent = 'Proceed to Saga Confirmation →';
    btnSubmitBooking.classList.add('hidden');
    updatePricingUI();
  } else if (step === 3) {
    btnModalPrev.classList.remove('hidden');
    btnModalNext.classList.add('hidden');
    btnSubmitBooking.classList.remove('hidden');
    updatePricingUI();
  }
}

// Navigation Listeners
btnModalNext.addEventListener('click', () => {
  if (currentBookingStep === 1) {
    const checkin = new Date(bookingCheckin.value);
    const checkout = new Date(bookingCheckout.value);
    if (!bookingCheckin.value || !bookingCheckout.value || checkout <= checkin) {
      showToast('Invalid Dates', 'Check-out date must be after check-in date', 'warning');
      return;
    }
    goToBookingStep(2);
  } else if (currentBookingStep === 2) {
    if (!bookingGuestName.value.trim()) {
      showToast('Missing Name', 'Please enter primary guest name', 'warning');
      bookingGuestName.focus();
      return;
    }
    goToBookingStep(3);
  }
});

btnModalPrev.addEventListener('click', () => {
  if (currentBookingStep > 1) {
    goToBookingStep(currentBookingStep - 1);
  }
});

// Dynamic price updates on input changes
[bookingCheckin, bookingCheckout, bookingGuestCount, bookingVipAddon].forEach(elem => {
  if (elem) elem.addEventListener('change', updatePricingUI);
});

// Reservation form submission directly linking to POST /api/bookings
bookingForm.addEventListener('submit', async e => {
  e.preventDefault();
  bookingConflictAlert.classList.add('hidden');

  const roomId = Number(bookingRoomId.value);
  let guestName = bookingGuestName.value.trim();
  const checkInDate = bookingCheckin.value;
  const checkOutDate = bookingCheckout.value;
  const guestCount = Number(bookingGuestCount.value) || 1;
  const simulateDecline = cbSimulateDecline && cbSimulateDecline.checked;

  if (simulateDecline && !guestName.toUpperCase().includes('DECLINE')) {
    guestName += ' [DECLINE]';
  }

  btnSubmitBooking.disabled = true;
  btnSubmitBooking.textContent = '⏳ Executing Transactional Saga...';

  try {
    const booking = await createBooking({ roomId, guestName, checkInDate, checkOutDate, guestCount });
    bookingModal.classList.add('hidden');
    
    if (simulateDecline) {
      showToast(
        'Saga Initiated (Simulating Failure)',
        `Reservation #${booking.id} created. Payment Service will reject transaction and trigger compensating rollback.`,
        'warning',
        7000
      );
    } else {
      showToast(
        'Booking Saga Initiated!',
        `Reservation #${booking.id} created for ${guestName} (${guestCount} guests). AMQP event published to RabbitMQ.`,
        'success',
        6500
      );
    }

    // Add confirmed card to live feed
    addEventToFeed({
      status: 'CONFIRMED',
      bookingReference: `Reservation #${booking.id} - ${guestName} (Room #${roomId}, ${guestCount} Guests)`,
      timestamp: new Date().toISOString(),
      receivedAt: new Date().toLocaleTimeString(),
    });

    // Refresh rooms so state is up-to-date
    await loadRooms();
  } catch (err) {
    if (err.status === 409) {
      bookingConflictAlert.classList.remove('hidden');
      bookingConflictAlert.innerHTML = `⚠️ <strong>409 Conflict:</strong> Room #${roomId} is already booked for ${checkInDate} to ${checkOutDate}. Feign Client & Booking Service prevented double-booking!`;
      showToast('Double-Booking Prevented', 'Conflict detected for overlapping dates (HTTP 409)', 'warning', 6000);
    } else {
      showToast('Booking Failed', err.message, 'danger', 6000);
    }
  } finally {
    btnSubmitBooking.disabled = false;
    btnSubmitBooking.textContent = '✨ Confirm Reservation & Dispatch Saga';
  }
});

// ---------------- WebSocket STOMP Live Listener ----------------
onConnectionStatusChange((status, details) => {
  if (!wsStatusDot || !wsStatusText) return;
  wsStatusDot.className = 'dot';
  if (status === 'CONNECTED') {
    wsStatusDot.classList.add('connected');
    wsStatusText.textContent = 'WS: Connected';
    showToast('Real-time Stream Connected', 'Subscribed to /topic/payments on API Gateway', 'success', 3000);
  } else if (status === 'CONNECTING') {
    wsStatusDot.classList.add('connecting');
    wsStatusText.textContent = 'WS: Connecting...';
  } else {
    wsStatusDot.classList.add('disconnected');
    wsStatusText.textContent = 'WS: Disconnected';
  }
});

onPaymentEvent(event => {
  liveEventsCount++;
  if (statEventsCount) statEventsCount.textContent = liveEventsCount;
  
  if (event.status === 'FAILED') {
    showToast(
      '🚨 Compensating Saga Triggered',
      `Payment Failed: ${event.reason || 'Card declined'}. Compensating rollback dispatched to Booking Service. Booking rolled back to CANCELLED_PAYMENT_FAILED.`,
      'danger',
      8000
    );
    loadRooms();
    if (myBookingsModal && !myBookingsModal.classList.contains('hidden')) {
      loadMyBookings();
    }
  } else {
    showToast(
      `Payment ${event.status || 'COMPLETED'}`,
      `Ref: ${event.bookingReference || 'Live Clearance'}<br><small style="opacity:0.8">${event.timestamp || new Date().toISOString()}</small>`,
      'info',
      6000
    );
  }

  addEventToFeed(event);
});

function addEventToFeed(event) {
  if (!liveEventFeed) return;
  const item = document.createElement('div');
  item.className = 'event-item';
  item.innerHTML = `
    <div class="event-item-head">
      <span class="event-item-title" style="color: var(--accent-cyan); font-weight: 700;">${event.status || 'COMPLETED'}</span>
      <span class="event-item-time">${event.receivedAt || new Date().toLocaleTimeString()}</span>
    </div>
    <div class="event-item-ref">${event.bookingReference || JSON.stringify(event)}</div>
  `;
  liveEventFeed.prepend(item);
}

// ---------------- AMQP Event Trigger Simulation ----------------
async function handleSimulateAmqp() {
  try {
    showToast('Dispatching AMQP Event', 'POST /api/bookings/test-event via Gateway...', 'info', 2500);
    const result = await simulateBookingEvent();
    showToast('Event Dispatched to RabbitMQ', result, 'success', 3500);
  } catch (err) {
    showToast('AMQP Dispatch Failed', err.message, 'danger', 5000);
  }
}

if (btnTriggerAmqpTest) btnTriggerAmqpTest.addEventListener('click', handleSimulateAmqp);

// ---------------- Resilience & Circuit Breaker Handling ----------------
onCircuitBreakerFallback(details => {
  console.warn('🛡️ Circuit Breaker Intercepted 503:', details);
  if (cbBanner && cbBannerMessage) {
    cbBanner.classList.remove('hidden');
    cbBannerMessage.textContent = `${details.message} (Triggered on ${details.path})`;
  }
  showToast(
    'Resilience4j Circuit Breaker Active',
    'Downstream booking service is unreachable or rate-limited. Serving /fallback/bookings (HTTP 503).',
    'danger',
    7000
  );
});

async function handleTestCircuitBreakerFallback() {
  try {
    showToast('Testing Circuit Breaker', 'Triggering /fallback/bookings on Gateway...', 'warning', 2500);
    await testCircuitBreakerFallback();
  } catch (err) {
    console.log('Handled fallback test response:', err);
  }
}

if (btnTestCb) btnTestCb.addEventListener('click', handleTestCircuitBreakerFallback);
if (btnTriggerCbTest) btnTriggerCbTest.addEventListener('click', handleTestCircuitBreakerFallback);

if (btnDismissCb) {
  btnDismissCb.addEventListener('click', () => {
    cbBanner.classList.add('hidden');
  });
}

if (btnRetryCb) {
  btnRetryCb.addEventListener('click', async () => {
    cbBanner.classList.add('hidden');
    showToast('Probing Microservice Mesh', 'Testing route connectivity...', 'info', 2000);
    await loadRooms();
  });
}

if (btnRefreshHealth) {
  btnRefreshHealth.addEventListener('click', async () => {
    showToast('Discovery Health', 'Querying Eureka Discovery Server on port 8761...', 'info', 2000);
    const services = await fetchEurekaServices();
    if (services && services.length) {
      const list = services.map(s => s.name).join(', ');
      showToast('Services Healthy', `Registered: ${list}`, 'success', 5000);
    } else {
      showToast('Service Discovery', 'Gateway is routing traffic dynamically via Eureka.', 'success', 4000);
    }
  });
}

// ---------------- Modal Controls & Event Listeners ----------------
if (btnOpenBookingModal) {
  btnOpenBookingModal.addEventListener('click', () => {
    openBookingModal();
  });
}

btnOpenLogin.addEventListener('click', () => {
  loginModal.classList.remove('hidden');
});

btnCloseLoginModal.addEventListener('click', () => {
  loginModal.classList.add('hidden');
});

btnCloseBookingModal.addEventListener('click', () => {
  bookingModal.classList.add('hidden');
});

btnCancelBooking.addEventListener('click', () => {
  bookingModal.classList.add('hidden');
});

btnLogout.addEventListener('click', () => {
  logout();
  showToast('Signed Out', 'JWT session cleared', 'info');
});

loginForm.addEventListener('submit', async e => {
  e.preventDefault();
  loginErrorAlert.classList.add('hidden');
  const user = document.getElementById('login-username').value.trim();
  const pass = document.getElementById('login-password').value.trim();

  try {
    await login(user, pass);
    loginModal.classList.add('hidden');
    showToast('Welcome!', `Authenticated as ${user}`, 'success');
    loadRooms();
  } catch (err) {
    loginErrorAlert.classList.remove('hidden');
    loginErrorAlert.textContent = `Login failed: ${err.message}`;
  }
});

if (searchQuery) searchQuery.addEventListener('input', () => renderRooms(allRooms));
if (filterType) filterType.addEventListener('change', () => renderRooms(allRooms));
if (filterPrice) filterPrice.addEventListener('change', () => renderRooms(allRooms));
if (filterAvailability) filterAvailability.addEventListener('change', () => renderRooms(allRooms));
if (btnRefreshRooms) btnRefreshRooms.addEventListener('click', loadRooms);
if (btnClearEvents) {
  btnClearEvents.addEventListener('click', () => {
    liveEventFeed.innerHTML = '';
  });
}

// ---------------- My Bookings Management Drawer Logic ----------------
async function loadMyBookings() {
  const user = getCurrentUser() || 'admin';
  if (!myBookingsLoading || !myBookingsEmpty || !myBookingsList) return;

  myBookingsLoading.classList.remove('hidden');
  myBookingsEmpty.classList.add('hidden');
  myBookingsList.classList.add('hidden');
  myBookingsList.innerHTML = '';

  try {
    const bookings = await getUserBookings(user);
    myBookingsLoading.classList.add('hidden');

    if (!bookings || bookings.length === 0) {
      myBookingsEmpty.classList.remove('hidden');
      return;
    }

    myBookingsList.classList.remove('hidden');
    bookings.sort((a, b) => (b.id || 0) - (a.id || 0));

    bookings.forEach(b => {
      const room = allRooms.find(r => r.id === b.roomId) || { category: 'Luxury Suite' };
      const card = document.createElement('div');
      card.style.cssText = 'background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 12px; padding: 16px; display: flex; justify-content: space-between; align-items: center; gap: 16px; transition: all 0.2s ease;';
      
      let statusColor = '#34d399';
      let statusBg = 'rgba(52, 211, 153, 0.15)';
      let statusText = b.status || 'CONFIRMED';
      let canCancel = b.status === 'CONFIRMED';

      if (b.status === 'CANCELLED') {
        statusColor = '#94a3b8';
        statusBg = 'rgba(148, 163, 184, 0.15)';
      } else if (b.status === 'CANCELLED_PAYMENT_FAILED') {
        statusColor = '#f43f5e';
        statusBg = 'rgba(244, 63, 94, 0.15)';
        statusText = 'ROLLBACK: PAYMENT DECLINED';
      }

      card.innerHTML = `
        <div style="flex: 1;">
          <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 6px;">
            <strong style="color: #ffffff; font-size: 1rem;">Reservation #${b.id}</strong>
            <span style="background: ${statusBg}; color: ${statusColor}; font-size: 0.72rem; font-weight: 700; padding: 3px 8px; border-radius: 6px; letter-spacing: 0.5px;">
              ${statusText}
            </span>
          </div>
          <div style="font-size: 0.86rem; color: var(--accent-gold); margin-bottom: 4px;">
            🏨 Room #${b.roomId} — ${room.category || 'Luxury Suite'}
          </div>
          <div style="font-size: 0.8rem; color: var(--text-muted); display: flex; flex-wrap: wrap; gap: 14px;">
            <span>📅 ${b.checkInDate || 'N/A'} → ${b.checkOutDate || 'N/A'}</span>
            <span>👤 ${b.guestName || 'Guest'} (${b.guestCount || 1} Guests)</span>
          </div>
        </div>
        <div>
          ${canCancel ? `
            <button class="btn btn-sm btn-cancel-res" data-id="${b.id}" style="background: rgba(244, 63, 94, 0.15); color: #fda4af; border: 1px solid rgba(244, 63, 94, 0.4); white-space: nowrap; font-size: 0.8rem; padding: 6px 12px; border-radius: 6px; cursor: pointer;">
              Cancel Booking
            </button>
          ` : `
            <span style="font-size: 0.78rem; color: var(--text-muted); font-style: italic;">Inactive</span>
          `}
        </div>
      `;

      myBookingsList.appendChild(card);
    });

    // Add click listeners to Cancel buttons
    myBookingsList.querySelectorAll('.btn-cancel-res').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        if (confirm(`Are you sure you want to cancel Reservation #${id}? This will immediately free up the suite dates.`)) {
          e.target.disabled = true;
          e.target.textContent = 'Cancelling...';
          try {
            await cancelBooking(id);
            showToast('Reservation Cancelled', `Booking #${id} has been cancelled and suite dates are released.`, 'success', 5000);
            await loadMyBookings();
            await loadRooms();
          } catch (err) {
            showToast('Cancellation Failed', err.message, 'danger', 5000);
            e.target.disabled = false;
            e.target.textContent = 'Cancel Booking';
          }
        }
      });
    });

  } catch (err) {
    if (myBookingsLoading) myBookingsLoading.classList.add('hidden');
    showToast('Failed to Load Bookings', err.message, 'danger', 5000);
  }
}

if (btnOpenMyBookings) {
  btnOpenMyBookings.addEventListener('click', () => {
    if (myBookingsModal) myBookingsModal.classList.remove('hidden');
    loadMyBookings();
  });
}

if (btnCloseMyBookingsModal) {
  btnCloseMyBookingsModal.addEventListener('click', () => {
    if (myBookingsModal) myBookingsModal.classList.add('hidden');
  });
}

if (btnCloseMyBookingsFooter) {
  btnCloseMyBookingsFooter.addEventListener('click', () => {
    if (myBookingsModal) myBookingsModal.classList.add('hidden');
  });
}

if (btnRefreshMyBookings) {
  btnRefreshMyBookings.addEventListener('click', () => {
    loadMyBookings();
  });
}

// ---------------- Bootstrap Initialization ----------------
async function init() {
  console.log('🚀 Initializing Starlight Stays Frontend...');
  await ensureAuthenticated();
  await loadRooms();
  connectWebSocket();
}

init();
