# Smart Campus - Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
Informatics Institute of Technology - University of Westminster  

---

- **Name:** Pavithma Madushni Fernando  
- **IIT Number:** 20240349  
- **UOW Number:** W2121211  

---

### [Video Demonstration](https://drive.google.com/file/d/1iIrCwZ4yRHey5oOj_T612dWdLUcXNo_h/view?usp=sharing) 

### [Conceptual Report](https://drive.google.com/file/d/1WzQYTJiuBt52dY81YqTfRdf6vtsdm263/view?usp=sharing)

---

## Section 1: Overview

The **Smart Campus API** is a RESTful web service that manages the physical rooms and IoT sensors across a university campus. It exposes a versioned endpoint collection under `/api/v1/` for creating and querying rooms, registering sensors, and recording real-time sensor readings (temperature, CO₂, occupancy, etc.).

The service is packaged as a standard **WAR** file and deployed to **Apache Tomcat**. Jersey acts purely as a JAX-RS servlet running inside the Tomcat servlet container — there is no embedded HTTP server such as Grizzly.

### Tech Stack

| Layer                  | Technology                                      |
|------------------------|-------------------------------------------------|
| Language               | Java 17                                         |
| JAX-RS Implementation  | Jersey 2.41 (`javax.ws.rs` namespace)           |
| Servlet Container      | Apache Tomcat 10 (external, WAR deployment)     |
| Packaging              | WAR (`maven-war-plugin`)                        |
| JSON Serialisation     | Jackson via `jersey-media-json-jackson`         |
| Dependency Injection   | HK2 via `jersey-hk2`                            |
| Build Tool             | Maven 3.8+                                      |
| In-Memory Storage      | `ConcurrentHashMap` / `CopyOnWriteArrayList`    |

> ⚠️ **No database is used.** All data lives in in-memory Java data structures for the lifetime of the server process, as required by the coursework specification.

### Design Principles

- **Resource-oriented design** — every URI identifies a noun, not a verb.
- **Consistent error shape** — every non-2xx response returns a JSON object with an `error` code and a human-readable `message`, never an HTML page or a raw stack trace.
- **Application-scoped, thread-safe stores** — all shared state lives in singleton `ConcurrentHashMap` instances, never in resource-class fields.
- **Sub-resource locator pattern** — `SensorReadingResource` is not a root resource; it is instantiated by a locator method on `SensorResource`, enforcing the parent-child hierarchy at the routing layer.
- **Centralised cross-cutting concerns** — request/response logging and exception mapping are handled by `@Provider`-annotated filters and mappers, not inline in resource methods.
- **HATEOAS discovery** — the root `/api/v1/` endpoint exposes all resource URIs and supported methods so clients do not need to hardcode paths.

---

## Section 2: How to Build and Run

### Prerequisites

| Requirement       | Minimum Version | Check Command          |
|-------------------|-----------------|------------------------|
| JDK               | 17              | `java -version`        |
| Maven             | 3.8             | `mvn -version`         |
| Apache Tomcat     | 10.x            | —                      |

> Download Tomcat 10 from [https://tomcat.apache.org/download-10.cgi](https://tomcat.apache.org/download-10.cgi) if you do not already have it.

### Step 1 — Build the WAR

```bash
cd smartcampus
mvn clean package
```

This compiles all sources and produces `target/smartcampus.war`. Maven downloads all declared dependencies automatically on the first run.

### Step 2 — Deploy to Tomcat

Copy the WAR into Tomcat's `webapps/` directory:

```bash
cp target/smartcampus.war $CATALINA_HOME/webapps/
```

*(Replace `$CATALINA_HOME` with your actual Tomcat installation path, e.g. `/usr/local/tomcat` or `/opt/homebrew/opt/tomcat/libexec`.)*

### Step 3 — Start Tomcat

```bash
$CATALINA_HOME/bin/startup.sh        # macOS / Linux
%CATALINA_HOME%\bin\startup.bat      # Windows
```

Tomcat auto-deploys the WAR. The API is then available at:

```
http://localhost:8080/smartcampus/api/v1/
```

> The context path `/smartcampus` comes from the WAR file name. If you rename the WAR to `ROOT.war`, the context path becomes `/` and the base URL becomes `http://localhost:8080/api/v1/`.

### Step 4 — Stop Tomcat

```bash
$CATALINA_HOME/bin/shutdown.sh       # macOS / Linux
```

### One-liner (build + copy)

```bash
mvn clean package -q && cp target/smartcampus.war $CATALINA_HOME/webapps/
```

Then start Tomcat as above.

---

## Section 3: Sample curl Commands

All commands assume the server is running and accessible at `http://localhost:8080/smartcampus`. Adjust the host/port and context path if your Tomcat configuration differs.

> You can also run the bundled test script: `bash test.sh`

---

### 1. Discovery — GET /api/v1/

```bash
curl -s http://localhost:8080/smartcampus/api/v1/ | python3 -m json.tool
```

Returns the API name, version, contact, HATEOAS resource links, and a server timestamp.

---

### 2. List all rooms — GET /api/v1/rooms

```bash
curl -s http://localhost:8080/smartcampus/api/v1/rooms | python3 -m json.tool
```

Returns the two seeded rooms (`LIB-301`, `LAB-101`) plus any rooms created at runtime.

---

### 3. Create a room — POST /api/v1/rooms

```bash
curl -s -X POST http://localhost:8080/smartcampus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Cafeteria","capacity":120}' | python3 -m json.tool
```

The server generates an ID (`ROOM-XXXXXX`). Returns `201 Created` with a `Location` header.

---

### 4. Get a specific room — GET /api/v1/rooms/{roomId}

```bash
curl -s http://localhost:8080/smartcampus/api/v1/rooms/LIB-301 | python3 -m json.tool
```

Returns the room object. Returns `404 NOT_FOUND` if the ID does not exist.

---

### 5. Attempted delete of a non-empty room — 409 Conflict

```bash
curl -i -X DELETE http://localhost:8080/smartcampus/api/v1/rooms/LIB-301
```

`LIB-301` has `TEMP-001` in its `sensorIds` list. Expected response:

```
HTTP/1.1 409 Conflict
{"error":"ROOM_NOT_EMPTY","roomId":"LIB-301","activeSensorCount":1,...}
```

---

### 6. Create sensor with invalid roomId — 422 Unprocessable Entity

```bash
curl -i -X POST http://localhost:8080/smartcampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"Humidity","roomId":"DOES-NOT-EXIST"}'
```

Expected response:

```
HTTP/1.1 422
{"error":"LINKED_RESOURCE_NOT_FOUND","field":"roomId","value":"DOES-NOT-EXIST"}
```

---

### 7. Filter sensors by type — GET /api/v1/sensors?type=Temperature

```bash
curl -s "http://localhost:8080/smartcampus/api/v1/sensors?type=Temperature" | python3 -m json.tool
```

Returns only sensors whose `type` field matches `Temperature` (case-insensitive).

---

### 8. Post a reading on a MAINTENANCE sensor — 403 Forbidden

```bash
curl -i -X POST http://localhost:8080/smartcampus/api/v1/sensors/CO2-007/readings \
  -H "Content-Type: application/json" \
  -d '{"value":400}'
```

`CO2-007` is seeded with status `MAINTENANCE`. Expected response:

```
HTTP/1.1 403 Forbidden
{"error":"SENSOR_UNAVAILABLE","sensorId":"CO2-007","status":"MAINTENANCE"}
```

---

### 9. Post a valid reading — POST /api/v1/sensors/{sensorId}/readings

```bash
curl -i -X POST http://localhost:8080/smartcampus/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.7}'
```

Returns `201 Created` with server-generated `id` (UUID) and `timestamp` (epoch ms). Also updates `TEMP-001`'s `currentValue` to `24.7`.

---

### 10. Get readings history — GET /api/v1/sensors/{sensorId}/readings

```bash
curl -s http://localhost:8080/smartcampus/api/v1/sensors/TEMP-001/readings | python3 -m json.tool
```

Returns all readings for `TEMP-001` sorted by `timestamp` descending (most recent first).

---

## Conceptual Report

---

### Part 1: Service Architecture & Setup (10 Marks)

#### 1. Project & Application Configuration (5 Marks)

In JAX-RS, resource classes are request-scoped by default, meaning a new instance of the resource class is created for each incoming HTTP request. This ensures that each request is handled independently without sharing object state.

However, this behaviour has important implications when using in-memory data structures such as `HashMap` or `ArrayList`. Since each request gets a new instance, data stored inside the resource class itself will not persist across requests unless it is stored in a shared or static structure.

- A new object is created per request  
- No shared state between requests by default  
- Shared data must be stored in static/global structures  
- Synchronization is required to avoid race conditions  
- Improper handling may lead to data inconsistency or loss  

---

#### 2. The "Discovery" Endpoint (5 Marks)

Hypermedia (HATEOAS) refers to including links within API responses that guide clients on what actions they can perform next. Instead of relying on fixed endpoints, clients dynamically discover available resources through the API responses.

This approach improves flexibility and usability because clients do not need prior knowledge of all endpoints. It also allows APIs to evolve without breaking existing clients.

- Makes APIs self-discoverable  
- Reduces dependency on static documentation  
- Improves flexibility and maintainability  
- Helps clients navigate the API dynamically  
- Supports better RESTful design principles  

---

### Part 2: Room Management (20 Marks)

#### 1. Room Resource Implementation (10 Marks)

When returning a list of rooms, the API can either return only room IDs or full room objects. Returning only IDs results in smaller responses and reduced network usage but requires additional requests from the client to fetch full details. Returning full objects provides all necessary data in a single response but increases payload size.

The choice depends on system requirements and data size.

**IDs only:**
- Smaller payload  
- Faster transmission  
- Requires extra client requests  

**Full objects:**
- More complete information  
- Fewer API calls needed  
- Higher bandwidth usage  

---

#### 2. Room Deletion & Safety Logic (10 Marks)

The DELETE operation is considered idempotent because performing the same request multiple times results in the same final state. Once a resource is deleted, repeating the DELETE request will not change the outcome further.

For example, if a room is deleted successfully, subsequent DELETE requests for the same room will still result in the room being absent from the system.

- First DELETE removes the resource  
- Repeated DELETE requests do not change the result  
- Final system state remains consistent  
- Ensures reliability in API behaviour  

---

### Part 3: Sensor Operations & Linking (20 Marks)

#### 1. Sensor Resource & Integrity (10 Marks)

When a method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the API expects incoming data in JSON format. If a client sends data in a different format such as `text/plain` or `application/xml`, JAX-RS will not be able to process it.

As a result, the server will reject the request and return an HTTP 415 (Unsupported Media Type) error.

- API strictly expects JSON input  
- Other formats cannot be parsed  
- Request is rejected automatically  
- Returns HTTP 415 error  

---

#### 2. Filtered Retrieval & Search (10 Marks)

Filtering using query parameters (e.g., `/sensors?type=CO2`) is more appropriate than embedding filters in the path (e.g., `/sensors/type/CO2`). Query parameters are specifically designed for optional filtering and searching operations.

Path parameters, on the other hand, are meant for identifying specific resources and are less flexible for filtering.

**Query parameters:**
- Ideal for filtering and searching  
- Optional and flexible  
- Cleaner API design  

**Path parameters:**
- Used for identifying resources  
- Less suitable for filtering  
- Makes API less flexible  

---

### Part 4: Deep Nesting with Sub-Resources (20 Marks)

#### 1. The Sub-Resource Locator Pattern (10 Marks)

The sub-resource locator pattern allows splitting complex API structures into multiple smaller resource classes. Instead of handling all endpoints in one large class, each sub-resource is delegated to a dedicated class.

This improves maintainability and scalability, especially in large systems.

- Keeps code modular and organized  
- Improves readability and maintainability  
- Supports scalability for large APIs  
- Avoids overly complex controller classes  
- Promotes separation of concerns  

---

#### 2. Historical Data Management (10 Marks)

When a new sensor reading is added, it represents the most recent measurement from that sensor. Therefore, the `currentValue` field in the parent Sensor object must be updated accordingly.

This ensures consistency and allows clients to quickly access the latest sensor value without retrieving the full history.

- Reflects the latest sensor reading  
- Maintains consistency across the API  
- Avoids unnecessary data fetching  
- Improves performance for clients  

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging (30 Marks)

#### 2. Dependency Validation (422 Unprocessable Entity) (10 Marks)

HTTP 422 (Unprocessable Entity) is more appropriate when the request is syntactically correct but contains invalid data. For example, if a sensor is created with a non-existent room ID, the JSON structure is valid, but the reference is incorrect.

In contrast, HTTP 404 is used when the requested resource itself cannot be found.

**422:**
- Request format is valid  
- Contains invalid or inconsistent data  

**404:**
- Resource does not exist  

- 422 provides more precise error meaning  

---

#### 4. The Global Safety Net (500) (5 Marks)

Returning raw Java stack traces in API responses can expose sensitive internal details about the system. This information can be used by attackers to understand the system structure and identify vulnerabilities.

For security reasons, APIs should always return generic error messages instead of detailed internal errors.

- Reveals internal class names and structure  
- Exposes framework and implementation details  
- Helps attackers identify weaknesses  
- Increases security risks  
- Should be replaced with generic error responses  

---

#### 5. API Request & Response Logging Filters (5 Marks)

Using JAX-RS filters for logging allows handling cross-cutting concerns such as request and response logging in a centralized way. Instead of adding logging code in every resource method, filters automatically intercept all requests and responses.

This approach improves code cleanliness and maintainability.

- Centralized logging mechanism  
- Reduces code duplication  
- Keeps resource classes clean  
- Easier to maintain and update  
- Ensures consistent logging across the API  

---