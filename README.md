# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W – Client-Server Architectures  
**Title:** Smart Campus RESTful API  
**Technology Stack:** Java 11 · JAX-RS (Jersey 2.41) · Jackson · Apache Tomcat  

---

## Table of Contents

1. [API Design Overview](#api-design-overview)
2. [Project Structure](#project-structure)
3. [How to Build and Run](#how-to-build-and-run)
4. [API Endpoints Reference](#api-endpoints-reference)
5. [Sample curl Commands](#sample-curl-commands)
6. [Conceptual Report (Question Answers)](#conceptual-report-question-answers)

---

## API Design Overview

This project implements a RESTful API for the University of Westminster's "Smart Campus" initiative. The API manages **Rooms** and **Sensors** deployed within those rooms, and maintains a historical log of **Sensor Readings**.

### Resource Hierarchy

```
/api/v1                          ← Discovery endpoint
/api/v1/rooms                    ← Room collection
/api/v1/rooms/{roomId}           ← Individual room
/api/v1/sensors                  ← Sensor collection (supports ?type= filter)
/api/v1/sensors/{sensorId}       ← Individual sensor
/api/v1/sensors/{sensorId}/readings   ← Reading history (sub-resource)
```

### Core Design Principles

- **Resource-based URLs** – every entity (Room, Sensor, SensorReading) is a first-class resource addressable by a stable URI.
- **Correct HTTP semantics** – `GET` for retrieval, `POST` for creation, `DELETE` for removal; appropriate status codes (`201 Created`, `204 No Content`, `404`, `409`, `422`, `403`, `500`).
- **In-memory data store** – `CampusDataStore` is a thread-safe singleton backed by `ConcurrentHashMap`, seeded with two rooms and two sensors at startup.
- **Leak-proof error handling** – every exceptional condition is caught by a dedicated `ExceptionMapper`; raw stack traces are never exposed to clients.
- **Observability** – a JAX-RS `ContainerRequestFilter` / `ContainerResponseFilter` logs every incoming request and outgoing response status using `java.util.logging`.

### Data Models

| Class | Key Fields |
|-------|-----------|
| `Room` | `id`, `name`, `capacity`, `sensorIds` |
| `Sensor` | `id`, `type`, `status` (`ACTIVE`/`MAINTENANCE`/`OFFLINE`), `currentValue`, `roomId` |
| `SensorReading` | `id` (UUID), `timestamp` (epoch ms), `value` |

---

## Project Structure

```
CampusAPI_renamed/
├── pom.xml
└── src/main/java/com/campus/api/
    ├── app/
    │   └── CampusApplication.java          # JAX-RS Application, @ApplicationPath("/api/v1")
    ├── endpoint/
    │   ├── ApiInfoResource.java            # GET /api/v1
    │   ├── SensorRoomResource.java         # /api/v1/rooms
    │   ├── SensorResource.java             # /api/v1/sensors
    │   └── SensorReadingResource.java      # sub-resource: /api/v1/sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java               # thrown when deleting a room with sensors
    │   ├── RoomNotEmptyExceptionMapper.java          # → HTTP 409
    │   ├── LinkedResourceNotFoundException.java      # thrown when roomId is missing
    │   ├── LinkedResourceNotFoundExceptionMapper.java # → HTTP 422
    │   ├── SensorUnavailableException.java           # thrown for MAINTENANCE sensor
    │   ├── SensorUnavailableExceptionMapper.java     # → HTTP 403
    │   └── FallbackExceptionMapper.java              # catches Throwable → HTTP 500
    ├── filter/
    │   └── RequestResponseLogger.java      # logs all requests & response codes
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ApiError.java                   # uniform JSON error envelope
    └── storage/
        └── CampusDataStore.java            # singleton, ConcurrentHashMap-backed store
```

---

## How to Build and Run

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK    | 11 or higher |
| Apache Maven | 3.6+ |
| Apache Tomcat | 9.x (or any Servlet 4.0-compatible container) |

### Step 1 – Clone the repository

```bash
git clone https://github.com/<your-username>/CampusAPI.git
cd CampusAPI
```

### Step 2 – Build the WAR file

```bash
mvn clean package
```

A successful build produces:

```
target/CampusAPI-1.0-SNAPSHOT.war
```

### Step 3 – Deploy to Tomcat

**Option A – Manual deploy**

1. Copy the WAR to your Tomcat `webapps/` directory:

```bash
cp target/CampusAPI-1.0-SNAPSHOT.war $TOMCAT_HOME/webapps/CampusAPI.war
```

2. Start Tomcat:

```bash
$TOMCAT_HOME/bin/startup.sh       # Linux / macOS
$TOMCAT_HOME\bin\startup.bat      # Windows
```

**Option B – Deploy via Tomcat Manager**

1. Open `http://localhost:8080/manager/html` in your browser.
2. Under **WAR file to deploy**, choose `target/CampusAPI-1.0-SNAPSHOT.war` and click **Deploy**.

### Step 4 – Verify the API is running

```bash
curl http://localhost:8080/CampusAPI/api/v1
```

You should receive a JSON discovery response with version info and resource links.

> **Default base URL:** `http://localhost:8080/CampusAPI/api/v1`  
> Adjust the host/port/context-path if your Tomcat uses different settings.

---

## API Endpoints Reference

### Discovery

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| GET | `/api/v1` | Returns API metadata and resource links | 200 |

### Rooms – `/api/v1/rooms`

| Method | Path | Description | Success | Error |
|--------|------|-------------|---------|-------|
| GET | `/rooms` | List all rooms | 200 | – |
| POST | `/rooms` | Create a new room | 201 | 400 (missing id) |
| GET | `/rooms/{roomId}` | Fetch a single room | 200 | 404 |
| DELETE | `/rooms/{roomId}` | Delete a room (must have no sensors) | 204 | 404, 409 |

### Sensors – `/api/v1/sensors`

| Method | Path | Description | Success | Error |
|--------|------|-------------|---------|-------|
| GET | `/sensors` | List all sensors (optional `?type=` filter) | 200 | – |
| POST | `/sensors` | Register a new sensor | 201 | 422 (roomId not found) |
| GET | `/sensors/{sensorId}` | Fetch a single sensor | 200 | 404 |

### Sensor Readings – `/api/v1/sensors/{sensorId}/readings`

| Method | Path | Description | Success | Error |
|--------|------|-------------|---------|-------|
| GET | `/sensors/{id}/readings` | Retrieve full reading history | 200 | – |
| POST | `/sensors/{id}/readings` | Record a new reading; updates `currentValue` | 201 | 403 (MAINTENANCE), 404 |

### Error Response Shape

All error responses use the `ApiError` envelope:

```json
{
  "statusCode": 409,
  "errorType": "Conflict",
  "detail": "Cannot remove room LH-201 — it still has 1 sensor(s) assigned.",
  "occurredAt": 1713780000000
}
```

---

## Sample curl Commands

> Replace `localhost:8080` with your server address if different.  
> The API is pre-seeded with room `LH-201`, room `CS-LAB-05`, sensor `TMP-101` (Temperature/ACTIVE), and sensor `HUM-202` (Humidity/ACTIVE).

---

### 1. Get API discovery info

```bash
curl -s http://localhost:8080/CampusAPI/api/v1 | python3 -m json.tool
```

**Expected response:**

```json
{
  "api": "Campus Sensor Management API",
  "version": "1.0",
  "maintainer": "campus-admin@university.ac.uk",
  "status": "running",
  "endpoints": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2. Create a new room

```bash
curl -s -X POST http://localhost:8080/CampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":60}' \
  | python3 -m json.tool
```

**Expected response (201 Created):**

```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 60,
  "sensorIds": []
}
```

---

### 3. Register a new sensor linked to a room

```bash
curl -s -X POST http://localhost:8080/CampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-301","type":"CO2","status":"ACTIVE","currentValue":400.0,"roomId":"LIB-301"}' \
  | python3 -m json.tool
```

**Expected response (201 Created):**

```json
{
  "id": "CO2-301",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 400.0,
  "roomId": "LIB-301"
}
```

---

### 4. Get all sensors filtered by type

```bash
curl -s "http://localhost:8080/CampusAPI/api/v1/sensors?type=Temperature" \
  | python3 -m json.tool
```

**Expected response (200 OK):**

```json
[
  {
    "id": "TMP-101",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 21.3,
    "roomId": "LH-201"
  }
]
```

---

### 5. Post a sensor reading and verify currentValue is updated

```bash
# Post a reading for TMP-101
curl -s -X POST http://localhost:8080/CampusAPI/api/v1/sensors/TMP-101/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}' \
  | python3 -m json.tool

# Verify the sensor's currentValue has been updated to 23.7
curl -s http://localhost:8080/CampusAPI/api/v1/sensors/TMP-101 | python3 -m json.tool
```

**Expected reading response (201 Created):**

```json
{
  "id": "a3f2c1d4-...",
  "timestamp": 1713780123456,
  "value": 23.7
}
```

---

### 6. Attempt to delete a room that still has sensors (409 Conflict)

```bash
curl -s -X DELETE http://localhost:8080/CampusAPI/api/v1/rooms/LH-201 \
  | python3 -m json.tool
```

**Expected response (409 Conflict):**

```json
{
  "statusCode": 409,
  "errorType": "Conflict",
  "detail": "Cannot remove room LH-201 — it still has 1 sensor(s) assigned. Reassign or delete all sensors first.",
  "occurredAt": 1713780000000
}
```

---

### 7. Attempt to register a sensor with a non-existent roomId (422 Unprocessable Entity)

```bash
curl -s -X POST http://localhost:8080/CampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"GAS-999","type":"Gas","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-ROOM"}' \
  | python3 -m json.tool
```

**Expected response (422 Unprocessable Entity):**

```json
{
  "statusCode": 422,
  "errorType": "Unprocessable Entity",
  "detail": "Cannot register sensor: room 'FAKE-ROOM' does not exist in the system.",
  "occurredAt": 1713780000000
}
```


---
 
## Conceptual Report (Question Answers)
 
### Part 1: Service Architecture & Setup
 
#### Q1 – JAX-RS Resource Lifecycle & Thread Safety
 
JAX-RS will by default create a new instance of every resource class on each incoming HTTP request. Think of it as a disposable cup — it is created, used, and discarded. This means any data saved as a normal instance variable within a resource class would be lost the moment the request completes.
 
Because of this, rooms, sensors, and readings cannot be stored within the resource classes themselves. Instead, this project uses a dedicated `CampusDataStore` class, which holds all data as fields in a single class — meaning they exist at the JVM level and persist between requests. The maps are not plain `HashMap` but `ConcurrentHashMap`, because multiple requests can arrive at the server simultaneously. A standard `HashMap` is not thread-safe — two threads writing at the same time may corrupt its internal structure and lose data. `ConcurrentHashMap` is a thread-safe alternative that does not require explicit `synchronized` blocks.
 
#### Q2 – Hypermedia and HATEOAS
 
HATEOAS stands for Hypermedia as the Engine of Application State. The idea is straightforward: a client does not need to consult separate documentation to discover URLs — links are embedded directly in API responses.
 
The Discovery endpoint (`GET /api/v1`) in this API returns a `links` map pointing clients to the exact locations of the rooms and sensors collections. The advantages for client developers are significant: they do not need to hard-code URLs into their applications — they simply follow the links. If the server ever changes a URL, only the discovery response needs updating, not every client. It also makes the API self-documenting, reducing the risk of a client breaking due to stale static documentation.
 
---
 
### Part 2: Room Management
 
#### Q1 – Returning Full Objects vs. IDs Only
 
When the API returns only IDs in a room list, the response payload is minimal, saving bandwidth. However, the client must then make a separate `GET` request for each ID to retrieve the actual details — this is known as the N+1 problem. With 100 rooms, that means 100 additional requests, which is inefficient.
 
When the API returns full room objects in a single call, the payload is larger, but the client gets everything it needs in one round trip. This API returns whole objects, which is appropriate for a small-to-medium in-memory dataset. A large production system would typically introduce pagination to limit the number of rooms returned per request.
 
#### Q2 – Idempotency of DELETE
 
Yes, `DELETE` is idempotent in this implementation. Idempotency means that no matter how many times the same request is made, the final server state is identical.
 
Here is what happens across multiple `DELETE` calls on the same room:
 
- **First call** – the room is found, removed, and `204 No Content` is returned.
- **Second call** – the room is no longer found, so `404 Not Found` is returned.
The status code differs, but the important point is that in both cases the server ends up in the same state: the room does not exist. This is precisely what HTTP idempotency means — regardless of how many times the request is issued, the server reaches the same final state.
 
---
 
### Part 3: Sensor Operations & Linking
 
#### Q1 – `@Consumes` and Content-Type Enforcement
 
The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that a POST endpoint will only accept requests where the `Content-Type` header is set to `application/json`. If a client sends `text/plain` or `application/xml` instead, JAX-RS checks this before the method is ever invoked. Finding no match for those content types, the framework automatically returns `HTTP 415 Unsupported Media Type` without executing any application code. This is important because if the wrong format were allowed through, Jackson would be unable to deserialise it, potentially producing a `NullPointerException` or a corrupt object further down the call chain.
 
#### Q2 – Query Parameters vs. Path Segments for Filtering
 
Filtering by a query parameter such as `?type=CO2` (`GET /api/v1/sensors?type=CO2`) is the correct approach because query parameters are designed for optional search and filter values — they are not part of a resource's identity. A path-based alternative like `/api/v1/sensors/type/CO2` is misleading: it implies that `CO2` is a discrete resource living at that path, which it is not — it is simply a filter value.
 
Query parameters are also far more flexible. Multiple filters can be combined without changing the URL structure, for example `?type=CO2&status=ACTIVE`. Path-based filters cannot achieve this without defining entirely new route patterns for each combination. The established REST convention is that path segments identify resources, while query parameters modify or filter how those resources are retrieved.
 
---
 
### Part 4: Deep Nesting with Sub-Resources
 
#### Q1 – Sub-Resource Locator Pattern
 
The Sub-Resource Locator pattern means a resource class does not need to handle every possible URL itself — it can delegate a sub-path to a separate, specialised class. In this API, `SensorResource` handles `/sensors` and `/sensors/{id}`, but when the path extends to `/sensors/{id}/readings`, it instantiates and returns a `SensorReadingResource` to take over.
 
The key benefit is the Single Responsibility Principle — one class has one job. All reading-related logic lives in `SensorReadingResource`: the `MAINTENANCE` status check, the update to the parent sensor's `currentValue`, and so on. If all of this were merged into `SensorResource` alongside the other sensor operations, the class would become enormous, hard to understand, and difficult to maintain. Separate classes are also easier to unit test in isolation.
 
---
 
### Part 5: Advanced Error Handling, Exception Mapping & Logging
 
#### Q1 – HTTP 422 vs. 404 for Missing References
 
When a client submits a new sensor with a `roomId` that does not exist, returning `404 Not Found` would be confusing because the URL the client requested — `POST /api/v1/sensors` — was found perfectly well. The issue is not a missing endpoint but a problem inside the request body.
 
`HTTP 422 Unprocessable Entity` is more semantically accurate because it signals: "I received your request, your JSON is valid, but I cannot process it because something within the data is wrong." In this case, the `roomId` value references a room that does not exist — a semantic validation failure. Responding with `404` would mislead the client into thinking the endpoint itself is missing, when in fact it is the data reference that is broken.
 
#### Q2 – Risks of Exposing Java Stack Traces
 
Exposing raw Java stack traces to external API consumers poses several security risks. A stack trace reveals internal package names, class names, and method names, giving an attacker a detailed map of the codebase. It also discloses the names and versions of libraries and frameworks in use — an attacker can cross-reference these against known vulnerability databases (CVEs) to find exploits targeting those specific versions. If the error involves file access or configuration, the trace may expose server file paths and system usernames. It also reveals the exact line where the error occurred, making it straightforward to craft inputs that reliably trigger the failure.
 
This project avoids all of this through `FallbackExceptionMapper`, which intercepts all unhandled `Throwable`s, logs the full detail to a secure server-side log accessible only to administrators, and returns a generic `500 Internal Server Error` JSON response to the client with no internal information whatsoever.
 
#### Q3 – JAX-RS Filters for Cross-Cutting Concerns
 
Logging is a cross-cutting concern — it must happen across all endpoints, not just a select few. Manually inserting `Logger.info()` calls inside every resource method duplicates the same code dozens of times throughout the project, violating the DRY (Don't Repeat Yourself) principle. It also becomes a maintenance nightmare: changing the log format or adding a new field to every log entry would require modifying each method individually.
 
A JAX-RS filter annotated with `@Provider` is registered once and runs automatically on every request and response across the entire API, without touching any resource code. This keeps resource methods clean and focused on business logic only. The filter also has direct access to `ContainerRequestContext` and `ContainerResponseContext`, which provide the HTTP method, full request URI, and response status code — exactly the fields needed for meaningful request/response logging.
