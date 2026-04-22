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
