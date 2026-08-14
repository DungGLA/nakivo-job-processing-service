# Job Processing Service

A Spring Boot application for creating, searching, and asynchronously processing jobs.

The service provides RESTful APIs to manage jobs and supports batch processing with asynchronous event-driven execution and retry handling.

## ✨ Features

* RESTful API for creating jobs
* Get job details by ID
* Search jobs by status with pagination
* Process pending jobs

---

## 🏗️ Project Structure

The project follows a **feature-based package architecture**:

```text
com.nakivo.job_processing
├── common
│   ├── constants
│   ├── exception
│   └── ...
│
└── job
    ├── controller
    ├── service
    ├── repository
    ├── entity
    ├── dto
    └── ...
```

### Common Package

Contains shared components used across the application:

* Constants
* Shared models
* Exception handling

### Job Package

Contains all components related to job creation, searching, and processing.

---

# 📋 API

## 1. Create Job

### `POST /api/jobs`

Creates a new job with `PENDING` status.

### Request Body

```json
{
  "type": "EMAIL",
  "payload": {
    "recipient": "test@example.com",
    "subject": "Hello",
    "body": "Test message 234",
    "fail": true
  }
}
```

### Request Fields

| Field     | Type        | Required | Description          |
| --------- | ----------- | -------- | -------------------- |
| `type`    | String      | Yes      | Job type             |
| `payload` | JSON Object | Yes      | Job-specific payload |

### Response

```json
{
  "jobId": 1
}
```

---

## 2. Get Job by ID

### `GET /api/jobs/{id}`

Returns the details of a specific job.

### Path Parameters

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `id`      | Long | Job ID      |

### Response

```json
{
  "id": 10,
  "type": "EMAIL",
  "status": "COMPLETED",
  "payload": {
    "body": "Test message 234",
    "subject": "Hello",
    "recipient": "test@example.com"
  },
  "errorMessage": null,
  "retryCount": 0
}
```

### Error Response

Returns `404 Not Found` when the specified job does not exist.

---

## 3. Search Jobs

### `GET /api/jobs`

Searches jobs by status with pagination.

### Query Parameters

| Parameter | Type    | Required | Default | Description             |
| --------- | ------- | -------- | ------- | ----------------------- |
| `status`  | String  | Yes      | -       | Job status              |
| `page`    | Integer | No       | `0`     | Page number             |
| `size`    | Integer | No       | `10`    | Number of jobs per page |

### Example Request

```http
GET /api/jobs?page=0&size=10&status=PENDING
```

### Response

```json
{
  "content": [
    {
      "id": 20,
      "type": "EMAIL",
      "status": "PENDING",
      "payload": {
        "body": "Test message 234",
        "fail": true,
        "subject": "Hello",
        "recipient": "test@example.com"
      },
      "errorMessage": null,
      "retryCount": 0
    },
    {
      "id": 21,
      "type": "EMAIL",
      "status": "PENDING",
      "payload": {
        "body": "Test message 234",
        "fail": true,
        "subject": "Hello",
        "recipient": "test@example.com"
      },
      "errorMessage": null,
      "retryCount": 0
    }
  ],
  "page": 0,
  "size": 10,
  "totalPages": 1,
  "totalElements": 2
}
```

---

## 4. Process Jobs

### `POST /api/jobs/process`

Triggers process pending jobs.

### Processing Flow

When the API is called, the service:

1. Queries the database for `PENDING` jobs in batches by using:
    ```SELECT ...  FOR UPDATE SKIP LOCKED;```
2. Locks the selected jobs to prevent concurrent processing requests from claiming the same jobs.
3. Updates the selected jobs from `PENDING` to `PROCESSING`.
4. Publishes a processing event for each job.
5. Repeats the process from step 1 until no more PENDING jobs can be claimed.
6. An asynchronous event listener receives the event.
7. The listener processes the job.
8. If processing succeeds:

  * Update status to `COMPLETED`.
9. If processing fails:

  * Increment `retryCount`.
  * Retry the job by publishing another processing event.
10. If the retry count exceeds the maximum retry limit:

  * Update status to `FAILED`.
  * Store the error message.

Because job processing is asynchronous, the API returns immediately after the processing events are published.

### Job Status Flow

```text
                 ┌──────────────┐
                 │    PENDING   │
                 └──────┬───────┘
                        │
                        │ Process
                        ▼
                 ┌──────────────┐
                 │  PROCESSING  │
                 └──────┬───────┘
                        │
                ┌───────┴────────┐
                │                │
             Success           Failure
                │                │
                ▼                ▼
        ┌──────────────┐   ┌──────────────┐
        │  COMPLETED   │   │ Retry Count  │
        └──────────────┘   │    + 1       │
                           └──────┬───────┘
                                  │
                         ┌────────┴────────┐
                         │                 │
                    Retry available   Max retries reached
                         │                 │
                         ▼                 ▼
                    PROCESSING         FAILED
```

---

# 🗄️ Database

The application uses an **H2 in-memory database**.

## Job Table

| Column          | Type      | Description                         |
| --------------- | --------- | ----------------------------------- |
| `id`            | Long      | Unique job identifier               |
| `status`        | VARCHAR   | Job status                          |
| `type`          | VARCHAR   | Job type                            |
| `payload`       | JSON      | Job-specific payload                |
| `created_at`    | TIMESTAMP | Creation timestamp                  |
| `updated_at`    | TIMESTAMP | Last update timestamp               |
| `retry_count`   | INT       | Number of processing retries        |
| `error_message` | VARCHAR   | Error message when processing fails |

### Job Statuses

```text
PENDING
PROCESSING
COMPLETED
FAILED
```

---

# 🔄 Processing Architecture

The job processing flow is designed to support **asynchronous execution and concurrent API calls**.

```text
                  ┌─────────────────────┐
                  │  POST /api/jobs/    │
                  │      process        │
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │   Query PENDING     │
                  │       Jobs          │
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │ Lock + Update Jobs  │
                  │   → PROCESSING      │
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │ Publish Job Event   │
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │ Async Event Listener│
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │    Process Job      │
                  └──────────┬──────────┘
                             │
                  ┌──────────┴──────────┐
                  │                     │
               Success                Failure
                  │                     │
                  ▼                     ▼
             COMPLETED            retryCount++
                                        │
                              ┌─────────┴─────────┐
                              │                   │
                         Retry available      Max retries
                              │                   │
                              ▼                   ▼
                         Process again          FAILED
```

---

# Question A - System Design

Suppose this service needs to support 1 million jobs per day and multiple application instances running in parallel. How would you improve the current design for production use?
In your answer, please explain the proposed architecture, data flow, failure handling, duplicate processing prevention, scaling approach, and operational considerations.

## Approach
- To support 1 million jobs per day (~12 jobs/s) and multiple application instances, I would enhance my current design as follows:
  + Using Kafka instead of in-memory event publishing to handle high throughput and provide durability and don't need to manage the thread pool size
    or queue capacity like using @Async. 
    - Scale consumers horizontally based on Kafka lag.
    - Support retry for better error handling event.
    - Allow multiple consumers to process jobs in parallel.
  + Using outbox pattern to make sure event message doesn't lost in case of application crash, instead of publish event after commit transaction like current design.
  + Add composite index in the job table for any api need to select job data to improve query performance in case the data increase day by day.
  + Add API rate limiting:
    - Configure rate limits at the API Gateway.
    - Protect create/process APIs from excessive traffic and prevent
      downstream resource exhaustion.
  + The consumer should be had a retry mechanism with exponential backoff to handle transient failures. If a job fails after the maximum number of retries, it should be moved to a dead-letter queue for further investigation.
  + Expose a background job to handle the PROCESSING jobs (updated_at < now() - 5 minutes) in case consumer reach max retry and send to dlq.
  + Setup Kafka monitor to check lag, throughput and DB monitor to check CPU, memory, connection pool,...

Call API, update job status to PROCESSING and response flow.
```text
                    POST /process
                           │
                           ▼
                 Application Instance
                           │
                           ▼
                ┌─────────────────────┐
                │ Claim Batch         │
                │                     │
                │ SELECT PENDING      │
                │ FOR UPDATE          │
                │ SKIP LOCKED         │
                │                     │
                │ PENDING → PROCESSING│
                │ Create Outbox Event │
                └──────────┬──────────┘
                           │
                         COMMIT
                           │
                           ▼
                     Next Batch
                           │
                     until empty
                           │
                           ▼
                     Return response
```

Asynchronous Job Processing Flow
```text
                     PostgreSQL
                         │
                         │ Outbox Event
                         ▼
                  ┌───────────────┐
                  │ Outbox Worker │
                  └───────┬───────┘
                          │
                    Publish Event
                          │
                          ▼
                     ┌────────┐
                     │ Kafka  │
                     └────┬───┘
                          │
             ┌────────────┼────────────┐
             │            │            │
             ▼            ▼            ▼
         Consumer A   Consumer B   Consumer C
             │            │            │
             └────────────┼────────────┘
                          ▼
                    Process Job
                          │
                    ┌─────┴─────┐
                    │           │
                  Success      Fail
                    │           │
                    ▼           ▼
                COMPLETED    Retry Logic

```

---

# Question B - Database Performance

The `jobs` table has 50 million records. GET /api/jobs?status=PENDING&page=0&size=20 becomes slow. How would you investigate the issue and improve the performance?
In your answer, please explain your investigation steps, likely bottlenecks, possible database/query changes, and any trade-offs.


## Investigation Approach

```
SELECT ...
FROM job
WHERE status = 'PENDING'
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

- First, I would not assume this is a database performance issue.
- I would first identify the scope of the problem:
  + When the issue started
  + Check whether all APIs are affected or only this endpoint.
  + Check whether it only happens in production.
  + Check whether there was a recent deployment, configuration, or infrastructure change.
- Case 1: All APIs are slow => investigate shared resources such as CPU, memory, database, network, or infrastructure.
  + Check metrics and logs using Prometheus or Grafana such as CPU, memory, JVM GC, thread pools, database connection pool, and database CPU
- Case 2: Only this endpoint is slow => focus on its specific business flow of the API.
  + Check tracing log (like OpenTelemetry) to break down the response time and identify which span is actually slow.
  + If the database query step takes slow: (Assume using Postgres)
    - Run EXPLAIN ANALYZE to check whether PostgresSQL is doing a sequential scan, how many rows it scans versus returns, whether there is an expensive sort.
    - If it is sequential scan => create an composite index (status, created_at DESC) to support the query. 
    ```(Trace off: This will speed up the query but will slow down inserts and updates.)```
    - If the index is already used but the query is still slow => investigate whether table or index bloat is causing excessive page reads. 
Jobs table is frequently updated from PENDING to PROCESSING to COMPLETED or FAILED, PostgreSQL can accumulate dead tuples due to MVCC. 
Check pg_stat_user_tables for dead tuples and autovacuum activity, and check table and index sizes. 
Then, also verify whether autovacuum is keeping up with the update rate. 
If vacuum is not keeping up, investigate and consider update autovacuum for this table. For example using a lower vacuum scale factor or threshold.
```(Trace-off: This will increase the frequency of vacuuming, which can impact performance during vacuum runs, but will reduce bloat and improve query performance.)```



# 🚀 Getting Started

## Prerequisites

Make sure the following are installed:

* Java
* Maven

## Clone the Repository

```bash
git clone https://github.com/DungGLA/nakivo-job-processing-service.git

cd nakivo-job-processing-service
```

## Build the Project

```bash
mvn clean install
```

## Run the Application

```bash
mvn spring-boot:run
```

The application will start using the H2 in-memory database.

---
