# Distributed Ride-Hailing Saga — Cloud-Native Serverless Orchestration Backend

[![AWS CDK](https://img.shields.io/badge/AWS_CDK-Java-orange.svg)](https://aws.amazon.com/cdk/)
[![ECS Fargate](https://img.shields.io/badge/AWS_ECS-Fargate-blue.svg)](https://aws.amazon.com/ecs/)
[![Step Functions](https://img.shields.io/badge/AWS-Step_Functions-pink.svg)](https://aws.amazon.com/step-functions/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue.svg)](https://www.docker.com/)

A cloud-native, distributed ride-hailing backend built with **Java 17**, **Spring Boot**, and **AWS CDK (Java)**. This project implements the **Saga Pattern (Orchestration)** to manage distributed transactions and guarantee eventual consistency across microservices without relying on a centralized database.


---

## 📐 System Architecture

The infrastructure is provisioned entirely using **AWS CDK in Java** and deployed onto a Multi-AZ Virtual Private Cloud (VPC) spanning 2 Availability Zones.

<img width="1654" height="1169" alt="cloud-deployment-final" src="https://github.com/user-attachments/assets/fe622b04-df4f-422f-831a-736fbfa43f8b" />

### Ingress & Traffic Flow
1. **Public Front Door (Trigger API)**: An external REST API Gateway receives incoming client requests (`POST /start-saga`) and uses Apache Velocity Template Language (VTL) to transform the JSON payload into an Amazon Coral RPC `StartSyncExecution` call natively without intermediate Lambda compute.
2. **Orchestration Engine**: **AWS Step Functions** acts as the central brain, executing a Synchronous Express State Machine workflow (`CreateTrip` $\rightarrow$ `ProcessPayment` $\rightarrow$ `CreateDispatch`).
3. **TLS Termination & Private Bridge**: Step Functions invokes an internal **HTTP API Gateway (Proxy API)**, which routes traffic through an **API Gateway VPC Link** with Elastic Network Interfaces (ENIs) directly into private subnets.
4. **Internal Routing & Compute**: Traffic lands on an **Internal Application Load Balancer (ALB)** performing path-based routing (`/trip`, `/payment`, `/dispatch`) to 3 Spring Boot microservices running on serverless **AWS ECS Fargate** tasks.
5. **Private Service Discovery**: Microservices discover internal endpoints using **AWS CloudMap** private DNS (`saga.local`).

---

## 🔄 Saga Orchestration & Rollback Mechanics

Each microservice manages its own isolated MySQL database (**Database-per-Service** pattern). Because traditional `@Transactional` blocks cannot cross network boundaries, transaction consistency is managed programmatically via Step Functions.

### 1. The Happy Path Execution
* **`CreateTrip`**: Invokes `POST /trip`. The `trip-service` creates a ride record in `trip_db` and returns a generated `tripId` UUID.
* **Payload Accumulation ("Backpack")**: The State Machine uses `.resultPath("$.tripResult")` to preserve global execution state while appending task responses into an accumulating state payload.
* **`ProcessPayment`**: Invokes `POST /payment` passing `tripId` and `paymentAmount`. The `payment-service` validates the request, checks idempotency headers, locks funds in `payment_db`, and returns `paymentResult`.
* **`CreateDispatch`**: Invokes `POST /dispatch` passing `tripId`, `cabNo`, `cabDriver` and `pickupLocation`. The `dispatch-service` assigns a driver in `dispatch_db` and returns `dispatchResult`.

### 2. Idempotency & Concurrency Shielding
* Incoming payment requests require an execution-specific `Idempotency-Key` generated natively in CDK using Amazon States Language (ASL) intrinsic functions (`States.Array(States.UUID())`).
* The `payment-service` uses a custom Spring Boot `HandlerInterceptor`. Under high concurrency, duplicate requests trigger a `DataIntegrityViolationException`, returning HTTP `409 Conflict` to prevent double charges.

### 3. Failover & Automated Compensating Transactions (Chaos Testing)
* **The "Poison Pill"**: Passing `"pickupLocation": "FAILOVER_TEST"` in the Dispatch payload causes `dispatch-service` to throw an intentional `503 Service Unavailable` error.
* **Error Interception**: An explicit `.addCatch()` block on the `CreateDispatch` task intercepts `ApiGateway.503` errors while preserving the execution context in `$.errorInfo`.
* **Compensating Workflow**: Step Functions automatically triggers reverse compensating calls:
    1. `POST /payments/refund` $\rightarrow$ Issues a refund record in `payment_db`.
    2. `POST /trip/cancel` $\rightarrow$ Marks the trip status as `CANCELLED` in `trip_db`.
* **Result**: Eliminates orphaned charges and guarantees eventual consistency across all isolated databases.

---

## 🛠️ Key Technical Challenges & Lessons Learned

| Challenge | Root Cause | Solution |
| :--- | :--- | :--- |
| **Startup Deadlocks & Health Check Race Conditions** | Spring Boot / HikariCP timed out (30s) waiting for MySQL containers to finish initialization on port 3306, causing ECS task flapping and ALB 502/503 errors. | Set `SPRING_DATASOURCE_HIKARI_INITIALIZATIONFAILTIMEOUT="-1"`, explicitly set `SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.MySQLDialect"`, increased ALB health check timeout to 15s, and added `healthCheckGracePeriod(180s)` in CDK. |
| **State Payload Erasure** | Step Functions default behavior overwrites the state input (`$`) with the output of the completed task. | Utilized explicit `.resultPath("$.tripResult")`, `.resultPath("$.paymentResult")`, and `.resultPath("$.dispatchResult")` namespaces to accumulate state. |
| **Containerized Relational Databases** | Deploying MySQL instances as ephemeral ECS Fargate containers with local storage created data volatility risks. | Identified as a PoC architectural anti-pattern; production deployments should utilize managed relational database services like **Amazon RDS** or **Amazon Aurora**. |
| **Single-Stage Docker Builds** | Shipping full Maven build toolchains inside runtime containers bloated image sizes and slowed cold boots. | Future optimization: transition to **Multi-Stage Docker builds** to separate compile-time Maven artifacts from lightweight JRE runtime images. |

---

## 🚀 Repository Structure

```
distributed-ride-hailing-saga/
├── pom.xml                        # Parent Multi-Module Maven POM
├── trip-service/                  # Spring Boot Trip Microservice & Dockerfile
│   ├── src/
│   └── pom.xml
├── payment-service/               # Spring Boot Payment Microservice & Dockerfile
│   ├── src/
│   └── pom.xml
├── dispatch-service/              # Spring Boot Dispatch Microservice & Dockerfile
│   ├── src/
│   └── pom.xml
└── cdk/                           # Infrastructure as Code (AWS CDK Java)
    ├── src/main/java/com/myorg/
    │   ├── CdkApp.java            # CDK Application Entry Point
    │   ├── VpcStack.java          # Network Infrastructure Stack
    │   └── ComputeStack.java      # ECS Fargate, ALB, API Gateway & Step Functions
    ├── cdk.json
    └── pom.xml
```

---

## 🧪 Testing & Verification

### 1. Happy Path Execution
Send a POST request to the API Gateway Trigger URL:
```bash
curl -X POST https://<api-gateway-id>.execute-api.<region>.amazonaws.com/prod/start-saga \
  -H "Content-Type: application/json" \
  -d '{
        "contactNumber": "0777225834",
        "pickupLocation": "Galle Face",
        "dropOffLocation": "Nawala"
      }'
```
**Expected Response (`200 OK`)**:
Returns an accumulated JSON payload containing `tripResult`, `paymentResult`, and `dispatchResult` with matching UUID references across all databases.

### 2. Chaos / Failover Test (`FAILOVER_TEST`)
Trigger the poison pill failover mechanism:
```bash
curl -X POST https://<api-gateway-id>.execute-api.<region>.amazonaws.com/prod/start-saga \
  -H "Content-Type: application/json" \
  -d '{
        "contactNumber": "0777225834",
        "pickupLocation": "FAILOVER_TEST",
        "dropOffLocation": "Nawala"
      }'
```
**Expected Response (`200 OK` with Rollback Details)**:
The `dispatch-service` returns a `503 Service Unavailable`. Step Functions catches the failure, executes compensating calls (`/payments/refund` $\rightarrow$ `/trip/cancel`), and returns state details including `$.errorInfo`, `$.paymentRefundResult`, and `$.cancelTripResult` while preserving data consistency.

---

## 📜 License & Acknowledgments

Built as an independent cloud engineering project applying **AWS Solutions Architect Associate (SAA)** concepts.
