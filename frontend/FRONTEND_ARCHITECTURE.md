# Starlight Stays — Frontend Architecture & Workflow Documentation

## 1. Executive Summary & Architectural Vision

The **Starlight Stays Frontend** is a modern, responsive Single-Page Application (SPA) designed for luxury hospitality booking. Built with **Vite 5**, **ES6+ JavaScript modules**, **Semantic HTML5**, and **Vanilla CSS**, the frontend operates as the presentation and client-side orchestration layer within a distributed **Service-Oriented Architecture (SOA) / Spring Cloud Microservices Mesh**.

### Key Architectural Highlights
- **Zero-Heavy-Framework Overhead**: High-performance native DOM rendering without heavy virtual DOM runtimes, ensuring sub-second cold loads and minimal memory footprint.
- **Central Edge Routing**: All HTTP and WebSocket communications route exclusively through a unified **Spring Cloud API Gateway (Port 8080)**. Downstream services remain decoupled from the client.
- **Resilient Observer Architecture**: Built-in pub/sub listeners for authentication transitions, real-time STOMP payment events, and circuit breaker trip detection.
- **Graceful Fault Interception**: Native HTTP 503 / Circuit Breaker interception with active UI alerts, allowing users to probe service availability without breaking UI state.
- **Real-time Event Streaming**: Full-duplex STOMP over SockJS connection for instant payment status and clearance broadcasting.

---

## 2. Technology Stack & Directory Structure

### 2.1 Technology Stack Matrix

| Layer / Concern | Technology / Library | Purpose |
| :--- | :--- | :--- |
| **Build & Tooling** | Vite 5 (`vite`, ES Modules) | Fast HMR, development server (`:5173`), optimized asset bundling |
| **Core Client Logic** | ES6+ Vanilla JavaScript | Module-based state management, DOM manipulation, pub/sub event bus |
| **User Interface** | HTML5 + Custom Vanilla CSS | CSS Custom Properties, Glassmorphism, Flexbox/Grid layouts, Dark luxury styling |
| **Real-time Comms** | SockJS Client & STOMP.js | Fallback-capable WebSocket connection for event-driven updates |
| **Session Security** | JWT (JSON Web Tokens) | Stateless bearer token storage (`localStorage`) and automated request injection |
| **Edge Gateway** | Spring Cloud Gateway (`:8080`) | Reverse proxy, route rewriting, Resilience4j circuit breaking |

### 2.2 Directory Structure

```
frontend/
├── index.html                  # Core application shell & semantic HTML structure
├── package.json                # Project metadata, Vite dev/build scripts, dependencies
├── package-lock.json           # Pinned dependency tree
├── dist/                       # Production build output
└── src/
    ├── index.css               # Design system, CSS variables, layout, animations
    ├── main.js                 # App controller: bootstrapping, UI binding, event handlers
    ├── assets/                 # Luxury property imagery (penthouse, nebula, aurora, etc.)
    │   ├── aurora.jpg
    │   ├── nebula.jpg
    │   ├── penthouse.jpg
    │   └── waterfront.jpg
    └── services/
        ├── api.js              # Central HTTP client, JWT auth, circuit breaker interceptor
        └── websocket.js        # SockJS & STOMP client manager with exponential backoff
```

---

## 3. Component & Layer Architecture

```mermaid
graph TD
    subgraph Client ["Client Browser (Frontend)"]
        HTML["index.html\n(App Shell & Modals)"]
        CSS["index.css\n(Design System & Themes)"]
        MAIN["src/main.js\n(App Controller & State Engine)"]
        
        subgraph Services ["Service Layer (src/services/)"]
            API["api.js\n- Central apiRequest()\n- JWT Session Manager\n- Circuit Breaker Interceptor"]
            WS["websocket.js\n- SockJS & STOMP Client\n- Auto-reconnect with Backoff\n- Topic Subscriber"]
        end
    end

    subgraph Edge ["API Gateway Layer (Port 8080)"]
        GW["Spring Cloud API Gateway"]
        CB["Resilience4j Circuit Breaker\n(/fallback/bookings -> 503)"]
    end

    subgraph Mesh ["Microservices Mesh & Infrastructure"]
        AUTH_SVC["User / Auth Service\n(/api/users/login)"]
        ROOM_SVC["Room Catalog Service\n(/api/rooms)"]
        BOOK_SVC["Booking Service\n(/api/bookings)"]
        PAY_SVC["Payment Service"]
        RABBIT["RabbitMQ Broker\n(AMQP Events)"]
        EUREKA["Eureka Discovery Server\n(:8761)"]
    end

    HTML --> MAIN
    CSS --> HTML
    MAIN --> API
    MAIN --> WS

    API -->|HTTP REST + Bearer JWT| GW
    WS -->|STOMP over SockJS /ws-starlight| GW

    GW --> CB
    GW -->|Route: /api/users/**| AUTH_SVC
    GW -->|Route: /api/rooms/**| ROOM_SVC
    GW -->|Route: /api/bookings/**| BOOK_SVC
    GW -.->|Query Registered Services| EUREKA

    BOOK_SVC -->|Feign Client Check| ROOM_SVC
    BOOK_SVC -->|Publish Booking Event| RABBIT
    RABBIT -->|Consume Event| PAY_SVC
    PAY_SVC -->|Broadcast to /topic/payments| GW
```

---

## 4. Detailed Component Breakdowns

### 4.1 Application Orchestrator (`src/main.js`)
The `main.js` module coordinates presentation, state, and interaction.
- **State Management**:
  - `allRooms`: Master cache of room models loaded from the backend.
  - `liveEventsCount`: Counter tracking real-time STOMP transaction events.
  - Client-side search and multi-criteria filtering (search keywords, room type, availability).
- **Observer Hooks**:
  - Registers `onAuthStateChange` to toggle user badge/sign-in buttons.
  - Registers `onCircuitBreakerFallback` to raise the persistent resilience alert banner.
  - Registers `onPaymentEvent` to update stats, show animated toasts, and prepend live event cards.
  - Registers `onConnectionStatusChange` to update the WebSocket visual status pill (`connected`, `connecting`, `disconnected`).

### 4.2 Central API Client (`src/services/api.js`)
Encapsulates all outbound HTTP requests with interceptors:
- **JWT Injection**: Automatically appends `Authorization: Bearer <token>` to request headers if authenticated.
- **Automated JSON Parsing**: Serializes outgoing payload objects and parses incoming responses.
- **503 Circuit Breaker Interception**: Detects HTTP 503 status, notifies registered circuit breaker listeners, and throws structured errors.
- **401 Unauthorized Interception**: Clears expired tokens, resets user state, and prompts re-authentication.
- **Network Failure Fallback**: Detects network connection loss and alerts the circuit breaker listener.

### 4.3 WebSocket & STOMP Client (`src/services/websocket.js`)
Manages the real-time event pipeline:
- Connects to `http://localhost:8080/ws-starlight` using SockJS.
- Initializes STOMP over the SockJS transport.
- Subscribes to `/topic/payments`.
- **Fault-Tolerant Reconnect**: Uses exponential backoff ($1000 \times 1.5^{\text{attempts}}$ capped at 10s) for up to 10 retry attempts on connection drops.

---

## 5. End-to-End Workflow Flowcharts

### Workflow 1: Application Bootstrapping & Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Browser as Browser (index.html)
    participant Main as src/main.js
    participant API as src/services/api.js
    participant WS as src/services/websocket.js
    participant Gateway as Spring Cloud Gateway (:8080)

    User->>Browser: Loads http://localhost:5173
    Browser->>Main: Executes init()
    Main->>API: ensureAuthenticated()
    alt Token exists in localStorage
        API-->>Main: Return cached user session
        Main->>Browser: Render user badge (admin)
    else No Token Found
        Main->>API: login("admin", "password")
        API->>Gateway: POST /api/users/login
        Gateway-->>API: 200 OK + JWT String
        API-->>Main: Set localStorage session
        Main->>Browser: Render user badge & show Auth Toast
    end

    Main->>API: fetchRooms()
    API->>Gateway: GET /api/rooms (Bearer JWT)
    Gateway-->>API: 200 OK [Room List JSON]
    API-->>Main: allRooms = data
    Main->>Browser: renderRooms(allRooms) & updateStats()

    Main->>WS: connectWebSocket()
    WS->>Gateway: SockJS Handshake (/ws-starlight)
    Gateway-->>WS: Upgrade Connection
    WS->>Gateway: STOMP CONNECT frame
    Gateway-->>WS: STOMP CONNECTED frame
    WS->>Gateway: SUBSCRIBE /topic/payments
    WS-->>Main: Status: CONNECTED
    Main->>Browser: Status Pill: WS Connected (Green)
```

---

### Workflow 2: Room Browsing, Search, and Filtering

```mermaid
flowchart TD
    Start([User modifies Search or Filter]) --> Event[Input / Change Event on DOM]
    Event --> ReadInputs[Read: searchQuery, filterType, filterAvailability]
    ReadInputs --> FilterLoop[Filter in-memory allRooms array]
    
    FilterLoop --> QCheck{Matches Query?}
    QCheck -- No --> Exclude[Exclude Room]
    QCheck -- Yes --> TCheck{Matches Room Type?}
    
    TCheck -- No --> Exclude
    TCheck -- Yes --> ACheck{Matches Availability?}
    
    ACheck -- No --> Exclude
    ACheck -- Yes --> Include[Include Room in filtered list]
    
    Include --> RenderCheck{Filtered list empty?}
    Exclude --> RenderCheck
    
    RenderCheck -- Yes --> ShowEmpty[Render 'No Stays Match' Empty State]
    RenderCheck -- No --> MapCards[Map Room Array to Luxury Card HTML]
    MapCards --> InjectDOM[Update #rooms-container innerHTML]
    InjectDOM --> Rebind[Attach Event Listeners to 'Reserve Now' Buttons]
    Rebind --> Done([Updated Catalog UI])
    ShowEmpty --> Done
```

---

### Workflow 3: Reservation Creation & Conflict Handling (HTTP 409)

```mermaid
sequenceDiagram
    autonumber
    actor Guest as User / Guest
    participant Main as src/main.js
    participant Modal as #booking-modal
    participant API as src/services/api.js
    participant Gateway as API Gateway (:8080)
    participant BookingSvc as Booking Service
    participant RoomSvc as Room Service
    participant Rabbit as RabbitMQ

    Guest->>Main: Click "Reserve Now" on Room #1
    Main->>Main: Check isAuthenticated()
    Main->>Modal: Populate Room Details & Default Dates (Tomorrow + 3 days)
    Modal-->>Guest: Displays interactive Booking Modal
    Guest->>Modal: Submits Guest Name & Check-In / Out Dates
    Modal->>Main: form.submit event
    Main->>API: createBooking({ roomId, guestName, checkInDate, checkOutDate })
    API->>Gateway: POST /api/bookings/create (with JWT)
    Gateway->>BookingSvc: Route request to Booking Service
    BookingSvc->>RoomSvc: Feign Client verification (Is Room available for dates?)
    
    alt Overlapping Booking Conflict
        RoomSvc-->>BookingSvc: Room Unavailable / Booked
        BookingSvc-->>Gateway: HTTP 409 Conflict
        Gateway-->>API: 409 Conflict + Error Message
        API-->>Main: Throws Error (status: 409)
        Main->>Modal: Unhide #booking-conflict-alert
        Main->>Guest: Toast: "Double-Booking Prevented (HTTP 409)"
    else Booking Successfully Reserved
        BookingSvc->>BookingSvc: Persist Booking Record (Status: CONFIRMED)
        BookingSvc->>Rabbit: Publish AMQP Booking Event
        BookingSvc-->>Gateway: HTTP 200 OK (Booking JSON)
        Gateway-->>API: 200 OK (Booking JSON)
        API-->>Main: Return booking payload
        Main->>Modal: Close modal
        Main->>Guest: Toast: "Booking Confirmed! Reservation #ID"
        Main->>Main: Prepend Confirmed Card to Live Event Feed
    end
```

---

### Workflow 4: Real-time Payment Event Streaming (STOMP / SockJS)

```mermaid
sequenceDiagram
    autonumber
    participant Rabbit as RabbitMQ Broker
    participant PaySvc as Payment Service
    participant Gateway as Spring Cloud Gateway
    participant WS as src/services/websocket.js
    participant Main as src/main.js
    participant DOM as Live Event Feed & Toasts

    Rabbit->>PaySvc: Delivers Booking Event
    PaySvc->>PaySvc: Process Simulated Payment Transaction
    PaySvc->>Gateway: STOMP SEND /topic/payments (Payment JSON)
    Gateway->>WS: Push STOMP MESSAGE frame over WebSocket
    WS->>WS: JSON.parse(message.body)
    WS->>Main: dispatchEvent(parsedPayment)
    Main->>Main: liveEventsCount++
    Main->>DOM: Update stat-events-count in Hero Stats
    Main->>DOM: showToast("Payment COMPLETED", Ref: Clearance ID, type: "info")
    Main->>DOM: Prepend new .event-item into #live-event-feed
```

---

### Workflow 5: Resilience4j Circuit Breaker Interception & Fallback

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Main as src/main.js
    participant API as src/services/api.js
    participant Gateway as Spring Cloud Gateway (:8080)
    participant CB as Resilience4j Filter
    participant Banner as #circuit-breaker-banner
    participant Toast as Toast Notification Shelf

    User->>Main: Clicks "Test Circuit Breaker Fallback"
    Main->>API: testCircuitBreakerFallback()
    API->>Gateway: POST /fallback/bookings
    Gateway->>CB: Evaluate Circuit State / Route
    CB-->>Gateway: Returns Fallback Body (HTTP 503 Service Unavailable)
    Gateway-->>API: HTTP 503 Service Unavailable
    API->>API: Intercept status === 503
    API->>API: notifyCircuitBreaker(details)
    API-->>Main: Dispatches to onCircuitBreakerFallback listeners
    Main->>Banner: Remove .hidden class (Display warning banner)
    Main->>Banner: Set message: "Resilience4j Circuit Breaker Active..."
    Main->>Toast: Display Danger Alert Toast (7s timeout)
    
    opt User Clicks "Retry Endpoint"
        User->>Banner: Click #btn-retry-cb
        Banner->>Main: Trigger retry handler
        Main->>Banner: Hide banner
        Main->>API: fetchRooms()
        API->>Gateway: GET /api/rooms
        Gateway-->>API: 200 OK (Rooms restored)
        API-->>Main: Re-render catalog
    end
```

---

## 6. Resilience & Security Mechanisms

### 6.1 Security Architecture
- **Stateless Bearer Tokens**: User credentials authenticate against `/api/users/login`, returning signed JWTs stored in `localStorage` under `starlight_jwt_token`.
- **Automatic Token Propagation**: Every downstream call initiated via `apiRequest()` injects the header:
  ```http
  Authorization: Bearer <starlight_jwt_token>
  ```
- **Proactive 401 Eviction**: Upon receiving an HTTP 401 Unauthorized response from any service, the frontend clears storage, invokes `notifyAuthChange(null)`, and pops the login modal to prevent unauthorized stale requests.

### 6.2 Resilience & Fault Handling
- **Non-Blocking 503 Interception**: Rather than crashing the application or showing broken UI fragments, 503 responses trigger a dedicated pub/sub channel displaying an informative status ribbon at the top of the viewport.
- **SockJS Transport Fallback**: If raw WebSockets are blocked by proxies or firewalls, SockJS smoothly falls back to HTTP long-polling or streaming without application reconfiguration.
- **Bounded Exponential Reconnect**: Prevents frontend reconnect storms against recovering brokers by throttling reconnect intervals exponentially up to 10 seconds.

---

## 7. Operational & Verification Guide

### 7.1 Running the Frontend Locally
```bash
# Navigate to the frontend workspace
cd frontend

# Install required dependencies
npm install

# Start the Vite development server (Port 5173)
npm run dev
```

### 7.2 Microservice Connectivity Verification Checklist
| Test Case | Expected Frontend Reaction |
| :--- | :--- |
| **Gateway Reachability** | Header pill displays `Gateway: 8080` and room catalog renders 6 suites. |
| **WebSocket Connection** | Header pill toggles to `WS: Connected` (green dot) upon STOMP handshake. |
| **Auth Flow** | Login modal validates credentials; user badge displays `admin`. |
| **Double Booking (409)** | Reserving already booked dates flags the room with a red conflict alert box. |
| **Circuit Breaker (503)** | Clicking "Test Circuit Breaker" displays the top Resilience4j warning banner. |
| **RabbitMQ Simulation** | Dispatching AMQP event triggers a real-time card in the live event feed. |
