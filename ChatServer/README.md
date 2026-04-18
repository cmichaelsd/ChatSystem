# ChatServer

## Overview
This ChatServer handles WebSocket connections and message delivery for the chat system.

A chat server will register itself and its SQS queue URL with DynamoDB.

When a user joins a given chat server the user is associated with the server in DynamoDB.
Also, a mapping is kept of each user and their WebSocket session at runtime.

When a user sends a message, the target user's server is found from DynamoDB and the associated SQS queue URL is used to route the message to that server for delivery.

If the target user is offline the message is saved to a pending messages table in DynamoDB and delivered when the user reconnects.

Both 1-to-1 and group chat use the same conversation model. 
A conversation has members stored in DynamoDB and a message sent to a conversation is fanned out to all members. Offline behavior is the same 
- undelivered messages are saved as pending.

Message history is persisted to DynamoDB on every send and can be fetched via the REST API.


## DynamoDB Tables

### ServerRegistry
| Attribute      | Key | Type   | Description                        |
|----------------|-----|--------|------------------------------------|
| `serverId`     | PK  | String | Unique server identifier           |
| `queueUrl`     |     | String | SQS queue URL for this server      |
| `ip`           |     | String | Server IP address                  |
| `registeredAt` |     | String | ISO-8601 timestamp of registration |

### UserConnections
| Attribute  | Key | Type   | Description                               |
|------------|-----|--------|-------------------------------------------|
| `userId`   | PK  | String | Unique user identifier                    |
| `serverId` |     | String | Server the user is currently connected to |

### ConversationMembers
| Attribute        | Key | Type   | Description                    |
|------------------|-----|--------|--------------------------------|
| `conversationId` | PK  | String | Unique conversation identifier |
| `userId`         | SK  | String | Member of the conversation     |

### Messages
| Attribute        | Key | Type   | Description                          |
|------------------|-----|--------|--------------------------------------|
| `conversationId` | PK  | String | Conversation this message belongs to |
| `sentAt`         | SK  | String | ISO-8601 timestamp of send time      |
| `fromUserId`     |     | String | Sender's user ID                     |
| `content`        |     | String | Message content                      |

### PendingMessages
| Attribute        | Key | Type   | Description                              |
|------------------|-----|--------|------------------------------------------|
| `userId`         | PK  | String | Recipient user ID (offline at send time) |
| `messageId`      | SK  | String | UUID for the pending message             |
| `fromUserId`     |     | String | Sender's user ID                         |
| `conversationId` |     | String | Conversation this message belongs to     |
| `content`        |     | String | Message content                          |
| `sentAt`         |     | String | ISO-8601 timestamp of send time          |


## REST API

### Conversation management (called by ApiServer)
| Method   | Endpoint                                           | Description                                |
|----------|----------------------------------------------------|--------------------------------------------|
| `POST`   | `/conversations`                                   | Create a conversation with initial members |
| `POST`   | `/conversations/{conversationId}/members/{userId}` | Add a member to a conversation             |
| `DELETE` | `/conversations/{conversationId}/members/{userId}` | Remove a member from a conversation        |

### Message history
| Method | Endpoint                                   | Auth | Description                              |
|--------|--------------------------------------------|------|------------------------------------------|
| `GET`  | `/conversations/{conversationId}/messages` | JWT  | Fetch message history for a conversation |

### Presence (proxied to PresenceServer)
| Method | Endpoint           | Auth | Description                                   |
|--------|--------------------|------|-----------------------------------------------|
| `POST` | `/presence/batch`  | JWT  | Get online/offline status for a list of users |


## How to run locally
1) Create the shared Docker network (once): `docker network create chatsystem`
2) Start ApiServer: `docker compose -f docker-compose.dev.yml up` from `ApiServer/`
3) Start PresenceServer: `docker compose -f docker-compose.dev.yml up` from `PresenceServer/`
4) `docker compose -f docker-compose.dev.yml build --no-cache`
5) `docker compose -f docker-compose.dev.yml up`
6) Create users and groups from ApiServer - this registers conversations in ChatServer automatically
7) Log in as each user via `POST /auth/login` and save the returned `access_token`
8) In one terminal: `websocat ws://localhost:8080/chat?token=<USER_A_TOKEN>`
9) In another terminal: `websocat ws://localhost:8081/chat?token=<USER_B_TOKEN>`
10) Send a message from userA: `{"conversationId":"<GROUP_ID>","content":"hello"}`


## How to push docker image to AWS ECR
1) `aws ecr get-login-password --region us-west-1 --profile chatsystem | docker login --username AWS --password-stdin 657083456388.dkr.ecr.us-west-1.amazonaws.com`
2) `docker build -t chatsystem-chatserver .`
3) `docker tag chatsystem-chatserver:latest 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/chatserver:latest`
4) `docker push 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/chatserver:latest`
5) For image update in ECS (if needed): `aws ecs update-service --cluster chatsystem --service chatsystem-chatserver --force-new-deployment --profile chatsystem --region us-west-1`


## How to test
`./gradlew test clean`


## How to lint
- `./gradlew ktlintCheck`
- `./gradlew ktlintFormat`