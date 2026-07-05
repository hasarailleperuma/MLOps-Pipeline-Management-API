# MLOps Pipeline Management API

**Module:** 5COSC022W – Client-Server Architectures (2025/26 Referral/Deferral Coursework)
**Student:** Illeperumachchi Hasara Poornima (20241431)
**Stack:** JAX-RS (Jersey 2.41, `javax.*` namespace) · Java 11 · Apache Tomcat 9 · Maven · Jackson

---

## 1. API Overview

This project is a RESTful web service, built using JAX-RS, that allows an AI research lab to
manage **ML Workspaces** and the **Machine Learning Models** deployed inside them, along
with a historical log of **Evaluation Metrics** per model. It simulates the backend of a
cloud-native MLOps platform used by data scientists and automated pipelines.

The service follows REST principles throughout:
- **Resource-oriented URIs** — workspaces, models, and metrics are all addressed as nouns,
  not verbs.
- **Statelessness** — every request is self-contained; no session state is kept between calls.
- **Correct use of HTTP methods and status codes** — `GET`, `POST`, `DELETE`, and `HEAD`
  map directly onto CRUD-style operations, and failures return meaningful 4xx/5xx codes
  instead of generic errors or raw stack traces.
- **Resource nesting** — evaluation metrics are modelled as a genuine sub-resource of a
  model (`/models/{id}/metrics`) via a JAX-RS sub-resource locator, rather than a flat
  top-level collection.

Data is held entirely in memory using `HashMap`/`LinkedHashMap` (via a singleton
`DataStore`) — no database or ORM is used, per the coursework constraints.

### Base URL
```
http://localhost:8080/MLOpsPipelineAPI/api/v1
```

### Resource Hierarchy
```
GET    /api/v1                          → Discovery / API metadata
GET    /api/v1/workspaces               → List all workspaces
POST   /api/v1/workspaces               → Create a workspace
GET    /api/v1/workspaces/{id}          → Get a single workspace
HEAD   /api/v1/workspaces/{id}          → Check workspace existence (no body)
DELETE /api/v1/workspaces/{id}          → Delete a workspace (blocked if not empty → 409)

GET    /api/v1/models                   → List all models (optional ?status= filter)
POST   /api/v1/models                   → Register a new model (validates workspaceId)
GET    /api/v1/models/{id}              → Get a single model
DELETE /api/v1/models/{id}              → Delete a model

GET    /api/v1/models/{id}/metrics      → List evaluation history for a model
POST   /api/v1/models/{id}/metrics      → Append a metric (blocked if model DEPRECATED → 403)
```

### Core Data Models
| Class | Purpose |
|---|---|
| `MLWorkspace` | `id`, `teamName`, `storageQuotaGb`, `modelIds` |
| `MachineLearningModel` | `id`, `framework`, `status` (`TRAINING`/`DEPLOYED`/`DEPRECATED`), `latestAccuracy`, `workspaceId` |
| `EvaluationMetric` | `id`, `timestamp`, `accuracyScore` |

---

## 2. Build & Run Instructions

### Prerequisites
- Java JDK 11+
- Apache Maven 3.6+
- Apache Tomcat 9.x
- (Optional) NetBeans or IntelliJ IDEA with a Tomcat server configured

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/hasarailleperuma/MLOps-Pipeline-Management-API.git
cd MLOps-Pipeline-Management-API
```

**2. Build the WAR file**
```bash
mvn clean package
```
This produces `target/MLOpsPipelineAPI.war`.

**3. Deploy to Tomcat**
- Copy `target/MLOpsPipelineAPI.war` into your Tomcat installation's `webapps/` folder and
  start Tomcat (`bin/startup.sh` or `bin/startup.bat`), **or**
- In an IDE (NetBeans/IntelliJ): right-click the project → **Run** with a Tomcat 9 server
  configured — the IDE will auto-deploy the WAR.

**4. Verify the server is running**
```bash
curl http://localhost:8080/MLOpsPipelineAPI/api/v1
```
Expected: a `200 OK` JSON response containing API name, version, and resource links.

---

## 3. Sample curl Commands

**1. Discovery — API metadata**
```bash
curl -X GET http://localhost:8080/MLOpsPipelineAPI/api/v1
```

**2. Get all workspaces (returns `Cache-Control` header)**
```bash
curl -i -X GET http://localhost:8080/MLOpsPipelineAPI/api/v1/workspaces
```

**3. Create a new workspace**
```bash
curl -X POST http://localhost:8080/MLOpsPipelineAPI/api/v1/workspaces \
  -H "Content-Type: application/json" \
  -d '{"id":"WS-DEMO-01","teamName":"Demo Team","storageQuotaGb":100}'
```

**4. Register a model against an existing workspace (server generates the `id`)**
```bash
curl -X POST http://localhost:8080/MLOpsPipelineAPI/api/v1/models \
  -H "Content-Type: application/json" \
  -d '{"framework":"TensorFlow","status":"TRAINING","latestAccuracy":0.80,"workspaceId":"WS-VISION-01"}'
```

**5. Filter models by status**
```bash
curl -X GET "http://localhost:8080/MLOpsPipelineAPI/api/v1/models?status=DEPLOYED"
```

**6. Post an evaluation metric to a model (updates parent `latestAccuracy`)**
```bash
curl -X POST http://localhost:8080/MLOpsPipelineAPI/api/v1/models/MOD-8832/metrics \
  -H "Content-Type: application/json" \
  -d '{"accuracyScore":0.95}'
```

**7. Check workspace existence without downloading the body**
```bash
curl -I http://localhost:8080/MLOpsPipelineAPI/api/v1/workspaces/WS-VISION-01
```

**8. Attempt to delete a non-empty workspace (expect `409 Conflict`)**
```bash
curl -i -X DELETE http://localhost:8080/MLOpsPipelineAPI/api/v1/workspaces/WS-VISION-01
```

**9. Register a model with a non-existent workspace (expect `422 Unprocessable Entity`)**
```bash
curl -i -X POST http://localhost:8080/MLOpsPipelineAPI/api/v1/models \
  -H "Content-Type: application/json" \
  -d '{"framework":"TensorFlow","workspaceId":"INVALID"}'
```

**10. Post a metric to a DEPRECATED model (expect `403 Forbidden`)**
```bash
curl -i -X POST http://localhost:8080/MLOpsPipelineAPI/api/v1/models/MOD-DEPR-01/metrics \
  -H "Content-Type: application/json" \
  -d '{"accuracyScore":0.90}'
```

---

## 4. Report — Answers to Coursework Questions

### Part 1 – Service Architecture & Setup

**Q1.1 — Explain the role of a `MessageBodyWriter` or a JSON provider (like Jackson) in serialisation.**

JAX-RS does not convert Java objects to JSON by itself — that job is delegated to a pluggable
component called a `MessageBodyWriter`. When a resource method (e.g. in `WorkspaceResource`)
returns a POJO such as `MLWorkspace`, the runtime matches the object's runtime type and the
method's `@Produces(MediaType.APPLICATION_JSON)` annotation against the registered
providers. In this project, Jersey's `jersey-media-json-jackson` module registers Jackson's
`JacksonJsonProvider`, which implements `MessageBodyWriter<Object>`. Its `writeTo()` method
uses reflection over the object's getters (`getId()`, `getTeamName()`, `getStorageQuotaGb()`,
etc.) to build the JSON body and stream it to the response, including nested fields like the
`modelIds` list. The reverse also applies: Jackson supplies a `MessageBodyReader` that
deserialises an incoming JSON request body into a Java object (e.g. the `MLWorkspace`
parameter in `createWorkspace()`), so resource classes never touch JSON parsing directly —
they only ever work with plain Java objects.

**Q1.2 — Define statelessness and explain why it helps cloud APIs scale horizontally.**

Statelessness means the server keeps no memory of previous requests from a client; every
request must carry all the information needed to process it (identifiers, parameters, any
required credentials). Because no server instance holds session data tied to a specific
client, a load balancer can route any incoming request to *any* available server node —
there is no need for "sticky sessions" or session replication across machines. This is why
cloud platforms can scale horizontally: new, blank server instances can be spun up (e.g. via
Kubernetes) and immediately start serving traffic, since there's no warm-up or state sync
required. It also improves fault tolerance — if one instance fails mid-request, the client can
simply retry, and any other instance can service that retried request identically.

---

### Part 2 – Workspace Management

**Q2.1 — Discuss how `Cache-Control` headers on `GET /workspaces` improve performance.**

`WorkspaceResource.getAllWorkspaces()` attaches a `Cache-Control: max-age=60,
must-revalidate` header to its response. This tells the client (or any intermediate proxy/CDN)
that it may reuse the cached response for up to 60 seconds without contacting the server again.
Since workspace data changes far less frequently than the polling rate of dashboards or MLOps
pipelines, this avoids redundant network round-trips for the client and skips the cost of
re-running the in-memory lookup and Jackson serialisation on the server for every duplicate
request. `must-revalidate` ensures that once the 60 seconds expire, the client re-checks with
the server rather than silently using stale data indefinitely — balancing performance against
freshness.

**Q2.2 — Which HTTP method should a client use to check if a workspace exists without downloading the body?**

`HEAD`. It is implemented in `WorkspaceResource` as
`HEAD /api/v1/workspaces/{workspaceId}`. `HEAD` performs the exact same routing and
lookup as `GET` — returning `200 OK` if the workspace exists in the in-memory map or
`404 Not Found` if it doesn't — but the JAX-RS runtime never serialises or sends a response
body. This lets a client (or an automated pipeline polling before a batch upload) confirm
existence and read response headers cheaply, without the bandwidth and parsing cost of a full
JSON payload.

---

### Part 3 – Model Operations & Linking

**Q3.1 — Discuss the security and data integrity reasons for generating model IDs server-side.**

If a client were allowed to supply its own `id`, a malicious or careless client could submit an
ID that collides with an existing model, silently overwriting it — an Insecure Direct Object
Reference-style risk. Predictable, client-chosen IDs are also easier to guess or enumerate,
letting a client probe for models belonging to other workspaces. Because the server is the
single source of truth for the model collection, it is the server's responsibility to guarantee
uniqueness; `ModelResource.createModel()` generates the ID as
`"MOD-" + UUID.randomUUID()...`, giving an astronomically low chance of collision
(roughly 1 in 2^122 for the truncated form used here) regardless of how many clients are
creating models concurrently. This removes an entire class of race conditions where two
clients might otherwise submit the same ID at nearly the same time.

**Q3.2 — How must a client encode a URL containing spaces and special characters (e.g. `?framework=Scikit Learn & Tools`)?**

The value must be percent-encoded before being placed in the query string, e.g.
`?framework=Scikit%20Learn%20%26%20Tools` (space → `%20`, `&` → `%26`). This is
necessary because certain characters have reserved, structural meaning in a URI — `&`
delimits separate query parameters, and a literal space is not a legal URI character at all. If
`framework=Scikit Learn & Tools` were sent unencoded, both the HTTP client and the JAX-RS
server would parse it as two separate parameters (`framework=Scikit Learn` and a stray
`Tools`) instead of one intended value, corrupting the request. Encoding functions such as
`encodeURIComponent()` (JavaScript) or `java.net.URLEncoder.encode()` (Java) handle this
automatically, and JAX-RS's `@QueryParam("framework")` decodes the value back to its
original form on arrival.

---

### Part 4 – Deep Nesting with Sub-Resources

**Q4.1 — What is the benefit of class-level `@Produces`, and how does method-level overriding work?**

`EvaluationMetricResource` (and the other resource classes) declare
`@Produces(MediaType.APPLICATION_JSON)` once at the class level, which becomes the default
media type for every method inside that class — following the DRY principle. Since nearly
every endpoint in this API returns JSON, this avoids repeating the annotation on `getMetrics()`
and `addMetric()` individually, and reduces the risk of forgetting it on a new method (which
would break content negotiation). If a specific method needs to return something else — for
example, a future `GET /metrics/export` endpoint returning CSV — it can carry its own
`@Produces("text/csv")` annotation, and JAX-RS's "most specific annotation wins" rule means
that method-level declaration overrides the class-level default *for that method only*; every
other method in the class still falls back to the inherited JSON default.

---

### Part 5 – Advanced Error Handling, Exception Mapping & Logging

**Q5.1 — Resource Conflict (409): Why does deleting a non-empty workspace return 409?**

Deleting `WS-VISION-01` while it still owns models is not malformed or unauthorized — the
request is syntactically valid and the workspace exists — but performing it would leave models
pointing at a workspace that no longer exists, breaking referential integrity in the in-memory
store. `409 Conflict` is the correct status because it signals that the request conflicts with the
*current state* of the resource, not that the request itself was wrong. `WorkspaceResource`
throws a custom `WorkspaceNotEmptyException` when this rule is violated, and
`WorkspaceNotEmptyExceptionMapper` converts it into a `409` response with a JSON body
listing which models are still attached.

**Q5.2 — Why must a validation failure from a non-existent `workspaceId` return a 4xx, not a 5xx?**

4xx codes signal that the *client* is responsible for the failure — the request reached the
server and was processed correctly, but the client provided invalid data. 5xx codes signal that
the *server* failed unexpectedly. When `ModelResource.createModel()` receives a
`workspaceId` that isn't in the store, the server behaved exactly as designed: it looked the ID
up and correctly determined it didn't exist. Returning a `500` here would be misleading — it
would imply a server bug when there is none — and many clients/pipelines automatically retry
on 5xx responses, assuming the failure is transient. Retrying an identical request with the
same invalid `workspaceId` would fail forever, wasting resources. `LinkedWorkspaceNotFoundException`
is therefore mapped to `422 Unprocessable Entity`, telling the client clearly that *its* payload
needs correcting before retrying.

**Q5.3 — State Constraint (403): Why is 403 correct for posting a metric to a DEPRECATED model?**

`403 Forbidden` communicates that the server understood the request perfectly but is
deliberately and permanently refusing to act on it, regardless of who sends it or how many
times it's retried — no authentication or retry will make a `DEPRECATED` model start
accepting metrics again, because that's a business rule, not a transient condition.
`EvaluationMetricResource.addMetric()` throws `ModelDeprecatedException` whenever a
`POST` targets a model whose `status` is `DEPRECATED`, and
`ModelDeprecatedExceptionMapper` converts this into a `403` with a message explaining that
the model has been retired from monitoring.

**Q5.4 — How does JAX-RS decide between a specific `ExceptionMapper` and the global `ExceptionMapper<Throwable>`?**

JAX-RS uses a **most-specific-type-wins** resolution strategy. When an exception is thrown, the
runtime walks up the exception's class hierarchy from its exact runtime type, looking at each
level for a registered `ExceptionMapper<T>` where `T` matches. Because
`LinkedWorkspaceNotFoundExceptionMapper` is registered directly for
`LinkedWorkspaceNotFoundException`, it is found immediately and used — it is a strictly
closer match than `GlobalExceptionMapper`, which is only registered for `Throwable`, at the
very top of the hierarchy. The global mapper therefore only ever fires as a last resort, for
exception types with no dedicated mapper — genuinely unexpected bugs like a
`NullPointerException` — which is exactly what keeps the API "leak-proof": every anticipated
failure gets its own descriptive 4xx response, while truly unexpected errors are caught and
sanitised into a generic `500` rather than leaking a raw stack trace to the client.

**Q5.5 — List two pieces of crucial HTTP metadata available from the filter contexts.**

From `ContainerRequestContext` in `LoggingFilter`: (1) `getMethod()` and
`getUriInfo().getRequestUri()` — together these show exactly which HTTP verb and full URL
(including query parameters like `?status=DEPLOYED`) a client used, which is essential for
reproducing a reported bug; and (2) `getHeaders()`, which exposes request headers such as
`Content-Type` and `User-Agent`, useful for identifying which client or pipeline made a
malformed request.

From `ContainerResponseContext`: (1) `getStatus()` — logging the final status code for every
outgoing response makes it possible to spot trends, such as a sudden spike in `409` or `422`
responses, that might indicate a systemic issue upstream; and (2) `getHeaders()` on the
response, which can confirm whether framework-level behaviour is correct — for example,
verifying that `Cache-Control` is actually present on `GET /workspaces` responses, or that a
`Location` header was set correctly on a `201 Created` response.

---

## 5. Notes on Design Choices

- **In-memory storage only.** A singleton `DataStore` backed by `LinkedHashMap` is used
  throughout, per the coursework constraint against SQL/database technology. Data does not
  persist across server restarts — this is expected and intentional for this assignment.
- **JAX-RS only.** No Spring or other frameworks are used; only `javax.ws.rs.*` and Jersey as
  the JAX-RS implementation.
- **Exception mapping is layered**, not monolithic: each anticipated business-rule violation
  (non-empty workspace, missing linked workspace, deprecated model, missing resource) has
  its own exception class and its own `ExceptionMapper`, with a single catch-all
  `ExceptionMapper<Throwable>` as the final safety net.

---

## References

- Oracle / Eclipse Foundation — *Jakarta RESTful Web Services (JAX-RS) Specification*.
- FasterXML — *Jackson Documentation for Processing JSON*.
- Fielding, R. (2000) — *Architectural Styles and the Design of Network-based Software Architectures*, Chapter 5.
- MDN Web Docs — *HTTP Response Status Codes* and *HTTP Cache-Control*.

**Repository:** https://github.com/hasarailleperuma/MLOps-Pipeline-Management-API
