# Smart Campus — Sensor & Room Management API

> **Module:** 5COSC022W — Client-Server Architectures (2025/26)  
> **Weight:** 60% of final grade | **Due:** 24 April 2026, 13:00  
> **Technology:** JAX-RS (Jersey 2.41) deployed on Apache Tomcat 10

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

## Section 4: Conceptual Report

---

### Part 1.1 — JAX-RS Resource Lifecycle

By default, Jersey creates a **new instance of every resource class for each incoming HTTP request**. This per-request scope is the JAX-RS specification default and exists so that resource classes can safely store request-scoped state (such as injected `@Context` objects) in instance fields without synchronisation. The implication is that any mutable instance-level field is reset on every request: if a resource class held a `List` as an instance variable, it would be empty on every request and changes would never persist across calls.

This is exactly why our three store classes — `RoomStore`, `SensorStore`, and `SensorReadingStore` — are **application-scoped singletons** implemented with the classic `private static final INSTANCE` pattern. Because the JVM initialises `static final` fields exactly once (at class-load time) in a thread-safe manner, every resource instance — across every request thread — shares the same store object. The stores use `ConcurrentHashMap` for the top-level key-value mapping, which provides segment-level locking so concurrent reads and writes do not corrupt the map's internal structure. `SensorReadingStore` additionally uses `CopyOnWriteArrayList` for the per-sensor reading lists, which is optimised for read-heavy, append-occasionally workloads: reads proceed without any locking because they operate on an immutable snapshot, while writes copy the underlying array atomically.

---

### Part 1.2 — HATEOAS Benefits

HATEOAS (Hypermedia As The Engine Of Application State) is the REST constraint that requires a server to embed navigational links in its responses so that clients can discover available actions at runtime rather than relying on out-of-band documentation compiled into their source code.

The primary benefit is **reduced client-server coupling**. A client that discovers the `rooms` href from the `/api/v1/` discovery endpoint does not need to hardcode `/api/v1/rooms`. If the path changes in a future API version, only the discovery response changes — clients that follow links dynamically adapt without a code release. This **evolvability** is critical in long-lived systems where URI schemes often change.

A second benefit is **discoverability**: a developer or an automated agent can start at the root and traverse the entire API surface without prior knowledge. Our `DiscoveryResource` exposes each resource's `href`, supported `methods`, and optional `queryParams`, providing a machine-readable contract equivalent to a lightweight API index. This also enables runtime feature detection — a client can check whether a `DELETE` method is listed before attempting one, rather than catching a 405 at runtime.

Finally, HATEOAS aligns with the web's own architecture. The success of HTML hyperlinks as an evolvable navigation model for billions of clients is precisely the pattern REST attempts to replicate for machine-to-machine communication.

---

### Part 2.1 — IDs vs Full Objects in a Collection Response

When designing a `GET /rooms` endpoint, there is a fundamental tension between two extremes. Returning **only IDs** (`["LIB-301","LAB-101"]`) minimises payload size but forces the client into an **N+1 request problem**: to display any useful information, it must issue a separate `GET /rooms/{id}` for every element — N rooms → N+1 round trips, each with its own network latency and server overhead.

Returning **full objects** (as our API currently does) solves N+1 but risks **over-fetching**: if the client only needs room names for a dropdown, it still receives capacity and full sensor ID lists for every room. On high-cardinality collections (thousands of rooms), this wastes bandwidth and parse time.

Production APIs typically resolve this with one or more middle-ground strategies. **Pagination** (`?page=0&size=20`) caps response size and is essential for unbounded collections. **Field selection** (`?fields=id,name`) lets the client declare exactly what it needs, reducing payload without multiple round trips — a pattern popularised by GraphQL and supported by many REST APIs. **Embedded vs linked resources** (a HATEOAS concept) allows a response to include a lightweight summary inline while providing a `href` for the full representation, giving clients the choice of whether to follow the link.

---

### Part 2.2 — DELETE Idempotency

Our `DELETE /rooms/{roomId}` endpoint is **idempotent** in the sense defined by RFC 7231: making the same request multiple times produces the same **server state** as making it once. After the first successful call (which returns `204 No Content`), the room is gone. Subsequent identical calls find no room to delete and return `404 Not Found`. The server state after each subsequent call is identical — the room remains absent — so the idempotency property holds.

It is important to distinguish **idempotency** from **safety**. A safe method (GET, HEAD) has no side effects on server state. DELETE is not safe — the first call does mutate state. Idempotency only requires that repeating the operation beyond the first time produces no additional state change.

The fact that the HTTP **status code** differs between the first call (`204`) and subsequent calls (`404`) is explicitly permitted by RFC 7231, which states that idempotency concerns server state, not response representation. A client implementing retry logic (e.g., after a network timeout where it cannot determine whether the first DELETE was received) can safely resend the request — at worst it receives a 404, which confirms the resource is gone, which is precisely the intended outcome.

---

### Part 3.1 — @Consumes Mismatch Behaviour

When a JAX-RS resource method is annotated with `@Consumes(MediaType.APPLICATION_JSON)` and a client sends a request with a `Content-Type` header of `text/plain` or `application/xml`, Jersey performs **content negotiation** during request matching and determines that no method can consume the supplied media type. It responds automatically with **HTTP 415 Unsupported Media Type** — the resource method is never invoked, and no application code runs.

The negotiation chain works as follows. Jersey first matches the request path to a set of candidate resource methods. It then filters those candidates by the request's `Content-Type` against each method's `@Consumes` declaration. If no candidate survives this filter, Jersey constructs the 415 response internally and hands it directly to the response pipeline, bypassing the method body entirely.

This behaviour is desirable: it means the resource method can trust that `sensor` (the deserialized body parameter) will never be `null` due to a content-type mismatch — Jackson is only invoked when the contract is satisfied. Any remaining null risk comes from the client sending `application/json` with an empty body or a JSON `null` literal, which is a separate validation concern handled by our explicit null guard.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

When filtering a collection by an attribute — for example, returning only sensors of type `Temperature` — the choice between `/api/v1/sensors?type=Temperature` and `/api/v1/sensors/Temperature` is an important REST design decision.

**Query parameters** are the correct choice for filters because they represent a **view predicate over a resource**, not a distinct resource identity. `/api/v1/sensors` is the collection resource; `?type=Temperature` is an instruction to narrow the view of that collection. The URI `/api/v1/sensors/Temperature` would imply that `Temperature` is a child resource of `sensors` — a different resource altogether — which is semantically incorrect and would require a dedicated route handler that duplicates the list logic.

Query parameters also **compose naturally**: `?type=CO2&status=ACTIVE` combines two predicates without creating an exponential number of route definitions. They are **optional by contract** — omitting them returns the full unfiltered collection, which is the correct default behaviour. They are also **cache-friendly**: HTTP caches and CDNs treat the query string as part of the cache key, so `?type=Temperature` and `?type=CO2` are cached independently without any additional configuration.

Finally, this pattern aligns with established conventions (RFC 3986, OData, the GitHub and Stripe REST APIs) and will be immediately familiar to API consumers.

---

### Part 4.1 — Sub-Resource Locator Benefits

The sub-resource locator pattern — where a root resource method returns an instance of another class without an HTTP verb annotation — provides several concrete advantages over placing all endpoint logic in a single large resource class.

**Single responsibility**: `SensorResource` handles sensor identity operations (create, retrieve, filter). `SensorReadingResource` handles reading operations. Neither class needs to know about the other's internal logic, making each easier to read, test, and modify independently.

**Enforced hierarchy at the routing layer**: Readings only make sense in the context of a sensor. The locator method in `SensorResource` verifies the sensor exists and throws `SensorNotFoundException` before Jersey even constructs a `SensorReadingResource`. This validation cannot be bypassed by crafting a different URL — it is structurally enforced.

**Testability**: `SensorReadingResource` can be unit-tested by constructing it directly with a known `sensorId`, without needing to stand up the full JAX-RS runtime or mock routing.

**Dynamic dispatch**: The locator could inspect the sensor's type or status and return a *different* subclass of `SensorReadingResource` — for example, a read-only variant for OFFLINE sensors — without any change to the client-facing URI.

**Avoidance of registration**: Sub-resources instantiated by locators must not be registered as root resources in `Application.getClasses()`. This prevents Jersey from treating `SensorReadingResource` as independently routable at `/readings`, which would violate the intended access model.

---

### Part 5.2 — Why 422 Over 404 for Invalid roomId

`HTTP 404 Not Found` means the **URI** the client addressed does not resolve to any resource on the server. When a client posts to `POST /api/v1/sensors`, that URI is valid and fully routable — Jersey finds `SensorResource.create()` and invokes it without issue. The problem is not with the URI; it is with the **semantic content of the request body**: the `roomId` field references an entity that does not exist.

Returning `404` in this situation would be misleading in two ways. First, it conflates a routing failure with a data validation failure — a client implementing retry logic on 404 responses (a common pattern for eventually-consistent systems) might unnecessarily retry a request that will never succeed regardless of timing. Second, it makes it impossible for the client to distinguish "you requested a URI that doesn't exist" from "your payload references a foreign key that doesn't exist."

`HTTP 422 Unprocessable Entity` (defined in RFC 4918, adopted in RFC 9110) exists precisely for this case: the request is syntactically well-formed JSON, the endpoint exists, but the server cannot process the payload because it is semantically invalid. Clients can reliably branch on 422 to surface a specific validation error (e.g., "Room not found") to the user, without conflating it with either routing errors (404) or generic server failures (500).

---

### Part 5.4 — Stack Trace Security Risks

Exposing raw stack traces or exception detail in HTTP error responses constitutes an **information disclosure vulnerability**, catalogued under OWASP Top 10 A05:2021 (Security Misconfiguration) and A09:2021 (Security Logging and Monitoring Failures).

A stack trace leaks several categories of sensitive information that an attacker can exploit directly. **Framework and library versions** (e.g., `org.glassfish.jersey 2.41`) enable targeted lookup of known CVEs against those exact versions. **Internal package structure** (`com.westminster.smartcampus.store.RoomStore`) reveals the application's architecture and aids in constructing targeted payloads. **File system paths** embedded in class-loading errors can hint at directory structures useful for path traversal attacks. **Database driver class names** (if present) reveal the persistence technology and version. **Line numbers** pinpoint exactly which line of code failed, allowing an attacker to craft inputs that reproducibly trigger the same exception — invaluable when probing for deserialization gadget chains or injection points.

Our `GenericThrowableMapper` addresses this by logging the full exception server-side (with a `requestId` for correlation) while returning only a safe, opaque message to the client. The `requestId` allows support teams to retrieve the full diagnostic information from server logs without exposing it externally.

---

### Part 5.5 — Filters vs Inline Logger Calls

Placing logging statements inside individual resource methods is a violation of the **Don't Repeat Yourself (DRY)** principle and introduces several operational risks that a `ContainerRequestFilter` / `ContainerResponseFilter` pair eliminates entirely.

**Coverage guarantee**: A filter registered in the JAX-RS application is invoked for every request that reaches the runtime, including requests to endpoints added in the future. Inline logging requires a developer to remember to add the log call to every new method — a requirement that is invisible at code-review time and silently fails when forgotten.

**Consistency**: Centralised logging enforces a uniform log format (`--> METHOD URI` / `<-- STATUS METHOD URI [requestId] (elapsed ms)`) across all endpoints. Consistent formatting is essential for log aggregation pipelines (ELK Stack, Splunk, CloudWatch Logs Insights) where queries rely on predictable field positions. Inconsistent formats produce silent query failures that appear as missing data.

**Separation of concerns**: Resource methods should express business logic — validation, store interaction, response construction. Logging is a cross-cutting infrastructure concern. Mixing the two violates the Single Responsibility Principle and makes resource methods harder to read and test.

**Centrally configurable**: A single filter class can be toggled off (by removing it from `getClasses()`), redirected to a different logging backend, or replaced with a structured JSON logger without touching any resource code. The same change applied inline would require editing every resource method.

This is the same rationale behind Aspect-Oriented Programming (AOP) and servlet filters in the Java EE world — cross-cutting concerns belong in interceptors, not business logic.

---

## Section 5: Project Structure

```
smartcampus/
│
├── pom.xml                            Maven build — WAR packaging, Jersey 2.41 deps
│
└── src/main/
    ├── webapp/
    │   ├── META-INF/context.xml       Tomcat context descriptor
    │   └── WEB-INF/web.xml            Servlet mapping: Jersey ServletContainer → /api/v1/*
    │
    └── java/com/westminster/smartcampus/
        │
        ├── RestApplication.java       JAX-RS Application subclass — explicit class registry
        │                              (@ApplicationPath("/api/v1") registered in web.xml)
        │
        ├── model/
        │   ├── Room.java              POJO: id, name, capacity, sensorIds
        │   ├── Sensor.java            POJO: id, type, status, currentValue, roomId
        │   └── SensorReading.java     POJO: id, timestamp, value
        │
        ├── store/
        │   ├── RoomStore.java         Singleton ConcurrentHashMap<String, Room>
        │   ├── SensorStore.java       Singleton ConcurrentHashMap<String, Sensor>
        │   └── SensorReadingStore.java  Singleton ConcurrentHashMap<String, CopyOnWriteArrayList<SensorReading>>
        │
        ├── resource/
        │   ├── DiscoveryResource.java    GET /api/v1/ — HATEOAS discovery endpoint
        │   ├── SensorRoomResource.java   GET|POST /rooms, GET|DELETE /rooms/{roomId}
        │   ├── SensorResource.java       GET|POST /sensors, GET /sensors/{id}, sub-resource locator
        │   └── SensorReadingResource.java  GET|POST /sensors/{sensorId}/readings (sub-resource, not root)
        │
        ├── exception/
        │   ├── RoomNotFoundException.java
        │   ├── RoomNotEmptyException.java
        │   ├── SensorNotFoundException.java
        │   ├── SensorUnavailableException.java
        │   └── LinkedResourceNotFoundException.java
        │
        ├── mapper/
        │   ├── RoomNotFoundExceptionMapper.java          → 404 NOT_FOUND
        │   ├── SensorNotFoundExceptionMapper.java        → 404 NOT_FOUND
        │   ├── RoomNotEmptyExceptionMapper.java          → 409 ROOM_NOT_EMPTY
        │   ├── LinkedResourceNotFoundExceptionMapper.java → 422 LINKED_RESOURCE_NOT_FOUND
        │   ├── SensorUnavailableExceptionMapper.java     → 403 SENSOR_UNAVAILABLE
        │   ├── NotFoundMapper.java                       → 404 for unmatched JAX-RS routes
        │   └── GenericThrowableMapper.java               → 500 catch-all, logs full trace server-side
        │
        └── filter/
            └── LoggingFilter.java     ContainerRequestFilter + ContainerResponseFilter;
                                       logs method, URI, status, and elapsed time per request
```
