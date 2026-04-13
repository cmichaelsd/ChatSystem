# ChatSystem

## Overview
This project is broken into 5 portions
1) API Server - Python fastapi project which handles all HTTP requests and forwards requests related specifically to chatserver to that service. Handles user authentication, writes persistant data to a PSQL instance (groups, users).
2) Chat Server - Kotlin Ktor project which is primarily a websocket server to handle realtime chatting between group members. This server also handle internal HTTP requests from API Server to fetch things like conversation history. Has access to SQS and DynamoDB. SQS handles routing of messages between users. DynamoDB store data such as which server a user is connected to and a servers SQS queue url. 
3) Presence Server - Python fastapi project which communicates with a Redis instance. Redis acts as a cache which receives heartbeats from online users, if a heartbeat isn't received for a set period of time the cache entry automatically expires and the user becomes offline.
4) Chat Client - React + Vite project which is the frontend the user interacts with to chat. I didn't make this its AI generated, this excersice is for Infra and System Design not UI/UX.
5) Chat Infra - Terraform project used to setup infra required to support this project.

![Diagram of service](./diagram.svg)


## Scaling Dimensions
This is a real-time messaging system. The right scaling metrics are:

1. **Concurrent WebSocket connections** - how many users are simultaneously connected to ChatServer instances
2. **Messages per second** - throughput of the SQS fan-out and DynamoDB routing layer
3. **DAU / active sessions** - drives how many ChatServer replicas and PresenceServer capacity is needed


### How Scaling Works
ChatServer is the unit of horizontal scale. Each instance:
- Holds WebSocket sessions in an in-memory `SessionStore` (userId → WebSocket)
- Registers itself in DynamoDB (`ServerRegistry`) with its SQS queue URL on startup
- Writes each connected user's server mapping to DynamoDB (`UserConnections`)
- Owns one SQS queue - incoming messages for its connected users arrive via that queue

Because server discovery is DynamoDB-backed and each instance is independently addressable via SQS, adding replicas is transparent 
- new instances register themselves and immediately start receiving routed messages.

The in-memory `SessionStore` is the only stateful piece. A user is pinned to the instance they connected to for the duration of their WebSocket session. 
On disconnect, the `UserConnections` entry is removed and the next connection can land on any instance.


### Concurrent WebSocket Capacity
Ktor runs on the CIO engine (coroutine I/O). WebSocket connections are coroutines, not threads 
- coroutines are extremely lightweight (~a few KB of stack vs ~1 MB for a thread), so connection count is bound by heap memory, not OS thread limits.

On a Fargate task with 1 vCPU / 2 GB RAM:
- JVM + framework overhead: ~300 MB
- Per-connection cost (coroutine state, session map entry, frame buffers): ~30–50 KB for a chat workload
- Usable for connections: ~1.7 GB → roughly **30,000–50,000 concurrent connections per instance**

CPU is almost entirely idle during this - a chat server spends its time waiting on SQS and DynamoDB, not computing.
Memory is the binding constraint.


### Bottlenecks Under Load
**DynamoDB** wears the heaviest load: every routed message triggers `UserConnections` lookups for each conversation member to find their server, plus a write on connect/disconnect. 
For large group conversations this becomes a fan-out of N reads per message. 
On-demand capacity mode handles bursts; provisioned with auto-scaling is appropriate at sustained load.

**SQS** is not a meaningful bottleneck - standard queues support effectively unlimited throughput and each ChatServer instance has its own dedicated queue.

**PresenceServer / Redis** scales independently. Redis TTL-based presence is read-heavy and cache-friendly; 
a single ElastiCache node handles a large number of heartbeats before needing a replica.


### Hypothetical Ceiling
| Metric                           | Single Instance             | 10 Instances                  |
|----------------------------------|-----------------------------|-------------------------------|
| Concurrent WebSocket connections | ~30,000–50,000              | ~300,000–500,000              |
| Messages/sec                     | Bounded by DynamoDB RCU/WCU | Scales with DynamoDB capacity |
| SQS queues                       | 1                           | 10 (one per instance)         |
| Fargate task size                | 1 vCPU / 2 GB               | Same, horizontal not vertical |


## How to run locally
Check the README.md of each project for instructions.
Each service should be run at the same time locally for full end-to-end testing.


## How to setup remote on AWS
Similar to local run, check the ChatInfra project README.md for instructions.


## Requirements
- awscli
- docker
- docker-compose
- terraform